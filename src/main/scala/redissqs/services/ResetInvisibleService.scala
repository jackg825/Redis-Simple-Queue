package redissqs.services

import javax.inject.{Inject, Singleton}
import com.twitter.finagle.redis.{Client => RedisClient}
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.redis.protocol.ZInterval
import com.twitter.inject.Logging
import com.twitter.io.Buf
import com.twitter.util.Future
import redissqs.domains.service.QueueName
import redissqs.utils.AppConfigLib.getConfig
import redissqs.utils.PipeOperator._
import redissqs.utils.Implicits._
import redissqs.utils.Prefixs._

@Singleton
class ResetInvisibleService @Inject()(redis: RedisClient) extends Service[Request, List[QueueName]] with Logging {

  private val invisibleTime = getConfig[Int]("INVISIBLE_TIME").get

  override def apply(request: Request): Future[List[QueueName]] = {
    for {
      names  <- getQueueNames()
      result <- Future.collect(names.map(resetByQueueName(_)))
    } yield {
      result.toList
    }
  }

  val getQueueNames: () => Future[List[QueueName]] = { () =>
    redis.sMembers(REGISTER_QUEUE.toBuf).map(_.map(_.deBuf.get.|>(QueueName)).toList).handle {
      case e =>
        e.|>(error("get registered queue names exception", _)).|>(_ => List.empty)
    }
  }

  val getExpiredData: QueueName => Future[Seq[Buf]] = { name =>
    redis
      .zRangeByScore(key = (INVISIBLE_PREFIX + name.v).toBuf,
                     min = ZInterval(1),
                     max = ZInterval(System.currentTimeMillis() + invisibleTime),
                     withScores = false,
                     limit = None)
      .filter(_.isRight)
      .map(_.right.get)
  }

  val resetExpiredData: (QueueName, Seq[Buf]) => Future[QueueName] = { (name, datas) =>
    redis.lPush(key = (QUEUE_PREFIX + name.v).toBuf, values = datas.toList).map {
      case n if n > 0 => name
      case _          => name.#!("reset expired data exception")
    }
  }

  val removeDataFromInvisibleQueue: (QueueName, Seq[Buf]) => Future[QueueName] = { (name, datas) =>
    redis.zRem(key = (INVISIBLE_PREFIX + name.v).toBuf, members = datas).map {
      case n if n > 0 => name
      case _          => name.#!("remove expired data from zset exception")
    }
  }

  val resetByQueueName: QueueName => Future[QueueName] = { name =>
    for {
      bufs   <- getExpiredData(name)
      _      <- resetExpiredData(name, bufs)
      result <- removeDataFromInvisibleQueue(name, bufs)
    } yield {
      result
    }
  }

}

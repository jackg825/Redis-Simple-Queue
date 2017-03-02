package redissqs.services

import javax.inject.{Inject, Singleton}

import io.github.hamsters.Validation._
import com.github.mehmetakiftutuncu.errors.{Errors, Maybe, SimpleError}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.twitter.finagle.redis.{Client => RedisClient}
import com.twitter.finagle.Service
import com.twitter.inject.Logging
import com.twitter.io.Buf
import com.twitter.util.Future
import io.github.hamsters.twitter.{FutureEither => EitherT}
import redissqs.domains.errors.ServiceErrors
import redissqs.domains.http.{ReceiveMessageRequest, ReceiveMessageResponse}
import redissqs.domains.service.QueueName
import redissqs.utils.PipeOperator._
import redissqs.utils.Implicits._
import redissqs.utils.Prefixs._
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ReceiveMessageService @Inject()(redis: RedisClient)
    extends Service[ReceiveMessageRequest, Maybe[ReceiveMessageResponse]]
    with Logging {

  override def apply(req: ReceiveMessageRequest): Future[Maybe[ReceiveMessageResponse]] = {
    val name = QueueName(req.name)
    (for {
      buf  <- EitherT(popQueue(name))
      _    <- EitherT(setValueInvisible(name, buf))
      json <- EitherT(buf2JsonNode(buf))
      rep  <- EitherT(provideReceiveMessageResponse(name, json))
    } yield {
      rep
    }).future.handle {
      case e =>
        e.#!(s"Receive Message exception: ${e.toString}").|>(_ => Errors(SimpleError.database)).|>(KO(_))
    }
  }

  val popQueue: QueueName => Future[Maybe[Buf]] = { name =>
    redis.lPop(key = (QUEUE_PREFIX + name.v).toBuf).map {
      case Some(b) => OK(b)
      case _       => KO(ServiceErrors.QueueNotFound)
    }
  }

  val setValueInvisible: (QueueName, Buf) => Future[Maybe[Long]] = { (name, buf) =>
    redis
      .zAdd(key = (INVISIBLE_PREFIX + name.v).toBuf, score = System.currentTimeMillis().toDouble, member = buf)
      .map {
        case n if n > 0 => OK(n)
        case _          => KO(ServiceErrors.Insert2ZsetError)
      }
  }

  val buf2JsonNode: Buf => Future[Maybe[JsonNode]] = { buf =>
    val mapper = new ObjectMapper()
    buf.deBuf.map(s => mapper.readTree(s).|>(OK(_))).getOrElse(KO(ServiceErrors.DeBufError)).|>(Future(_))
  }

  val provideReceiveMessageResponse: (QueueName, JsonNode) => Future[Maybe[ReceiveMessageResponse]] = { (name, json) =>
    ReceiveMessageResponse(name = name.v, value = json).|>(OK(_)).|>(Future(_))
  }
}

package redissqs.services

import javax.inject.{Inject, Singleton}

import io.github.hamsters.Validation._
import com.github.mehmetakiftutuncu.errors.Maybe
import com.twitter.finagle.redis.{Client => RedisClient}
import com.twitter.finagle.Service
import com.twitter.finatra.annotations.CamelCaseMapper
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging
import com.twitter.util.Future
import io.github.hamsters.twitter.FutureEither
import redissqs.domains.errors.ServiceErrors
import redissqs.domains.http.{SendMessageRequest, SendMessageResponse}
import redissqs.domains.service.QueueName
import redissqs.utils.PipeOperator._
import redissqs.utils.Implicits._
import redissqs.utils.Prefixs._
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SendMessageService @Inject()(redis: RedisClient, @CamelCaseMapper mapper: FinatraObjectMapper)
    extends Service[SendMessageRequest, Maybe[SendMessageResponse]]
    with Logging {

  override def apply(req: SendMessageRequest): Future[Maybe[SendMessageResponse]] = {
    val name = QueueName(req.name)
    (for {
      _      <- FutureEither(registerQueue(name))
      result <- FutureEither(insertQueue(req))
      rep    <- FutureEither(provideSendMessageResponse(result))
    } yield {
      rep
    }).future
  }

  val registerQueue: QueueName => Future[Maybe[QueueName]] = { (name) =>
    val existOrNot = redis.sIsMember(key = REGISTER_QUEUE.toBuf, member = name.v.toBuf)
    existOrNot.flatMap { x =>
      if (x) {
        name.|>(OK(_)).|>(Future(_))
      } else {
        redis.sAdd(key = REGISTER_QUEUE.toBuf, members = List(name.v.toBuf)).map {
          case n if n > 0 => name.|>(OK(_))
          case _          => ServiceErrors.RegisterQueuesError.|>(KO(_))
        }
      }
    }
  }

  val insertQueue: SendMessageRequest => Future[Maybe[SendMessageRequest]] = { (req) =>
    redis.rPush(key = (QUEUE_PREFIX + req.name).toBuf, values = List(mapper.writeValueAsString(req.value).toBuf)).map {
      case n if n > 0 => req.|>(OK(_))
      case _          => ServiceErrors.Insert2QueueError.|>(KO(_))
    }
  }

  val provideSendMessageResponse: SendMessageRequest => Future[Maybe[SendMessageResponse]] = { req =>
    SendMessageResponse(name = req.name, value = req.value).|>(OK(_)).|>(Future(_))
  }
}

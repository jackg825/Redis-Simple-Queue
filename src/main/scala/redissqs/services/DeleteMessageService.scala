package redissqs.services

import javax.inject.{Inject, Singleton}

import io.github.hamsters.Validation._
import com.github.mehmetakiftutuncu.errors.Maybe
import com.twitter.finagle.Service
import com.twitter.finagle.redis.{Client => RedisClient}
import com.twitter.finatra.annotations.CamelCaseMapper
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging
import com.twitter.util.Future
import io.github.hamsters.twitter.{FutureEither => EitherT}
import redissqs.domains.errors.ServiceErrors
import redissqs.domains.http.{DeleteMessageRequest, DeleteMessageResponse}
import redissqs.utils.Implicits._
import redissqs.utils.PipeOperator._
import redissqs.utils.Prefixs._
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DeleteMessageService @Inject()(redis: RedisClient, @CamelCaseMapper mapper: FinatraObjectMapper)
    extends Service[DeleteMessageRequest, Maybe[DeleteMessageResponse]]
    with Logging {

  override def apply(request: DeleteMessageRequest): Future[Maybe[DeleteMessageResponse]] = {
    (for {
      result   <- EitherT(removeZsetMember(request))
      _        <- EitherT(removeRegisteredNameIfEmpty(request))
      response <- EitherT(provideDeleteMessageResponse(result))
    } yield {
      response
    }).future
  }

  val removeZsetMember: DeleteMessageRequest => Future[Maybe[DeleteMessageRequest]] = { (req) =>
    info(s"removeZsetMember ${INVISIBLE_PREFIX + req.name} with key [${mapper.writeValueAsString(req.value)}]")
    redis
      .zRem(key = (INVISIBLE_PREFIX + req.name).toBuf, members = List(mapper.writeValueAsString(req.value).toBuf))
      .map {
        case n if n > 0 => req.|>(OK(_))
        case _          => ServiceErrors.DataInZsetNotFound.|>(KO(_))
      }
  }

  val removeRegisteredNameIfEmpty: DeleteMessageRequest => Future[Maybe[DeleteMessageRequest]] = { (req) =>
    for {
      queue <- redis.zCard(key = (INVISIBLE_PREFIX + req.name).toBuf)
      zset  <- redis.lLen(key = (QUEUE_PREFIX + req.name).toBuf)
    } yield {
      if (queue.+(zset) == 0) {
        redis.sRem(key = REGISTER_QUEUE.toBuf, members = List(req.name.toBuf))
      }
      req.|>(OK(_))
    }
  }

  val provideDeleteMessageResponse: DeleteMessageRequest => Future[Maybe[DeleteMessageResponse]] = { (req) =>
    DeleteMessageResponse(name = req.name, value = req.value).|>(OK(_)).|>(Future(_))
  }
}

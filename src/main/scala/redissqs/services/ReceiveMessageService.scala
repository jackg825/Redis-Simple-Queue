package redissqs.services

import javax.inject.{Inject, Singleton}

import io.github.hamsters.Validation._
import com.github.mehmetakiftutuncu.errors.{Errors, Maybe, SimpleError}
import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.finagle.redis.{Client => RedisClient}
import com.twitter.finagle.Service
import com.twitter.inject.Logging
import com.twitter.util.Future
import redissqs.domains.http.{ReceiveMessageRequest, ReceiveMessageResponse}
import redissqs.utils.PipeOperator._
import redissqs.utils.Implicits._
import redissqs.utils.Prefixs._

@Singleton
class ReceiveMessageService @Inject()(redis: RedisClient)
    extends Service[ReceiveMessageRequest, Maybe[ReceiveMessageResponse]]
    with Logging {

  override def apply(req: ReceiveMessageRequest): Future[Maybe[ReceiveMessageResponse]] = {
    val mapper = new ObjectMapper()
    (for {
      p <- redis.lPop(key = (QUEUE_PREFIX + req.name).toBuf)
      _ <- redis.zAdd(key = (INVISIBLE_PREFIX + req.name).toBuf,
                      score = System.currentTimeMillis().toDouble,
                      member = p.get)
      b    <- p.get.deBuf.|>(Future(_))
      json <- mapper.readTree(b.get).|>(Future(_))
    } yield {
      ReceiveMessageResponse(value = json).|>(OK(_))
    }).handle {
      case e => e.#!(s"Receive Message exception: ${e.toString}").|>(_ => Errors(SimpleError.database)).|>(KO(_))
    }
  }
}

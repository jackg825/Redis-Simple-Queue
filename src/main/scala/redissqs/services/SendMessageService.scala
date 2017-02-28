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
import redissqs.domains.http.{SendMessageRequest, SendMessageResponse}
import redissqs.utils.PipeOperator._
import redissqs.utils.Implicits._

@Singleton
class SendMessageService @Inject()(redis: RedisClient, @CamelCaseMapper mapper: FinatraObjectMapper)
    extends Service[SendMessageRequest, Maybe[SendMessageResponse]]
    with Logging {

  override def apply(req: SendMessageRequest): Future[Maybe[SendMessageResponse]] = {
    for {
      _ <- registerQueue(req.name)
      _ <- redis.rPush(key = ("queue_" + req.name).toBuf, values = List(mapper.writeValueAsString(req.value).toBuf))
    } yield {
      SendMessageResponse(name = req.name, value = req.value).|>(OK(_))
    }
  }

  val registerQueue: (String) => Future[String] = { (name) =>
    for {
      _ <- redis.sIsMember(key = "queues".toBuf, member = name.toBuf)
      _ <- redis.sAdd(key = "queues".toBuf, members = List(name.toBuf))
    } yield {
      name
    }
  }

}

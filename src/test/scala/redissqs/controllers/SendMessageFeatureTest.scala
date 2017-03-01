package redissqs.controllers

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.google.inject.Stage
import com.twitter.finagle.redis.{Client => RedisClient}
import com.twitter.finagle.http.Status
import com.twitter.finatra.annotations.CamelCaseMapper
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future
import redissqs.Server
import redissqs.utils.RedisDockerTestKit
import redissqs.utils.FutureUtils._
import redissqs.utils.PipeOperator._
import redissqs.utils.Implicits._
import redissqs.utils.Prefixs._

/***
  * sbt 'test-only redissqs.controllers.SendMessageFeatureTest'
  */
class SendMessageFeatureTest extends FeatureTest with RedisDockerTestKit {

  startAllOrFail()

  override def beforeAll(): Unit = {
    super.beforeAll()
    isContainerReady(redisContainer).toFutureValue shouldBe true
  }

  override def afterAll(): Unit = {
    stopAllQuietly()
    super.afterAll()
  }

  private val QUEUE_NAME  = "testqueue"
  private val QUEUE_VALUE = "abc"

  override val server = new EmbeddedHttpServer(twitterServer = new Server, stage = Stage.DEVELOPMENT)

  lazy private val mapper = server.injector.instance[FinatraObjectMapper, CamelCaseMapper]

  test("Succeed(200) when insert message to redis queue success") {
    val request =
      s"""
        |{
        | "name": "$QUEUE_NAME",
        | "value": {
        |   "v": "$QUEUE_VALUE"
        | }
        |}
      """.stripMargin
    val response =
      s"""
        |{
        | "name": "$QUEUE_NAME",
        | "value": {
        |   "v": "$QUEUE_VALUE"
        | }
        |}
      """.stripMargin
    server.httpPost(path = "/priv/redissqs/v1/msg/insert",
                    postBody = request,
                    andExpect = Status.Ok,
                    withJsonBody = response)
    val jsonNode = s"""{"v":"$QUEUE_VALUE"}"""
    getFromRedis(QUEUE_NAME).|>(mapper.writeValueAsString(_)) should be(jsonNode)
  }
  test("Fail(400) when required field is not provide") { pending }
  test("Fail(400) when required field value is not valid JSON format") { pending }
  test("Fail(500) when redis is not available") { pending }

  val getFromRedis: String => JsonNode = { name =>
    val client = server.injector.instance[RedisClient]
    val mapper = new ObjectMapper()
    (for {
      buf  <- client.lPop((QUEUE_PREFIX + name).toBuf)
      json <- mapper.readTree(buf.get.deBuf.get).|>(Future(_))
    } yield {
      json
    }).toFutureValue
  }
}

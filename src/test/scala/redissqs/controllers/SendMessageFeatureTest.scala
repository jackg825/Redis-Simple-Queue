package redissqs.controllers

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.google.inject.Stage
import com.twitter.finagle.redis.{Client => RedisClient}
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
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

  lazy private val mapper = server.injector.instance[ObjectMapper]

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
    val jsonNode = mapper.readTree(s"""{"v":"$QUEUE_VALUE"}""")
    getFromRedis(QUEUE_NAME) should be(jsonNode)
    flushRedisDatabase()
  }
  test("Fail(400) when required field is not provide") {
    val request =
      s"""
         |{
         | "abc": "$QUEUE_VALUE"
         |}
      """.stripMargin
    val errmsg = Seq("name: field is required", "value: field is required")
    server.httpPost(path = "/priv/redissqs/v1/msg/insert",
                    postBody = request,
                    andExpect = Status.BadRequest,
                    withErrors = errmsg)
    getRedisQueueLength(QUEUE_NAME) should be(0)
    flushRedisDatabase()
  }
  test("Fail(400) when required field value is not valid JSON format") {
    val request =
      s"""
         |{
         | "name": "$QUEUE_NAME",
         | "value": {
         |   "v" = "$QUEUE_VALUE"
         | }
         |}
      """.stripMargin
    val errmsg = Seq("Unexpected character ('=' (code 61)): was expecting a colon to separate field name and value")
    server.httpPost(path = "/priv/redissqs/v1/msg/insert",
                    postBody = request,
                    andExpect = Status.BadRequest,
                    withErrors = errmsg)
    getRedisQueueLength(QUEUE_NAME) should be(0)
    flushRedisDatabase()
  }
  test("Fail(500) when redis is not available") {
    stopAllQuietly()
    val request =
      s"""
         |{
         | "name": "$QUEUE_NAME",
         | "value": {
         |   "v": "$QUEUE_VALUE"
         | }
         |}
      """.stripMargin
    val errmsg = Seq("internal server error")
    server.httpPost(path = "/priv/redissqs/v1/msg/insert",
                    postBody = request,
                    andExpect = Status.InternalServerError,
                    withErrors = errmsg)
  }

  val getFromRedis: String => JsonNode = { name =>
    val client = server.injector.instance[RedisClient]
    (for {
      buf  <- client.lPop((QUEUE_PREFIX + name).toBuf)
      json <- mapper.readTree(buf.get.deBuf.get).|>(Future(_))
    } yield json).toFutureValue
  }

  val getRedisQueueLength: String => Long = { name =>
    val client = server.injector.instance[RedisClient]
    client.lLen((QUEUE_PREFIX + name).toBuf).toFutureValue
  }

  val flushRedisDatabase: () => Unit = { () =>
    val client = server.injector.instance[RedisClient]
    client.flushDB().toFutureValue
  }
}

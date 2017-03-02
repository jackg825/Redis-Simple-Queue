package redissqs.controllers

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.google.inject.Stage
import com.twitter.finagle.http.Status
import com.twitter.finagle.redis.{Client => RedisClient}
import com.twitter.finatra.annotations.CamelCaseMapper
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future
import redissqs.Server
import redissqs.domains.service.QueueName
import redissqs.utils.RedisDockerTestKit
import redissqs.utils.FutureUtils._
import redissqs.utils.Implicits._
import redissqs.utils.PipeOperator._
import redissqs.utils.Prefixs._

/***
  * sbt 'test-only redissqs.controllers.ReceiveMessageFeatureTest'
  */
class ReceiveMessageFeatureTest extends FeatureTest with RedisDockerTestKit {

  startAllOrFail()

  override def beforeAll(): Unit = {
    super.beforeAll()
    isContainerReady(redisContainer).toFutureValue shouldBe true
  }

  override def afterAll(): Unit = {
    stopAllQuietly()
    super.afterAll()
  }

  override val server = new EmbeddedHttpServer(twitterServer = new Server, stage = Stage.DEVELOPMENT)

  lazy private val finatraMapper = server.injector.instance[FinatraObjectMapper, CamelCaseMapper]
  lazy private val mapper        = server.injector.instance[ObjectMapper]

  private val QUEUE_NAME  = QueueName("testqueue")
  private val QUEUE_VALUE = "abc"
  private val QUEUE_JSON =
    s"""{
      |  "v": "$QUEUE_VALUE"
      |}
    """.stripMargin
  private val QUEUE_JSONNODE = mapper.readTree(QUEUE_JSON)

  test("Succeed(200) when get message from redis queue success") {
    insert2Redis(QUEUE_NAME, QUEUE_JSONNODE)
    val request =
      s"""{
         | "name": "${QUEUE_NAME.v}"
         |}
      """.stripMargin
    val response =
      s"""{
         | "name": "${QUEUE_NAME.v}",
         | "value": {
         |   "v": "$QUEUE_VALUE"
         | }
         |}
      """.stripMargin
    server
      .httpPost(path = "/priv/redissqs/v1/msg/get", postBody = request, andExpect = Status.Ok, withJsonBody = response)
    getRedisQueueLength(QUEUE_NAME) should be(0)
    getRedisInvisibleQueueLength(QUEUE_NAME) should be(1)
    flushRedisDatabase()
  }
  test("Fail(204) when get message from redis queue empty or not found") {
    insert2Redis(QUEUE_NAME, QUEUE_JSONNODE)
    val request =
      s"""{
         | "name": "12345qwertasdfgzxcvb"
         |}
      """.stripMargin
    server.httpPost(path = "/priv/redissqs/v1/msg/get", postBody = request, andExpect = Status.NoContent)
    getRedisQueueLength(QUEUE_NAME) should be(1)
    flushRedisDatabase()
  }
  test("Fail(400) when query name is empty") {
    insert2Redis(QUEUE_NAME, QUEUE_JSONNODE)
    val request =
      s"""{
         | "name": ""
         |}
      """.stripMargin
    val errmsg = Seq("name: cannot be empty")
    server.httpPost(path = "/priv/redissqs/v1/msg/get",
                    postBody = request,
                    andExpect = Status.BadRequest,
                    withErrors = errmsg)
    getRedisQueueLength(QUEUE_NAME) should be(1)
    flushRedisDatabase()
  }
  test("Fail(400) when required field is not provided") {
    insert2Redis(QUEUE_NAME, QUEUE_JSONNODE)
    val request =
      s"""{
         | "abc": "123"
         |}
      """.stripMargin
    val errmsg = Seq("name: field is required")
    server.httpPost(path = "/priv/redissqs/v1/msg/get",
                    postBody = request,
                    andExpect = Status.BadRequest,
                    withErrors = errmsg)
    getRedisQueueLength(QUEUE_NAME) should be(1)
    flushRedisDatabase()
  }
  test("Fail(500) when redis is not available") {
    stopAllQuietly()
    val request =
      s"""{
         | "name": "${QUEUE_NAME.v}"
         |}
      """.stripMargin
    server.httpPost(path = "/priv/redissqs/v1/msg/get", postBody = request, andExpect = Status.InternalServerError)
  }

  val insert2Redis: (QueueName, JsonNode) => JsonNode = { (name, json) =>
    val client = server.injector.instance[RedisClient]
    (for {
      v <- finatraMapper.writeValueAsString(json).toBuf.|>(Future(_))
      _ <- client.rPush(key = (QUEUE_PREFIX + name.v).toBuf, values = List(v))
    } yield json).toFutureValue
  }

  val getRedisQueueLength: QueueName => Long = { name =>
    val client = server.injector.instance[RedisClient]
    client.lLen((QUEUE_PREFIX + name.v).toBuf).toFutureValue
  }

  val getRedisInvisibleQueueLength: QueueName => Long = { name =>
    val client = server.injector.instance[RedisClient]
    client.zCard((INVISIBLE_PREFIX + name.v).toBuf).toFutureValue
  }

  val flushRedisDatabase: () => Unit = { () =>
    val client = server.injector.instance[RedisClient]
    client.flushDB().toFutureValue
  }
}

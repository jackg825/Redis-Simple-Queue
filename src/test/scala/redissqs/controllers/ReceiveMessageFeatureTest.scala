package redissqs.controllers

import com.google.inject.Stage
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import redissqs.Server
import redissqs.utils.RedisDockerTestKit
import redissqs.utils.FutureUtils._

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

  test("Succeed(200) when get message from redis queue success") {pending}
  test("Fail(204) when get message from redis queue empty or not found") {pending}
  test("Fail(400) when query parameter is empty") {pending}
  test("Fail(500) when redis is not available") {pending}
}

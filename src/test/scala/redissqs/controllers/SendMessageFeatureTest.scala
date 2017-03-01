package redissqs.controllers

import com.google.inject.Stage
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import redissqs.Server
import redissqs.utils.RedisDockerTestKit
import redissqs.utils.FutureUtils._

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

  override val server = new EmbeddedHttpServer(twitterServer = new Server, stage = Stage.DEVELOPMENT)

  test("Succeed(200) when insert message to redis queue success") {pending}
  test("Fail(400) when required field is not provide") {pending}
  test("Fail(400) when required field value is not valid JSON format") {pending}
  test("Fail(500) when redis is not available") {pending}
}

package redissqs.controllers

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import com.twitter.finagle.http.Status._
import redissqs.Server

class AdminControllerFeatureTest extends FeatureTest {
  val serviceVersion = "0.9.9"

  override val server =
    new EmbeddedHttpServer(twitterServer = new Server, flags = Map("service.version" -> serviceVersion))

  test("return `OK` (health check) if the service is on") {
    server.httpGet(path = "/health", andExpect = Ok)
  }

  test("return service version") {
    server.httpGet(path = "/version", andExpect = Ok, withBody = serviceVersion)
  }
}

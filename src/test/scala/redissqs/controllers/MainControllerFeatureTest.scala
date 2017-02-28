package redissqs.controllers

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import redissqs.Server

class MainControllerFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new Server)

  "Server" should {
    "respond" in {
      server.httpGet(
        path = "/",
        andExpect = Ok,
        withBody = "{\"message\":\"success\"}")
    }
  }
}
package redissqs.controllers

import javax.inject.{Inject, Singleton}
import io.github.hamsters.Validation._
import com.github.mehmetakiftutuncu.errors.Errors
import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.finatra.http.Controller
import redissqs.domains.http._
import redissqs.services.{DeleteMessageService, ReceiveMessageService, SendMessageService}
import redissqs.swagger.ProjectSwagger

@Singleton
class MainController @Inject()(SendMsgSrv: SendMessageService,
                               ReceiveMsgSrv: ReceiveMessageService,
                               deleteMsgSrv: DeleteMessageService)
    extends Controller
    with SwaggerSupport {

  implicit protected val swagger = ProjectSwagger

  getWithDoc("/msg") { o =>
    o.summary("")
      .description("""
                     |
                     | __Request description__
                     |
                     | | Name  | Type     | Example         | Required | Description                               |
                     | |-------|----------|-----------------|----------|-------------------------------------------|
                     | | name  | String   | queue           | true     | The name of the queue                     |
                     |
        """.stripMargin)
      .tag("Simple Queue")
      .queryParam[String](name = "name", description = "queryParam", required = true)
      .responseWith[ReceiveMessageResponse](200, "OK - the request has succeeded.")
      .responseWith[Errors](400, "Bad Request - a problem reading or understanding the request.")
      .responseWith[Errors](500, "Internal Server Error - the server could not process the request.")
  } { request: ReceiveMessageRequest =>
    ReceiveMsgSrv(request).map {
      case OK(r) => response.ok.json(r)
      case KO(_) => response.internalServerError
    }
  }
  deleteWithDoc("/msg") { o =>
    o.summary("")
      .description("""
                     |
                     | __Request description__
                     |
                     | ```
                     | {
                     |  "name": "",
                     |  "value": {
                     |    ...
                     |  }
                     | }
                     | ```
                     |
                     | | Name  | Type     | Example         | Required | Description                               |
                     | |-------|----------|-----------------|----------|-------------------------------------------|
                     | | name  | String   | queue           | true     | The name of the queue                     |
                     | | value | JsonNode | JsonNode Object | true     | The data wish to store by JsonNode format |
                     |
        """.stripMargin)
      .tag("Simple Queue")
      .bodyParam[DeleteMessageRequest]("Body", "description")
      .responseWith[DeleteMessageResponse](200, "OK - the request has succeeded.")
      .responseWith[Errors](400, "Bad Request - a problem reading or understanding the request.")
      .responseWith[Errors](500, "Internal Server Error - the server could not process the request.")
  } { request: DeleteMessageRequest =>
    response.ok.json(DeleteMessageResponse(name = request.name, value = request.value))
  }

  postWithDoc("/msg") { o =>
    o.summary("")
      .description("""
                     |
                     | __Request description__
                     |
                     | ```
                     | {
                     |  "name": "",
                     |  "value": {
                     |    ...
                     |  }
                     | }
                     | ```
                     |
                     | | Name  | Type     | Example         | Required | Description                               |
                     | |-------|----------|-----------------|----------|-------------------------------------------|
                     | | name  | String   | queue           | true     | The name of the queue                     |
                     | | value | JsonNode | JsonNode Object | true     | The data wish to store by JsonNode format |
                     |
        """.stripMargin)
      .tag("Simple Queue")
      .bodyParam[SendMessageRequest]("Body", "description")
      .responseWith[SendMessageResponse](200, "OK - the request has succeeded.")
      .responseWith(400, "Bad Request - a problem reading or understanding the request.")
      .responseWith(500, "Internal Server Error - the server could not process the request.")
  } { request: SendMessageRequest =>
    SendMsgSrv(request).map {
      case OK(r) =>
        response.ok.json(r)
      case KO(_) =>
        response.internalServerError
    }
  }
}

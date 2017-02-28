package redissqs

import com.github.xiaodongw.swagger.finatra.{SwaggerController, WebjarsController}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.annotations.Lifecycle
import io.swagger.converter.ModelConverters
import io.swagger.jackson.ModelResolver
import io.swagger.models.{Contact, Info}
import redissqs.controllers.AdminController
import redissqs.controllers.MainController
import redissqs.modules.{CustomJacksonModule, RedisModule, SchedulerHandler}
import redissqs.swagger.ProjectSwagger
import redissqs.utils.AppConfigLib._

object ServerMain extends Server

class Server extends HttpServer {
  val serviceVersion = flag[String]("service.version", "NA", "the version of service")

  override def defaultFinatraHttpPort = getConfig[String]("FINATRA_HTTP_PORT").fold(":9999")(x => s":$x")
  override val name                   = "redissqs RedisSqsSample"

  override val modules       = Seq(RedisModule)
  override def jacksonModule = CustomJacksonModule

// Swagger JSON support
  ModelConverters
    .getInstance()
    .addConverter(new ModelResolver(jacksonModule.asInstanceOf[FinatraJacksonModule].provideScalaObjectMapper(null)))

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[WebjarsController]
      .add[AdminController]
      .add(new SwaggerController(swagger = ProjectSwagger))
      .add[MainController]
  }

  @Lifecycle protected override def postWarmup(): Unit = {
    super.postWarmup()

    val info = new Info()
      .contact(new Contact().name("Jack Chung").email("Jack_Chung@htc.com"))
      .description("**Redis SQS Sample** - A Queue Service that serve as a SQS service with Redis database.")
      .version(serviceVersion())
      .title("Redis SQS Sample API")

    ProjectSwagger.info(info)
    handle[SchedulerHandler]()
  }

}

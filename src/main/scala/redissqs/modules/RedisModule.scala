package redissqs.modules

import javax.inject.Singleton
import com.google.inject.Provides
import com.twitter.finagle.{Redis, redis}
import com.twitter.inject.TwitterModule
import redissqs.utils.AppConfigLib._

object RedisModule extends TwitterModule {

  private[this] val redisHost = getConfig[String]("REDIS_HOST").get

  @Singleton
  @Provides
  def providesDatabase(): redis.Client = Redis.newRichClient(redisHost)
}

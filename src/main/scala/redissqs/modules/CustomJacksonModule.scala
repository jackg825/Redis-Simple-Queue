package redissqs.modules

import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.json.utils.CamelCasePropertyNamingStrategy

object CustomJacksonModule extends FinatraJacksonModule {

  override val propertyNamingStrategy = CamelCasePropertyNamingStrategy
}

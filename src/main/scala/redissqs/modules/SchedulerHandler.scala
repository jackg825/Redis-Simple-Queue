package redissqs.modules

import javax.inject.Inject

import com.twitter.finagle.redis.Client
import com.twitter.inject.Logging
import com.twitter.inject.utils.Handler
import com.twitter.util.Future
import monix.execution.Scheduler.{global => scheduler}
import redissqs.utils.AppConfigLib.getConfig
import redissqs.utils.PipeOperator._
import redissqs.utils.Implicits._

import scala.concurrent.duration._

class SchedulerHandler @Inject()(redis: Client) extends Handler with Logging {

  private val invisibleTime       = getConfig[Int]("INVISIBLE_TIME").getOrElse(25)
  private val schedulerInitTime   = getConfig[Int]("SCHEDULER_INIT_TIME").getOrElse(300)
  private val schedulerRepeatTime = getConfig[Int]("SCHEDULER_REPEAT_TIME").getOrElse(1200)

  override def handle(): Unit = {
    scheduler.scheduleAtFixedRate(schedulerInitTime.seconds, schedulerRepeatTime.seconds) {
      debug(s"Run schedule at: ${System.currentTimeMillis()}")
      for {
        sq <- redis.sMembers("queues".toBuf)
        r <- sq.map(_.deBuf.get).|>(Future(_))
      } yield {

      }
    }
  }
}

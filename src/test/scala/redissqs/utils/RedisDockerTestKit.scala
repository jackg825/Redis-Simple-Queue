package redissqs.utils

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.{DockerContainer, DockerFactory, DockerKit, DockerReadyChecker}

import scala.concurrent.duration._

trait RedisDockerTestKit extends DockerKit {

  def RedisAdvertisedPort = 6379

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  override val StartContainersTimeout = 300.seconds

  val redisContainer = DockerContainer("redis:latest")
    .withPorts((RedisAdvertisedPort, Some(RedisAdvertisedPort)))
    .withReadyChecker(
      DockerReadyChecker.LogLineContains("The server is now ready to accept connections on port 6379")
    )

  abstract override def dockerContainers: List[DockerContainer] =
    redisContainer :: super.dockerContainers
}

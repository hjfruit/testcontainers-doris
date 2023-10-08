package testcontainers.containers

import java.time.Duration
import java.util.concurrent.Callable

import scala.jdk.CollectionConverters._

import org.testcontainers.DockerClientFactory
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container

object Doris {

  /**
   * global constants
   */
  final val DefaultTag   = "1.2.1"
//  final val Username     = "root"
//  final val Password     = "root"
  final val StartTimeout = Duration.ofSeconds(30)
  final val StopTimeout  = 30
  final val NetworkName  = "testcontainers-doris-network"
  final val NetworkType  = "default"
  final val feServices   = "FE_SERVERS"
  final val feId         = "FE_ID"
  final val beAddr       = "BE_ADDR"

  final val PollInterval    = Duration.ofMillis(50)
  final val ContainerAtMost = Duration.ofSeconds(10)
  final val dorisPortAtMost = Duration.ofSeconds(5)

  /**
   * default log path
   */
  final val feLogPath = "/opt/apache-doris/fe/log"
  final val beLogPath = "/opt/apache-doris/be/log"

  // fe ==> graphd; be ==> metad
  /**
   * default fe paths
   */
  final val feMetaPath = "/opt/apache-doris/fe/doris-meta"
  final val feConfPath = "/opt/apache-doris/fe/conf"

  /**
   * default data be paths
   */
  final val beDataPath = "/opt/apache-doris/be/storage"
  final val beInitDbPath = "/docker-entrypoint-initdb.d"

  /**
   * default fe http server port
   */
  final val feHttpPort = 8030


  /**
   * default fe db port
   */
  final val feDBPort = 9030

  /**
   * default be fe correspond port
   */
  final val beExposedPort = 9040

  /**
   * default be fe correspond port
   */
  final val beFeCorrespondPort = 9010

  /**
   * be heart beat service port
   */
  final val beHeartbeatServicePort = 9050

  /**
   * default image name
   */
  final val defaultFEImageName = DockerImageName.parse("apache/doris")
  final val defaultBEImageName = DockerImageName.parse("apache/doris")

  /**
   * docker container name
   */
  final val feName = "fe"
  final val beName = "be"

  final val dockerClient: DockerClient = DockerClientFactory.lazyClient()

  final val Ryuk = "/testcontainers-ryuk"

  final lazy val TestcontainersRyukContainer = {
    val containersResponse = await()
      .atMost(Doris.ContainerAtMost)
      .pollInterval(Doris.PollInterval)
      .pollInSameThread()
      .until(
        new Callable[List[Container]] {
          override def call(): List[Container] =
            Doris.dockerClient
              .listContainersCmd()
              .exec()
              .asScala
              .toList
        },
        (cs: List[Container]) =>
          cs.filter { c =>
            c.getNames.toList.exists(_.startsWith(Ryuk))
          }.flatMap(_.getNames.headOption.toList).headOption.nonEmpty
      )
    containersResponse.headOption
  }

  final lazy val SessionId = {
    val ryukName = TestcontainersRyukContainer.map(_.getNames.headOption.toList).toList.flatten.headOption
    ryukName.map(_.stripPrefix(Ryuk + "-")).getOrElse(DockerClientFactory.SESSION_ID)
  }

  def removeTestcontainersNetwork(networkId: String): Unit =
    Doris.dockerClient
      .removeNetworkCmd(networkId)
      .exec()
}

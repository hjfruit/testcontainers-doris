package testcontainers
package containers

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.wait.strategy.{ HttpWaitStrategy, Wait }
import org.testcontainers.utility.DockerImageName

import com.github.dockerjava.api.model._

import testcontainers.containers.Doris._

object DorisFEContainer {

  def defaultPortBindings(httpPort: Int, dbPort: Int): List[PortBinding] =
    List(
      new PortBinding(
        Ports.Binding.bindPort(httpPort),
        new ExposedPort(Doris.feHttpPort) // docker container port
      ),
      new PortBinding(
        Ports.Binding.bindPort(dbPort),
        new ExposedPort(Doris.feDBPort) // docker container port
      )
    )

  def defaultVolumeBindings(absoluteHostPathPrefix: String, instanceIndex: Int = 1): List[DorisVolume] =
    List(
      DorisVolume(
        absoluteHostPathPrefix + Doris.feLogPath + instanceIndex,
        Doris.feLogPath,
        BindMode.READ_WRITE
      ),
      DorisVolume(
        absoluteHostPathPrefix + Doris.feMetaPath + instanceIndex,
        Doris.feMetaPath,
        BindMode.READ_WRITE
      )
    )
}

final class DorisFEContainer(
  feId: String,
  instanceIndex: Int,
  val containerIp: String,
  val feServicesStr: String,
  dockerImageName: DockerImageName,
  val portsBindings: List[PortBinding],
  val bindings: List[DorisVolume],
  hostName: Option[String]
) extends BaseContainer[DorisFEContainer](dockerImageName) {

  this.waitStrategy = Wait
    .forHttp("/api/health")
    .forPort(Doris.feHttpPort)

  this
    .withEnv(Doris.feServices, feServicesStr)
    .withEnv(Doris.feId, feId)
    .withCreateContainerCmdModifier(cmd =>
      cmd
        .withName(getContainerName)
        .withHostName(hostName.getOrElse(s"${Doris.feName}-$instanceIndex"))
    )

  def this(
    version: String,
    feId: String,
    containerIp: String,
    feBeCorrespondAddrs: String,
    instanceIndex: Int,
    portsBindings: List[PortBinding],
    bindings: List[DorisVolume],
    hostName: Option[String] = None
  ) =
    this(
      feId,
      instanceIndex,
      containerIp,
      feBeCorrespondAddrs,
      Doris.defaultFEImageName.withTag(version),
      portsBindings,
      bindings,
      hostName
    )

  override def getContainerName: String = Doris.feName + instanceIndex + "-" + Doris.SessionId
}

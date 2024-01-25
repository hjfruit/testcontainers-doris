package testcontainers.containers

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

import com.github.dockerjava.api.model._

object DorisBEContainer {

  def defaultPortBindings(bindingPort: Int): List[PortBinding] =
    List(
      new PortBinding(
        Ports.Binding.bindPort(bindingPort),
        new ExposedPort(Doris.beExposedPort) // docker container port
      )
    )

  def defaultVolumeBindings(absoluteHostPathPrefix: String, instanceIndex: Int = 1): List[DorisVolume] =
    List(
      DorisVolume(
        absoluteHostPathPrefix + Doris.beLogPath + instanceIndex,
        Doris.beLogPath,
        BindMode.READ_WRITE
      ),
      DorisVolume(
        absoluteHostPathPrefix + Doris.beDataPath + instanceIndex,
        Doris.beDataPath,
        BindMode.READ_WRITE
      ),
      DorisVolume(
        absoluteHostPathPrefix + Doris.beInitDbPath + instanceIndex,
        Doris.beInitDbPath,
        BindMode.READ_WRITE
      )
    )
}

final class DorisBEContainer(
  val containerIp: String,
  val feServicesStr: String,
  instanceIndex: Int,
  dockerImageName: DockerImageName,
  val portsBindings: List[PortBinding],
  val bindings: List[DorisVolume],
  hostName: Option[String]
) extends BaseContainer[DorisBEContainer](dockerImageName) {

  this.waitStrategy = Wait
    .forHttp("/api/health")
    .forPort(Doris.beExposedPort)

  this
    .withEnv(Doris.feServices, feServicesStr)
    .withEnv(Doris.beAddr, s"$containerIp:${Doris.beHeartbeatServicePort}")
//    .withStartupTimeout(Doris.StartTimeout)
    .withCreateContainerCmdModifier(cmd =>
      cmd
        .withName(getContainerName)
        .withHostName(hostName.getOrElse(s"${Doris.beName}-$instanceIndex"))
    )

  def this(
    version: String,
    containerIp: String,
    feBeCorrespondAddrs: String,
    instanceIndex: Int,
    portsBindings: List[PortBinding],
    bindings: List[DorisVolume],
    hostName: Option[String] = None
  ) =
    this(
      containerIp,
      feBeCorrespondAddrs,
      instanceIndex,
      Doris.defaultBEImageName.withTag(version),
      portsBindings,
      bindings,
      hostName
    )

  override def getContainerName: String = Doris.beName + instanceIndex + "-" + Doris.SessionId

//  this.waitStrategy = Wait.forHealthcheck()
}

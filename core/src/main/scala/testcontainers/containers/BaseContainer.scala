package testcontainers.containers

import java.net.ServerSocket
import java.util

import scala.jdk.CollectionConverters._

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

import com.github.dockerjava.api.model._

abstract class BaseContainer[T <: GenericContainer[T]](dockerImageName: DockerImageName)
    extends GenericContainer[T](dockerImageName) {

  val containerIp: String
  val feServicesStr: String
  val portsBindings: List[PortBinding]
  val bindings: List[DorisVolume]

  override def getLivenessCheckPortNumbers: util.Set[Integer] =
    portsBindings.map(_.getExposedPort.getPort).toSet.map((i: Int) => Integer.valueOf(i)).asJava

//  this.waitStrategy = Wait.forHttp("/api/health")

  this
    .withExposedPorts(portsBindings.map(p => Integer.valueOf(p.getExposedPort.getPort)): _*)
    .withCreateContainerCmdModifier(cmd =>
      cmd
        .withIpv4Address(containerIp)
        .getHostConfig
        .withPortBindings(portsBindings: _*)
        .withAutoRemove(true)
    )

  bindings.foreach(volume => this.withFileSystemBind(volume.hostPath, volume.containerPath, volume.mode))
}

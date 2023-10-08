package testcontainers.containers

import java.util

import scala.jdk.CollectionConverters._
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import com.github.dockerjava.api.model._

abstract class BaseContainer[T <: GenericContainer[T]](dockerImageName: DockerImageName)
    extends GenericContainer[T](dockerImageName) {

  val containerIp: String
  val feBeCorrespondAddrs: String
  val portsBindings: List[PortBinding]
  val bindings: List[DorisVolume]

//  override def getLivenessCheckPortNumbers: util.Set[Integer] =
//    portsBindings.map(_.getExposedPort.getPort).toSet.map(this.getMappedPort).asJava

  bindings.foreach(volume => this.withFileSystemBind(volume.hostPath, volume.containerPath, volume.mode))
}

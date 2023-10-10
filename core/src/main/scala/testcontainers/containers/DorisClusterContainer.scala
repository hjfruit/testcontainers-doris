package testcontainers.containers

import java.time.Duration
import java.util.{ List => JList }
import java.util.concurrent.{ Callable, TimeUnit }

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

import org.slf4j.LoggerFactory
import org.testcontainers.containers.{ GenericContainer, Network }
import org.testcontainers.lifecycle.Startable
import org.testcontainers.shaded.com.google.common.base.Throwables
import org.testcontainers.shaded.org.awaitility.Awaitility.await

import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Network.Ipam

import testcontainers.containers.Doris.dockerClient

object DorisClusterContainer {
  private val logger = LoggerFactory.getLogger(classOf[DorisClusterContainer])

  final class DebianContainer() extends GenericContainer[DebianContainer]("debian")
}

abstract class DorisClusterContainer(subnetIp: String, singleContainerStartUpTimeOut: Duration) extends Startable {

  import DorisClusterContainer._

  protected def gatewayIp: String = {
    if (subnetIp == null) {
      throw new IllegalStateException("subnetIp cannot be null")
    }
    if (!subnetIp.contains("/")) {
      throw new IllegalStateException("subnetIp is invalid")
    }
    val sub = subnetIp.split("/")(0)
    increaseLastIp(sub, 1)
  }

  protected val dorisNetwork: Network =
    Network
      .builder()
      .createNetworkCmdModifier { cmd =>
        cmd
          .withName(Doris.NetworkName)
          .withIpam(
            new Ipam()
              .withDriver(Doris.NetworkType)
              .withConfig(new Ipam.Config().withSubnet(subnetIp).withGateway(gatewayIp))
          )
      }
      .build()
  protected val beIpPortMapping: List[(String, Int)]
//  protected val feBeCorrespondIpMapping: List[(String, Int)]
  protected val feIdAddrMapping: List[(String, (String, Int, Int))]

//  protected lazy val beAddrs: String = generateIpAddrs(beIpPortMapping)

//  protected lazy val feAddrs: String = generateIpAddrs(feIdAddrMapping.map(_._2))

  protected lazy val feServiceStr: String =
    feIdAddrMapping.map(kv => s"${Doris.feName}${kv._1}:${kv._2._1}:${Doris.feEditLogPort}").mkString(",")

  protected def generateIpAddrs(ipPortMapping: List[(String, Int)]): String =
    ipPortMapping.map(kv => s"${kv._1}:${kv._2}").mkString(",")

  private lazy val ryukContainerId: String = {
    val containersResponse = Doris.TestcontainersRyukContainer
    containersResponse.map(_.getId).orNull
  }

  protected def increaseLastIp(ip: String, num: Int): String = {
    if (ip == null) {
      throw new IllegalStateException("IPAddress cannot be null!")
    }
    val ipSplits = ip.split("\\.").toList
    val last     = ipSplits.last.toInt
    ipSplits.updated(ipSplits.size - 1, last + num).mkString(".")
  }

  protected val bes: List[DorisBEContainer]

  protected val fes: List[DorisFEContainer]

  protected val debian = new DebianContainer()
    .withStartupTimeout(singleContainerStartUpTimeOut)
    .withCreateContainerCmdModifier(cmd => cmd.withStdinOpen(true).withTty(true).getHostConfig.withAutoRemove(true))

  def existsRunningContainer: Boolean

  private def awaitMappedPort[S <: GenericContainer[S]](container: GenericContainer[S], exposedPort: Int): Int = {
    val containerId = await()
      .atMost(Doris.dorisPortAtMost)
      .pollInterval(Doris.PollInterval)
      .pollInSameThread()
      .until(
        new Callable[String] {
          override def call(): String =
            container.getContainerId
        },
        (id: String) => id != null
      )

    if (containerId != null) {
      container.getMappedPort(exposedPort).intValue()
    } else
      throw new IllegalStateException(
        "Mapped port can only be obtained after the container is started, awaitMappedPort failed!"
      )
  }

  final override def start(): Unit = {
    val osName = System.getProperty("os.name")
    if (osName.toLowerCase().contains("mac")) {
      debian.setPrivilegedMode(true)
      debian.start()
      debian.execInContainer("sysctl -w vm.max_map_count=2000000")
    }

    fes.foreach(_.withStartupTimeout(singleContainerStartUpTimeOut).start())
    bes.foreach(_.withStartupTimeout(singleContainerStartUpTimeOut).start())
  }

  /**
   * Copy from ResourceReaper#removeContainer method
   */
  private final def stopIfExistsRyukContainer(): Unit = {
    var running = false
    try {
      val containerInfo = dockerClient.inspectContainerCmd(ryukContainerId).exec
      running = containerInfo.getState != null && true == containerInfo.getState.getRunning
    } catch {
      case e: NotFoundException =>
        logger.trace(s"Was going to stop container but it apparently no longer exists: ${ryukContainerId}")
        return
      case e: Exception =>
        logger.trace(
          s"Error encountered when checking container for shutdown (ID: $ryukContainerId) - it may not have been stopped, or may already be stopped. Root cause: ${Throwables.getRootCause(e).getMessage}"
        )
        return
    }

    if (running) try {
      logger.trace("Stopping container: ${ryukContainerId}")
      dockerClient.killContainerCmd(ryukContainerId).exec
      logger.trace(s"Stopped container: ${Doris.Ryuk.stripPrefix("/")}")
    } catch {
      case e: Exception =>
        logger.trace(
          s"Error encountered shutting down container (ID: $ryukContainerId) - it may not have been stopped, or may already be stopped. Root cause: ${Throwables.getRootCause(e).getMessage}"
        )
    }

    try dockerClient.inspectContainerCmd(ryukContainerId).exec
    catch {
      case e: Exception =>
        logger.trace(s"Was going to remove container but it apparently no longer exists: $ryukContainerId")
        return
    }

    try {
      logger.trace(s"Removing container: $ryukContainerId")
      dockerClient.removeContainerCmd(ryukContainerId).withRemoveVolumes(true).withForce(true).exec
      logger.debug(s"Removed container and associated volume(s): ${Doris.Ryuk.stripPrefix("/")}")
    } catch {
      case e: Exception =>
        logger.trace(
          s"Error encountered shutting down container (ID: $ryukContainerId) - it may not have been stopped, or may already be stopped. Root cause: ${Throwables.getRootCause(e).getMessage}"
        )
    }
  }

  final override def stop(): Unit =
    try {
      val res = Future.sequence(allContainers.map(f => Future(f)).map(_.map(_.stop())))
      Await.result(res, Doris.StopTimeout.seconds)
      stopIfExistsRyukContainer()
      if (dorisNetwork.getId != null) {
        Doris.removeTestcontainersNetwork(dorisNetwork.getId)
      }
    } catch {
      case e: com.github.dockerjava.api.exception.NotFoundException =>

      case e: Throwable =>
        logger.error("Stopped all containers failed", e)
    }

  final def allContainers: List[GenericContainer[_]] = bes ++ fes

  final def feUrlList: List[String] =
    fes.map(fe => s"http://${fe.getHost}:${awaitMappedPort(fe, Doris.feHttpPort)}")

  final def beUrlList: List[String] =
    bes.map(be => s"http://${be.getHost}:${awaitMappedPort(be, Doris.beExposedPort)}")

  final def fePortList: List[Int] = fes.map(gd => awaitMappedPort(gd, Doris.feHttpPort))

  final def bePortList: List[Int] = bes.map(md => awaitMappedPort(md, Doris.beExposedPort))

  final def beHostList: List[String] = bes.map(_.getHost)

  final def beList: List[GenericContainer[_]] = bes

  final def feList: List[GenericContainer[_]] = fes

  /**
   * *********************Java API**************************
   */
  final def getFePortList: JList[Integer] = fePortList.map(Integer.valueOf).asJava

  final def getBePortList: JList[Integer] = bePortList.map(Integer.valueOf).asJava

  final def getBeList: JList[GenericContainer[_]] = beList.asJava

  final def geFeList: JList[GenericContainer[_]] = feList.asJava

  final def getBeHostList: JList[String] = beHostList.asJava

  final def getAllContainers: JList[GenericContainer[_]] = allContainers.asJava

  final def getFeUrlList: JList[String] =
    feUrlList.asJava

  final def getBeUrlList: JList[String] =
    beUrlList.asJava

}

package testcontainers.containers

import scala.jdk.OptionConverters._

import org.slf4j.{ Logger, LoggerFactory }

object Doris1FE1FEClusterContainer {
  private val logger: Logger = LoggerFactory.getLogger(classOf[DorisClusterContainer])
}

/**
 * The simplest Doris service, with one fe, one be
 *
 * @param version
 *   The image version/tag
 * @param absoluteHostPathPrefix
 *   The prefix of your host path, eg: prefix/opt/apache-doris/be1/storage:/opt/apache-doris/be/storage,
 *   prefix/logs/fe1:/opt/apache-doris/fe/log
 */
class Doris1FE1FEClusterContainer(
  version: String = Doris.DefaultTag,
  absoluteHostPathPrefix: Option[String] = None,
  subnetIp: String = "172.28.0.0/16"
) extends DorisClusterContainer(subnetIp) {

  import Doris1FE1FEClusterContainer._

  def this(version: String, absoluteBindingPath: java.util.Optional[String]) =
    this(version, absoluteBindingPath.toScala)

  protected override val feIdAddrMapping: List[(String, (String, Int, Int))] =
    List(
      "1" -> (increaseLastIp(gatewayIp, 1), Doris.feHttpPort, Doris.feDBPort)
    )

  protected override val beIpPortMapping: List[(String, Int)] = List(
    increaseLastIp(gatewayIp, 2) -> Doris.beExposedPort
  )

//  override protected val feBeCorrespondIpMapping: List[(String, Int)] =
//    List(
//      increaseLastIp(gatewayIp, 1) -> Doris.beFeCorrespondPort
//    )

  protected override val fes: List[DorisFEContainer] = {
    val fullFeTag = s"$version-fe-x86_64"
    feIdAddrMapping.map { case (feId, addr) =>
      new DorisFEContainer(
        fullFeTag,
        feId,
        addr._1,
        feServiceStr,
        feId.toInt,
        DorisFEContainer.defaultPortBindings(addr._2, addr._3),
        absoluteHostPathPrefix.fold(List.empty[DorisVolume])(p => DorisFEContainer.defaultVolumeBindings(p))
      )
    }.map(_.withNetwork(dorisNetwork))
  }

  protected override val bes: List[DorisBEContainer] = {
    val fullBeTag = s"$version-be-x86_64"
    beIpPortMapping.map { case (ip, port) =>
      new DorisBEContainer(
        fullBeTag,
        ip,
        feServiceStr,
        1,
        DorisBEContainer.defaultPortBindings(port),
        absoluteHostPathPrefix.fold(List.empty[DorisVolume])(p => DorisBEContainer.defaultVolumeBindings(p))
      )
    }.map(_.dependsOn(fes: _*)).map(_.withNetwork(dorisNetwork))
  }

  override def existsRunningContainer: Boolean = bes.exists(_.isRunning) || fes.exists(_.isRunning)

  logger.info(s"doris fe started at ip: ${generateIpAddrs(feIdAddrMapping.map(_._2).map(e => (e._1, e._2)))}")
  logger.info(s"doris be started at ip: ${generateIpAddrs(beIpPortMapping)}")
}

val scala3_Version               = "3.3.1"
val scala2_13Version             = "2.13.12"
val scala2_12Version             = "2.12.18"
val testcontainersVersion        = "1.19.0"
val scalaCollectionCompatVersion = "2.11.0"
val logbackVersion               = "1.4.2"

val supportCrossVersionList = Seq(scala3_Version, scala2_13Version, scala2_12Version)

inThisBuild(
  List(
    scalaVersion     := supportCrossVersionList.head,
    homepage         := Some(url("https://github.com/hjfruit/testcontainers-doris")),
    licenses         := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    organization     := "io.github.jxnu-liguobin",
    organizationName := "jxnu-liguobin",
    developers       := List(
      Developer(
        id = "dengjian",
        name = "dengjian",
        email = "dengjian@hjfruit.com",
        url = url("https://github.com/dengj888")
      )
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val core = project
  .in(file("core"))
  .settings(
    name               := "testcontainers-doris",
    crossScalaVersions := supportCrossVersionList,
    scalaVersion       := scala2_13Version,
    libraryDependencies ++= Seq(
      "org.testcontainers"      % "testcontainers"          % testcontainersVersion,
      "org.scala-lang.modules" %% "scala-collection-compat" % scalaCollectionCompatVersion
    )
  )

lazy val examples = project
  .in(file("examples"))
  .settings(
    publish / skip           := true,
//    crossScalaVersions       := Nil,
    crossScalaVersions       := supportCrossVersionList,
    scalaVersion             := scala2_13Version,
    libraryDependencies ++= Seq(
      // java 11
      // ch/qos/logback/classic/spi/LogbackServiceProvider has been compiled by a more recent version of the Java Runtime (class file version 55.0)
      "ch.qos.logback" % "logback-classic" % logbackVersion
    ),
    Test / parallelExecution := false
  )
  .dependsOn(core /*% "compile->compile;test->test"*/ )

lazy val root = project
  .in(file("."))
  .settings(
    crossScalaVersions := Nil,
    publish / skip     := true
  )
  .aggregate(
    core,
    examples
  )

package testcontainers.containers

import org.testcontainers.containers.BindMode

final case class DorisVolume(
  hostPath: String,
  containerPath: String,
  mode: BindMode
)

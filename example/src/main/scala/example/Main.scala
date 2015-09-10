package example

import java.nio.file.Paths

import scala.util.Properties.envOrNone

import net.bryceanderson.http4s.dynamic.DynamicServiceLoader
import org.http4s.server.blaze.BlazeBuilder

object Main {

  def main(args: Array[String]): Unit = {
    val dir = envOrNone("HTTP4S_DYNAMIC_PATH") getOrElse "/tmp/http4s-dynamic"
    val port = (envOrNone("HTTP4S_PORT") getOrElse "8080").toInt

    println(s"Starting service at directory: $dir")
    BlazeBuilder
      .mountService(DynamicServiceLoader.vhost(Paths.get(dir)))
      .bindHttp(port, "0.0.0.0")
      .start
      .run
      .awaitShutdown()
  }
}


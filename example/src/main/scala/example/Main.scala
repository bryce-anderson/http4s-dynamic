package example

import java.nio.file.Paths

import scala.util.Properties.envOrNone

import net.bryceanderson.http4s.dynamic.DynamicServiceLoader
import org.http4s.server.blaze.BlazeBuilder

object Main {

  def main(args: Array[String]): Unit = {
    val dir = args.headOption orElse envOrNone("HTTP4S_DYNAMIC_PATH") getOrElse "/tmp/http4s-dynamic"
    println(s"Starting service at directory: $dir")
    BlazeBuilder
      .mountService(DynamicServiceLoader.vhost(Paths.get(dir)))
      .start
      .run
      .awaitShutdown()
  }
}


package example

import java.nio.file.Paths

import net.bryceanderson.http4s.dynamic.DynamicServiceLoader
import org.http4s.server.blaze.BlazeBuilder

object Main {

  def main(args: Array[String]): Unit = {
    println("Starting service.")
    BlazeBuilder
      .mountService(DynamicServiceLoader.simple(Paths.get("/tmp/http4s-dynamic")))
      .start
      .run
      .awaitShutdown()
  }
}


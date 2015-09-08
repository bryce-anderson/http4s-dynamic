package net.bryceanderson.http4s.dynamic

import java.nio.file._


import org.http4s.server.HttpService
import org.http4s.server.middleware.VirtualHost


object DynamicServiceLoader {

  val CONFIG_PATH = "CONFIG/config.txt"

  def simple(path: Path): HttpService = {
    DynamicLoader(path)(_.createService()){ ds => ds.shutdown(); HttpService.empty }
  }
}


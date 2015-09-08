package net.bryceanderson.http4s.dynamic

import java.nio.file._


import org.http4s.server.HttpService
import org.http4s.server.middleware.VirtualHost


object DynamicServiceLoader {

  val CONFIG_PATH = "CONFIG/config.txt"

  def simple(path: Path): HttpService = {
    DynamicLoader(path)(_.createService()){ ds => ds.shutdown(); HttpService.empty }
  }

  def virtualHost(path: Path): HttpService = {

    case class DHost(path: String, srvc: HttpService, dyn: DynamicService)

    var dss = List.empty[DHost]

    def makeHosts(): HttpService = {
      val next = dss.map{ case DHost(path,srvc,_) =>
          VirtualHost.exact(srvc, path)
      }

      if (next.nonEmpty) VirtualHost(next.head, next.tail: _*)
      else HttpService.empty
    }

    DynamicLoader(path){ ds =>
      dss = DHost(ds.path, ds.createService(), ds)::dss
      makeHosts()
    }{ ds =>
      ???
    }
  }

}



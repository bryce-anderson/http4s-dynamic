package net.bryceanderson.http4s.dynamic

import java.nio.file._


import org.http4s.server.HttpService
import org.http4s.server.middleware.VirtualHost

import org.log4s.getLogger

import scala.collection.mutable


object DynamicServiceLoader {

  private val logger = getLogger

  val CONFIG_PATH = "CONFIG/config.txt"

  def simple(path: Path): HttpService = {
    DynamicLoader(path)((_,ds) => ds.createService()){ (_,ds) => ds.shutdown(); HttpService.empty }
  }

  /** Create a Virtual Host based DynamicServiceLoader
    *
    * Creates a dynamic service loader that maps the path to virtual hosts
    * @param path Path to watch for new services
    * @param port Optional port expected in the Host header
    */
  def vhost(path: Path, port: Option[Int] = None): HttpService = {

    case class Element(ds: DynamicService, srvc: HttpService)

    val services = mutable.HashMap.empty[String,Element]

    def remove(host: String, ds: DynamicService): Unit = {
      services.remove(host).foreach { e =>
        e.ds.shutdown()
        logger.info(s"Removing host $host")
      }
    }

    def build(): HttpService =  {
      logger.debug(s"Rebuilding hosts: $services")

      val srvcs = services.toList.map { case (path, e) =>
        VirtualHost.exact(e.srvc, path, port)
      }

      if (srvcs.nonEmpty) VirtualHost(srvcs.head, srvcs.tail: _*)
      else HttpService.empty
    }

    def setup(host: String, ds: DynamicService): HttpService = {
      remove(host, ds)

      val srvc = ds.createService()
      services += host -> Element(ds, srvc)

      build()
    }

    def shutdown(host: String, ds: DynamicService): HttpService = {
      remove(host, ds)
      build()
    }

    DynamicLoader(path)(setup)(shutdown)
  }
}


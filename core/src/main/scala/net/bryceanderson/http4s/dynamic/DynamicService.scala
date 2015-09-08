package net.bryceanderson.http4s.dynamic

import org.http4s.server.HttpService

/**
 * Created on 9/7/15.
 */
trait DynamicService {
  val path: String
  def createService(): HttpService
  def shutdown(): Unit
}

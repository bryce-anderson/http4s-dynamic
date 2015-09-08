package example

import net.bryceanderson.http4s.dynamic.DynamicService
import org.http4s.Response
import org.http4s.server.HttpService

/**
 * Created on 9/8/15.
 */

class MyService extends DynamicService {
    override val path: String = "dynamic"

    override def shutdown(): Unit = ()

    override def createService(): HttpService = HttpService {
      case r => Response().withBody("Its me, the dynamic service!")
    }
  }
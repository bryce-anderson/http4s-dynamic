package net.bryceanderson.http4s.dynamic

import java.io.InputStream

/**
 * Created on 9/7/15.
 */
object ConfigParser {
  def parseAll(stream: InputStream): Seq[String] = {
    scala.io.Source.fromInputStream(stream)
      .getLines()
      .toSeq
      .filter(!_.isEmpty)
  }
}

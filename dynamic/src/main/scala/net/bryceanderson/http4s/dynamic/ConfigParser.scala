package net.bryceanderson.http4s.dynamic

import java.io.InputStream

import scalaz.{-\/, \/-, \/}

/**
 * Created on 9/7/15.
 */
object ConfigParser {
  case class Config(className: String, path: String)

  def parseAll(stream: InputStream): String\/Seq[Config] = {
    val lines = scala.io.Source.fromInputStream(stream)
      .getLines()
      .toSeq
      .filter(!_.isEmpty)

    lines.foldLeft[String\/Vector[Config]](\/-(Vector.empty)){ (acc, line) =>
      acc.flatMap { configs =>
        val parts = line.split(" ")
        if (parts.size != 2) -\/(s"Invalid config line: $line")
        else \/-(configs :+ Config(parts(0), parts(1)))
      }
    }
  }
}

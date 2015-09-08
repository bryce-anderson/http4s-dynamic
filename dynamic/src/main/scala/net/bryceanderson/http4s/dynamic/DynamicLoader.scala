package net.bryceanderson.http4s.dynamic

import java.io.{File, Closeable}
import java.net.URLClassLoader
import java.nio.file.{StandardWatchEventKinds => SWE, ClosedWatchServiceException, WatchEvent, FileSystems, Path}
import java.util.concurrent.atomic.AtomicReference

import org.http4s.server.HttpService

import org.log4s.getLogger

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Created on 9/7/15.
 */


private final class DynamicLoader private(
   watchPath: Path,
   append: DynamicService => HttpService,
   remove: DynamicService => HttpService
 ) extends Closeable {

  private val logger = getLogger

  private val service  = new AtomicReference[HttpService](HttpService.empty)
  private val pathMap = new mutable.HashMap[File,DynamicService]()
  private val watchService = FileSystems.getDefault().newWatchService()

  logger.info("Watching path: " + watchPath.toFile.getAbsolutePath)

  val innerService: HttpService = HttpService.lift(req => service.get()(req))


  // Initialize listening thread
  startup()

  private def addResource(path: Path): Unit = {
    logger.info("Adding resource at path " + path)

    val loader = new URLClassLoader(Array(path.toUri.toURL),
      Thread.currentThread().getContextClassLoader())

    val configs = loader.getResources(DynamicServiceLoader.CONFIG_PATH)

    if (!configs.hasMoreElements) {
      sys.error("No resources found!")
    }

    val s = configs.nextElement().openStream()

    ConfigParser.parseAll(s).foreach { name =>
      // this should be a class name
      val clazz = Class.forName(name, false, loader)

      if (!classOf[DynamicService].isAssignableFrom(clazz)) {
        logger.warn(s"Invalid class type (${clazz.getSimpleName}) at path $path. Aborting.")
      }
      else {
        val ds = clazz.newInstance().asInstanceOf[DynamicService]
        service.set(append(ds))

        pathMap += ((path.toFile,  ds))

        logger.info("Added service from path " + path)
      }
    }
  }

  private def removeResource(path: Path): Unit = {
    logger.info("Removing resource at path " + path)
    pathMap.get(path.toFile) match {
      case Some(ds) => service.set(remove(ds))
      case None     => logger.warn("Resource deleted that wasn't in watched dir.")
    }
  }

  private def isValidResource(path: Path): Boolean = {
    path.toString.toLowerCase().endsWith(".jar")
  }

  private def onPathChange(path: WatchEvent[Path]): Unit = {
    // check to see which services changed and act accordingly
    val fullPath = watchPath.resolve(path.context())
    logger.info("Path change detected: val dl = " + path.kind() + ", " + fullPath)

    if (isValidResource(path.context())) path.kind() match {
      case SWE.ENTRY_CREATE => addResource(fullPath)
      case SWE.ENTRY_DELETE => removeResource(fullPath)
      case SWE.ENTRY_MODIFY =>
        removeResource(fullPath)
        addResource(fullPath)

      case other => throw new Exception(s"Unrecognized WatchEvent: $other")
    }
  }

  private[dynamic] def startup(): Unit = {

    // add all the resources already in the path
    watchPath.toFile.listFiles().foreach { file =>
      val p = file.toPath()
      if (isValidResource(p)) addResource(p)
    }

    watchPath.register(watchService, DynamicLoader.events)

    // Need to create a daemon thread that will watch the directory for changes
    val threadWatcher = new Thread("File watcher") {
      override def run(): Unit = {
        try while (true) {
          val watchKey = watchService.take()

          watchKey.pollEvents().foreach { event =>

            event.kind() match {
              case SWE.OVERFLOW =>
                throw new Exception("Overflow in directory watcher")

              case other =>
                val fevent = event.asInstanceOf[WatchEvent[Path]]
                onPathChange(fevent)
            }
          }

          watchKey.reset()
        }
        catch {
          case e: ClosedWatchServiceException => /* NOOP */
          case e: Throwable =>
            logger.error(e)("Failure in directory watcher loop")
        }

        // shutdown
        ???
      }
    }

    threadWatcher.setDaemon(true)
    threadWatcher.start()
  }

  override def close(): Unit = {
    watchService.close()
  }
}

object DynamicLoader {
  def apply(path: Path)(add: DynamicService => HttpService)(remove: DynamicService => HttpService): HttpService = {
    new DynamicLoader(path, add, remove).innerService
  }

  private val events: Array[WatchEvent.Kind[_]] = Array(
    SWE.ENTRY_CREATE,
    SWE.ENTRY_DELETE,
    SWE.ENTRY_MODIFY,
    SWE.OVERFLOW
  )
}

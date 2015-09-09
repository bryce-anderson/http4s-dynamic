package net.bryceanderson.http4s.dynamic

import java.io.{File, Closeable}
import java.net.URLClassLoader
import java.nio.file.{StandardWatchEventKinds => SWE, ClosedWatchServiceException, WatchEvent, FileSystems, Path}
import java.util.concurrent.atomic.AtomicReference

import net.bryceanderson.http4s.dynamic.ConfigParser.Config
import org.http4s.server.HttpService

import org.log4s.getLogger

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.control.NonFatal
import scalaz.{\/-, -\/}

/**
 * Created on 9/7/15.
 */


private final class DynamicLoader private(
   watchPath: Path,
   append: (String,DynamicService) => HttpService,
   remove: (String,DynamicService) => HttpService
 ) extends Closeable {

  private case class ServicePair(mountPath: String, ds: DynamicService)

  private val logger = getLogger

  private val service  = new AtomicReference[HttpService](HttpService.empty)
  private val pathMap = new mutable.HashMap[File,ServicePair]()
  private val watchService = FileSystems.getDefault().newWatchService()

  logger.info("Watching path: " + watchPath.toFile.getAbsolutePath)

  val innerService: HttpService = HttpService.lift(req => service.get()(req))


  // Initialize
  startup()

  private def addResource(jarPath: Path): Unit = {

    val loader = new URLClassLoader(Array(jarPath.toUri.toURL),
      Thread.currentThread().getContextClassLoader())

    val configs = loader.getResources(DynamicServiceLoader.CONFIG_PATH)

    if (!configs.hasMoreElements) logger.warn(s"No resources found in $jarPath!")
    else {
      val s = configs.nextElement().openStream()

      ConfigParser.parseAll(s) match {
        case -\/(error) => logger.error(s"Error loading jar at '$jarPath': $error")
        case \/-(configs) => configs.foreach { case Config(className, mountPath) =>
        // this should be a class name
        val clazz = Class.forName(className, false, loader)

        if (!classOf[DynamicService].isAssignableFrom(clazz)) {
          logger.warn(s"Invalid class type (${clazz.getSimpleName}) at path $jarPath. Aborting.")
        }
        else {
          val ds = clazz.newInstance().asInstanceOf[DynamicService]
          try {
            service.set(append(mountPath, ds))
            pathMap += jarPath.toFile -> ServicePair(mountPath,ds)
            logger.info("Added service from path " + jarPath + " to mount point " + mountPath)
          }
          catch {
            case NonFatal(t) => logger.error(t)(s"Failed to add resource at path $jarPath")
          }
        }
      }
      }
    }
  }

  private def removeResource(path: Path): Unit = {
    logger.info("Removing resource at path " + path)
    pathMap.get(path.toFile) match {
      case Some(ServicePair(mountPath,ds)) =>
        try service.set(remove(mountPath,ds))
        catch { case NonFatal(t) => logger.error(t)(s"Error removing service for path $path") }
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
    Option(watchPath.toFile.listFiles()) match {
      case None => logger.warn("DynamicLoader directory doesn't exist.")
      case Some(files) => files.foreach { file =>
        val p = file.toPath()
        if (isValidResource(p)) addResource(p)
      }
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
          case e: ClosedWatchServiceException => /* NOOP: this should happen on shutdown */
          case e: Throwable =>
            logger.error(e)("Failure in directory watcher loop")
        }

        // shutdown
        service.set(HttpService.empty)
        pathMap.values.foreach { servicePair =>
          try remove(servicePair.mountPath, servicePair.ds)
          catch { case NonFatal(t) => logger.error("Failure shutting down service") }
        }
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
  def apply(path: Path)(add: (String,DynamicService) => HttpService)
                       (remove: (String,DynamicService) => HttpService): HttpService = {
    new DynamicLoader(path, add, remove).innerService
  }

  private val events: Array[WatchEvent.Kind[_]] = Array(
    SWE.ENTRY_CREATE,
    SWE.ENTRY_DELETE,
    SWE.ENTRY_MODIFY,
    SWE.OVERFLOW
  )
}

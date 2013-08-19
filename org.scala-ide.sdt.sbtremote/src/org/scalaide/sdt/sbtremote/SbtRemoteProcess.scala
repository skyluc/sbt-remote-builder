package org.scalaide.sdt.sbtremote

import java.io.File
import akka.actor.ActorRef
import com.typesafe.sbtrc.SbtProcess
import akka.actor.ActorSystem
import org.eclipse.core.resources.IProject
import org.scalaide.sdt.sbtremote.builder.SbtrcProperties
import scala.concurrent.Await
import com.typesafe.sbtrc.protocol.ErrorResponse
import com.typesafe.sbtrc.protocol.CompileResponse
import scala.tools.eclipse.logging.HasLogger
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.sbtrc.protocol.CompileRequest
import scala.concurrent.duration._
import com.typesafe.sbtrc.protocol.NameResponse
import com.typesafe.sbtrc.protocol.ErrorResponse
import com.typesafe.sbtrc.protocol.NameRequest
import com.typesafe.sbtrc.launching.BasicSbtProcessLauncher
import com.typesafe.sbtrc.launching.SbtBasicProcessLaunchInfo

object SbtRemoteProcess {

  // TODO: this is not thread safe
  private var cache = Map[File, SbtRemoteProcess]()

  // TODO: this is not thread safe
  def getCachedProcessFor(buildRoot: File): SbtRemoteProcess = {
    cache.get(buildRoot) match {
      case Some(process) =>
        process
      case None =>
        val actor = startRemoteSbt(buildRoot)
        val process = new SbtRemoteProcess(actor)
        cache += ((buildRoot, process))
        process
    }
  }

  class ProcessLauncher(resources: List[String]) extends BasicSbtProcessLauncher {

    override def getLaunchInfo(version: String): SbtBasicProcessLaunchInfo =
      // TODO: find out why we are not getting the right version of sbt, the value seems to be always '0.12' right now.
      // which doesn't work at all, it is not a valid sbt version
      new ProcessLaunchInfo("0.12.2", resources)

    override def sbtLauncherJar: java.io.File = new File(SbtRemotePlugin.plugin.SbtLaunchJarLocation)

  }

  class ProcessLaunchInfo(version: String, resources: List[String]) extends SbtBasicProcessLaunchInfo {

    override def controllerClasspath: Seq[java.io.File] = Nil

    override val propsFile: java.io.File = SbtrcProperties.generateFile(version, resources)

  }

  private def startRemoteSbt(buildRoot: File): ActorRef = {
    val system = ActorSystem("System")

    val child = SbtProcess(
      system,
      buildRoot,
      new ProcessLauncher(
        List(
          SbtRemotePlugin.plugin.SbtRcUiInterface012JarLocation,
          SbtRemotePlugin.plugin.SbtRcProbe012JarLocation,
          SbtRemotePlugin.plugin.SbtRcPropsJarLocation)))

    child

  }

}

class SbtRemoteProcess private (actor: ActorRef) extends HasLogger {

  implicit val timeout = Timeout(1000.seconds)

  def compile(): Boolean = {
    Await.result(actor ? CompileRequest(false), timeout.duration) match {
      case CompileResponse(n) => {
        n
      }
      case ErrorResponse(error) =>
        logger.error(s"Failed to compile: $error")
        false
    }
  }

  def getName(): String = {
    Await.result(actor ? NameRequest(sendEvents = false), timeout.duration) match {
      case NameResponse(n) => {
        n
      }
      case ErrorResponse(error) =>
        logger.error(s"Failed to get project name: $error")
        "<unknown>"
    }
  }

}
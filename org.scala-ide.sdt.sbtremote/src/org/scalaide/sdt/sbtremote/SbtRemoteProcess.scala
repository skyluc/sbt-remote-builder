package org.scalaide.sdt.sbtremote

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.tools.eclipse.logging.HasLogger
import org.scalaide.sdt.sbtremote.builder.SbtrcProperties
import com.typesafe.sbtrc.SbtProcess
import com.typesafe.sbtrc.launching.BasicSbtProcessLauncher
import com.typesafe.sbtrc.launching.SbtBasicProcessLaunchInfo
import com.typesafe.sbtrc.protocol.CompileRequest
import com.typesafe.sbtrc.protocol.CompileResponse
import com.typesafe.sbtrc.protocol.ErrorResponse
import com.typesafe.sbtrc.protocol.NameRequest
import com.typesafe.sbtrc.protocol.NameResponse
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.sbtrc.io.SbtVersionUtil

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

  class ProcessLauncher(buildRoot: File) extends BasicSbtProcessLauncher {

    override def getLaunchInfo(version: String): SbtBasicProcessLaunchInfo = {
      // TODO: version contains only the binary version (major.minor). We need the full version
      // check with the activator guys if it can be improved

      // TODO: fail if we cannot find the full version
      val fullVersion = SbtVersionUtil.findProjectSbtVersion(buildRoot).get

      version match {
        case "0.12" =>
          new ProcessLaunchInfo(fullVersion, SbtRemotePlugin.plugin.resources012, SbtRemotePlugin.plugin.controlerClasspath012)
        case "0.13" =>
          new ProcessLaunchInfo(fullVersion, SbtRemotePlugin.plugin.resources013, SbtRemotePlugin.plugin.controlerClasspath013)
      }
    }

    override def sbtLauncherJar: java.io.File = new File(SbtRemotePlugin.plugin.SbtLaunchJarLocation)

  }

  class ProcessLaunchInfo(version: String, resources: List[String], override val controllerClasspath: Seq[File]) extends SbtBasicProcessLaunchInfo {

    override val propsFile: java.io.File = SbtrcProperties.generateFile(version, resources)

  }

  private def startRemoteSbt(buildRoot: File): ActorRef = {
    val system = ActorSystem("System")

    SbtProcess(
      system,
      buildRoot,
      new ProcessLauncher(buildRoot))
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
      case NameResponse(n, _) => {
        n
      }
      case ErrorResponse(error) =>
        logger.error(s"Failed to get project name: $error")
        "<unknown>"
    }
  }

//  def getKeys(id: String): Seq[ScopedKey] = {
//    Await.result(actor ? SettingKeyRequest(KeyFilter(None, None, Some(id))), timeout.duration) match {
//      case KeyListResponse(KeyList(l),) =>
//        l
//      case ErrorResponse(error) =>
//        logger.error(s"Failed to get keys: $error")
//        Nil
//    }
//  }

}
package org.scalaide.sdt.sbtremote

import java.io.File
import java.util.{ Map => JMap }

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.tools.eclipse.logging.HasLogger

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.IProgressMonitor

import com.typesafe.sbtrc.BasicSbtProcessLauncher
import com.typesafe.sbtrc.SbtBasicProcessLaunchInfo
import com.typesafe.sbtrc.SbtProcess
import com.typesafe.sbtrc.protocol.CompileRequest
import com.typesafe.sbtrc.protocol.CompileResponse
import com.typesafe.sbtrc.protocol.ErrorResponse

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout

object RemoteBuilder {

  class ProcessLauncher extends BasicSbtProcessLauncher {

    def getLaunchInfo(version: String): com.typesafe.sbtrc.SbtBasicProcessLaunchInfo = new ProcessLaunchInfo

    def sbtLauncherJar: java.io.File = new File("/home/luc/opt/sbt/bin/sbt-launch.jar")

  }

  class ProcessLaunchInfo extends SbtBasicProcessLaunchInfo {

    def controllerClasspath: Seq[java.io.File] = Nil

    def propsFile: java.io.File = new File("/home/luc/tmp/dummy-sbt/someFile.properties")
  }

  def startRemoteSbt(project: IProject): ActorRef = {
    val system = ActorSystem("System")

    val child = SbtProcess(system, project.getLocation().toFile(), new ProcessLauncher)

    child

  }

}

class RemoteBuilder extends IncrementalProjectBuilder with HasLogger {

  private var sbtProcess: Option[ActorRef] = None

  /**
   * TODO: find better solution, this is not thread safe
   */
  private def getSbtProcess(): ActorRef = {
    sbtProcess match {
      case Some(p) =>
        p
      case None =>
        val newSbtProcess = RemoteBuilder.startRemoteSbt(getProject())
        sbtProcess = Some(newSbtProcess)
        newSbtProcess
    }
  }

  def build(kind: Int, args: JMap[String, String], monitor: IProgressMonitor): Array[IProject] = {

    implicit val timeout = Timeout(1000.seconds)

    val compilationSuccess = Await.result(getSbtProcess ? CompileRequest(false), timeout.duration) match {
      case CompileResponse(n) => {
        n
      }
      case ErrorResponse(error) =>
        logger.error(s"Failed to compile: $error")
        false
    }

    println(s"Compilation result is: $compilationSuccess")

    Array()
  }

}
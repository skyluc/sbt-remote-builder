package org.scalaide.sdt.sbtremote.builder

import java.io.File
import java.util.{ Map => JMap }

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.tools.eclipse.logging.HasLogger

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.IProgressMonitor
import org.scalaide.sdt.sbtremote.SbtRemotePlugin

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

  class ProcessLauncher(resources: List[String]) extends BasicSbtProcessLauncher {

    override def getLaunchInfo(version: String): com.typesafe.sbtrc.SbtBasicProcessLaunchInfo =
      // TODO: find out why we are not getting the right version of sbt, the value seems to be always '0.12' right now.
      // which doesn't work at all, it is not a valid sbt version
      new ProcessLaunchInfo("0.12.2", resources)

    override def sbtLauncherJar: java.io.File = new File(SbtRemotePlugin.plugin.SbtLaunchJarLocation)

  }

  class ProcessLaunchInfo(version: String, resources: List[String]) extends SbtBasicProcessLaunchInfo {

    override def controllerClasspath: Seq[java.io.File] = Nil

    override val propsFile: java.io.File = SbtrcProperties.generateFile(version, resources)

  }

  def startRemoteSbt(project: IProject): ActorRef = {
    val system = ActorSystem("System")

    val child = SbtProcess(
      system,
      project.getLocation().toFile(),
      new ProcessLauncher(
        List(
          SbtRemotePlugin.plugin.SbtRcControllerJarLocation,
          SbtRemotePlugin.plugin.SbtShimUiInterfaceJarLocation,
          SbtRemotePlugin.plugin.SbtRcPropsJarLocation)))

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

  override def build(kind: Int, args: JMap[String, String], monitor: IProgressMonitor): Array[IProject] = {

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
    
    // TODO: get the compilation result (errors, ...)
    // TODO: refresh the output folders

    if (compilationSuccess) {
      getProject.getReferencedProjects()
    } else {
      Array()
    }
  }

}
package org.scalaide.sdt.sbtremote.builder

import java.util.{Map => JMap}

import scala.tools.eclipse.logging.HasLogger

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.IProgressMonitor
import org.scalaide.sdt.sbtremote.SbtRemoteProcess

class RemoteBuilder extends IncrementalProjectBuilder with HasLogger {

  private def getSbtProcess() = {
    SbtRemoteProcess.getCachedProcessFor(getProject().getLocation().toFile())
  }

  override def build(kind: Int, args: JMap[String, String], monitor: IProgressMonitor): Array[IProject] = {
    val sbtProcess = getSbtProcess

    val compilationSuccess = sbtProcess.compile()

    println(s"Compilation result is: $compilationSuccess")
//    
//    val keys = sbtProcess.getKeys("baseDirectory")
//    
//    println(s"keys:\n ${keys.mkString("\n")}")
    
    // TODO: get the compilation result (errors, ...)
    // TODO: refresh the output folders

    if (compilationSuccess) {
      getProject.getReferencedProjects()
    } else {
      Array()
    }
  }

}
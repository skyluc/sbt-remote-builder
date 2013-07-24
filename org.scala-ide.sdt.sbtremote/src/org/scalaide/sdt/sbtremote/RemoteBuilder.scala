package org.scalaide.sdt.sbtremote

import java.util.{Map => JMap}

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.IProgressMonitor

class RemoteBuilder extends IncrementalProjectBuilder {
  
  def build(kind: Int, args: JMap[String,String], monitor: IProgressMonitor): Array[IProject] = {
    println("Yeah!!!")
    Array()
  }

}
package org.scalaide.sdt.sbtremote

import scala.tools.eclipse.javaelements.ScalaCompilationUnit

object SomeInterestingCode {
  
  def numberOfTypes(scalaCompilationUnit: ScalaCompilationUnit): Int = {
    scalaCompilationUnit.getCompilationUnit.getAllTypes.length
  }

}
package org.scalaide.sdt.sbtremote.wizard

import java.io.File
import java.io.FileInputStream
import java.util.Properties
import scala.tools.eclipse.util.SWTUtils.fnToPropertyChangeListener
import org.eclipse.jface.preference.DirectoryFieldEditor
import org.eclipse.jface.preference.FieldEditor
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.jface.wizard.WizardPage
import org.eclipse.swt.SWT.NONE
import org.eclipse.swt.widgets.Composite
import org.eclipse.jface.preference.StringFieldEditor

object ImportSbtBuildRootSelectionWizardPage {

  private class SbtBuildRootFieldEditor(parent: Composite) extends DirectoryFieldEditor("buildRoot", "Sbt Build Root:", parent) {

    setErrorMessage("Unable to find 'sbt.version' in project/build.properties file.")

    override def setValidateStrategy(strategy: Int) {
      // shortcut this feature, we always want VALIDATE_ON_KEY_STROKE 
      super.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE)
    }

    override def doCheckState(): Boolean = {
      if (super.doCheckState()) {
        val buildRoot = new File(getStringValue().trim())
        val buildPropertiesFile = new File(buildRoot, "project/build.properties")
        if (buildPropertiesFile.exists) {
          val properties = new Properties()
          // TODO: need to close the stream?
          val fileInputStream = new FileInputStream(buildPropertiesFile)
          properties.load(fileInputStream)

          properties.getProperty("sbt.version") != null
        } else {
          false
        }
      } else {
        false
      }
    }
  }

}

class ImportSbtBuildRootSelectionWizardPage extends WizardPage("rootSelection", "Sbt build root", null) {

  import ImportSbtBuildRootSelectionWizardPage._

  var buildRootEditor: StringFieldEditor = _

  override def createControl(parent: Composite) {

    val topLevel = new Composite(parent, NONE)

    buildRootEditor = new SbtBuildRootFieldEditor(topLevel)
    buildRootEditor.setPage(this)
    buildRootEditor.setPropertyChangeListener {
      event: PropertyChangeEvent =>
        checkState()
    }

    setControl(topLevel)

    setPageComplete(false)
  }

  private def checkState() {
    setPageComplete(buildRootEditor.isValid())
  }

}
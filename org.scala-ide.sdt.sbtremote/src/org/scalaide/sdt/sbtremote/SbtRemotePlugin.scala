package org.scalaide.sdt.sbtremote

import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.Path

object SbtRemotePlugin {

  @volatile
  var plugin: SbtRemotePlugin = _

}

class SbtRemotePlugin extends AbstractUIPlugin {
  import SbtRemotePlugin._
  
  lazy val SbtLaunchJarLocation = libOsLocationFromBundle("sbt-launch.jar")
  lazy val SbtRcUiInterface012JarLocation = libOsLocationFromBundle("sbt-rc-ui-interface-0-12.jar")
  lazy val SbtRcProbe012JarLocation = libOsLocationFromBundle("sbt-rc-probe-0-12.jar")
  lazy val SbtRcPropsJarLocation = libOsLocationFromBundle("sbt-rc-props.jar")

  override def start(context: BundleContext) {
    super.start(context)
    plugin = this
  }

  private def libOsLocationFromBundle(fileName: String): String =
    FileLocator.toFileURL(FileLocator.find(getBundle, Path.fromPortableString("/target/lib/" + fileName), null)).getPath()

}
package org.jetbrains.sbt

import java.io.File
import scala.io.Source

/**
 * @author Pavel Fatin
 */
object Loader {
  private val JavaVM = canonicalPath(new File(new File(new File(System.getProperty("java.home")), "bin"), "java"))
  private val SbtLauncher = canonicalPath(new File("sbt-launch.jar"))
  private val SbtPlugin = canonicalPath(new File("target/scala-2.9.2/sbt-0.12/classes/"))
  private val JavaOpts = Option(System.getenv("JAVA_OPTS")).getOrElse("")

  def load(project: File): String = {
    val tempFile = File.createTempFile("sbt-structure", "xml")
    tempFile.deleteOnExit()

    val command = JavaVM + " " + JavaOpts + """ -jar """ + SbtLauncher +
      """ "; set artifactPath := new File(\"""" + canonicalPath(tempFile) +
      """\") ; apply -cp """ + SbtPlugin + """ org.jetbrains.sbt.Plugin""""

    Runtime.getRuntime.exec(command, null, project).waitFor()

    assert(tempFile.exists, "File must be created: " + tempFile.getPath)

    Source.fromFile(tempFile).getLines().mkString("\n")
  }

  private def canonicalPath(file: File): String = file.getAbsolutePath.replace('\\', '/')
}

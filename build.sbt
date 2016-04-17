lazy val typeclasses = project
  .copy(id = "typeclasses")
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

name := "typeclasses"

libraryDependencies ++= Vector(
  Library.scalaCheck % "test"
)

initialCommands := """|import com.github.rockjam.typeclasses._
                      |""".stripMargin

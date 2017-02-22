lazy val typeclasses = project
  .copy(id = "typeclasses")
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

name := "typeclasses"

libraryDependencies ++= Vector(
  "com.chuusai" %% "shapeless" % "2.3.2"
)

initialCommands := """|import com.github.rockjam.typeclasses._
                      |""".stripMargin

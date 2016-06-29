lazy val typeclasses = project
  .copy(id = "typeclasses")
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

name := "typeclasses"

libraryDependencies ++= Vector(
  "com.github.mpilquist" %% "simulacrum" % "0.7.0",
  "com.chuusai" %% "shapeless" % "2.3.0",
  Library.scalaCheck % "test"
)

initialCommands := """|import com.github.rockjam.typeclasses._
                      |""".stripMargin

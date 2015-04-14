organization in ThisBuild := "com.himuguang"

scalaVersion in ThisBuild := "2.11.6"

scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-deprecation",     // Emit warning and location for usages of deprecated APIs.
  "-feature",         // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",       // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint",           // Enable recommended additional warnings.
  "-Ywarn-adapted-args",  // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code",     // Warn when dead code is identified.
  "-Ywarn-inaccessible",  // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen"  // Warn when numerics are widened.
)

lazy val apiUtil = (project in file("api-util"))

lazy val apiServer = (project in file("api-server"))
  .dependsOn(apiUtil)
  .enablePlugins(PlayScala)

lazy val testRedirect = (project in file("test-redirect"))
  .enablePlugins(PlayScala)

lazy val root = (project in file(".")).aggregate(
  apiUtil,
  apiServer,
  testRedirect
)
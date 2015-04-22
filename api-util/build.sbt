import BuildSettings.Versions._

name := """api-util"""

version := "1.0.0"


// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "com.twitter"     %  "twitter-text"    % "1.11.1",
//  "org.scalaz"      %  "scalaz-geo_2.10" % "6.0.4",
  "org.scalatest"   %% "scalatest"       % scalatestVersion % "test"
)



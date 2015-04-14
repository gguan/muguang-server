import scalariform.formatter.preferences._

name := """api-server"""

version := "1.0.0"

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "com.github.nscala-time"    %% "nscala-time"      % "1.8.0",
  "com.typesafe.play.extras"  %% "play-geojson"     % "1.2.0",
  "com.mohiva"                %% "play-silhouette"  % "2.0",
  "com.typesafe.play"         %% "play-mailer"      % "2.4.0",
  "net.codingwell"            %% "scala-guice"      % "4.0.0-beta5",
  "org.reactivemongo"         %% "play2-reactivemongo"      % "0.10.5.0.akka23",
  "com.mohiva"                %% "play-silhouette-testkit"  % "2.0" % "test",
  "de.flapdoodle.embed"       %  "de.flapdoodle.embed.mongo" % "1.47.2" % "test",
  cache
)

//********************************************************
// Scalariform settings
//********************************************************
scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(DoubleIndentClassDeclaration, false)
  .setPreference(PreserveDanglingCloseParenthesis, true)



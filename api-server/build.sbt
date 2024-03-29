import scalariform.formatter.preferences._

name := """api-server"""

version := "1.0.0"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "com.qiniu"                 %  "qiniu-java-sdk"   % "7.0.2",
  "redis.clients"             %  "jedis"            % "2.7.2",
  "com.github.nscala-time"    %% "nscala-time"      % "2.0.0",
  "com.typesafe.play.extras"  %% "play-geojson"     % "1.2.0",
  "com.mohiva"                %% "play-silhouette"  % "2.0",
  "com.typesafe.play"         %% "play-mailer"      % "2.4.1",
  "net.codingwell"            %% "scala-guice"      % "4.0.0-beta5",
  "com.github.cb372"          %% "scalacache-redis" % "0.6.2",
  "org.reactivemongo"         %% "play2-reactivemongo"       % "0.10.5.0.akka23",
  "com.mohiva"                %% "play-silhouette-testkit"   % "2.0" % "test",
  "de.flapdoodle.embed"       %  "de.flapdoodle.embed.mongo" % "1.47.3" % "test",
  cache,
  filters
)

//********************************************************
// Scalariform settings
//********************************************************
scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)

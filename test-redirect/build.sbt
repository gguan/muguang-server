import scalariform.formatter.preferences._

name := """test-redirect"""

version := "1.0"

libraryDependencies ++= Seq(
  cache,
  ws
)


//********************************************************
// Scalariform settings
//********************************************************
scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(DoubleIndentClassDeclaration, false)
  .setPreference(PreserveDanglingCloseParenthesis, true)


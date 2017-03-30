name := "LoveIsInTheAir"

version := "1.0"

scalaVersion := "2.12.1"


val akkaVersion = "2.4.17"

val akkaHttpVersion = "10.0.4"

val akka = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-kernel" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)

val other = Seq(
  "com.typesafe" % "config" % "1.3.1",
  "org.scalatest" %% "scalatest" % "3.0.1",
  "org.json4s" %% "json4s-jackson" % "3.5.0",
  "org.mockito" % "mockito-core" % "1.9.5"
)

lazy val love = project in file(".")

libraryDependencies ++= akka ++ other

fork in run := true

mainClass in Compile := Some("pl.mowczarek.love.System")

resourceDirectory in Compile := baseDirectory.value / "conf"

resourceDirectory in Test := baseDirectory.value / "conf"

unmanagedResourceDirectories in Compile += baseDirectory.value / "src/main/resources"

unmanagedResourceDirectories in Test += baseDirectory.value / "src/main/resources"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")
import sbtavrohugger.SbtAvrohugger.autoImport.*


ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.18"

val sparkVersion = "3.5.2"

enablePlugins(SbtAvrohugger)
enablePlugins(ProtocPlugin)


lazy val root = (project in file("."))
  .settings(
    name := "spark-app",
    resolvers += "Confluent" at "https://packages.confluent.io/maven/",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
      "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
      "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
      "org.apache.kafka" % "kafka-clients" % "3.9.0",
      "io.confluent" % "kafka-avro-serializer" % "7.9.0",
      "org.apache.avro" % "avro" % "1.12.0" % "provided",
      "com.typesafe" % "config" % "1.4.3",
      "com.google.protobuf" % "protobuf-java" % "3.22.3",
      "com.google.protobuf" % "protobuf-java-util" % "3.22.3",
      "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.17" % "protobuf"
    ),
    Compile / sourceGenerators += (Compile / avroScalaGenerateSpecific).taskValue,
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    target in assembly := file("jars")
  )

assembly / assemblyMergeStrategy := {
  case PathList("module-info.class") => MergeStrategy.discard
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "reference.conf"              => MergeStrategy.concat
  case _                             => MergeStrategy.first
}

name := "me.seravkin.replacer"

version := "0.2"

scalaVersion := "2.12.6"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.bintrayRepo("seravkin", "maven")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
addCompilerPlugin("com.thoughtworks.dsl" %% "compilerplugins-bangnotation" % "1.0.0-RC10")
addCompilerPlugin("com.thoughtworks.dsl" %% "compilerplugins-reseteverywhere" % "1.0.0-RC10")

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq(
  "info.mukel" %% "telegrambot4s" % "3.0.15",
  "org.slf4j" % "slf4j-simple" % "1.7.25",

  "org.typelevel" %% "cats-core" % "1.1.0",
  "org.typelevel" %% "cats-effect" % "1.0.0-RC2",

  "com.rklaehn" % "radixtree_2.12" % "0.5.0",

  "com.thoughtworks.dsl" %% "domains-cats" % "1.0.0-RC10",
  "com.thoughtworks.dsl" %% "keywords-monadic" % "1.0.0-RC10",

  "org.tpolecat" %% "doobie-core"      % "0.5.3",
  "org.tpolecat" %% "doobie-hikari"    % "0.5.3",
  "org.tpolecat" %% "doobie-postgres"  % "0.5.3",

  "com.github.pureconfig" %% "pureconfig" % "0.9.1",

  "default" %% "me-seravkin-tg-adapter" % "0.1",

  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
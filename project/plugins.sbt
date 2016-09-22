addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.0") // advanced assets handling

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0") //live refresh

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1") // packaging for production

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.12")

addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.2.0") // templates

addSbtPlugin("com.gilt" % "sbt-dependency-graph-sugar" % "0.7.5-1") // visual dependency management

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0") // for publishing

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.2.0")

libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "0.1.3"

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M14-2")

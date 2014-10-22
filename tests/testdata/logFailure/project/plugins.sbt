addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.13")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.4")

resolvers += Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Typesafe repository mwn" at "http://repo.typesafe.com/typesafe/maven-releases/"

addSbtPlugin("org.jetbrains" % "sbt-teamcity-logger" % "0.1.0-SNAPSHOT")
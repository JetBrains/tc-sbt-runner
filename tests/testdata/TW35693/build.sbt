resolvers += "SonaType" at "https://oss.sonatype.org/content/groups/public"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.3"
)

logBuffered in Test := true

scalaVersion := "2.10.3"

parallelExecution in test := true

testGrouping <<= definedTests in Test map { tests =>
  tests.map { test =>
    import Tests._
    new Group(
      name = test.name,
      tests = Seq(test),
      runPolicy = InProcess)
  }.sortWith(_.name < _.name)
}

testOptions in Test += Tests.Setup( () => println("Setup") )

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"


# TC log failure

Minimal project reproducing a TeamCity runner failure.

## Error description

The warning messages are triggered in our case 
* when both addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.13”) and addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.4”) are in the plugins.sbt
* `build.sbt` contains enablePlugins(PlayScala)
* you run `sbt test` (sbt compile/run will not show the problem)

### Reproduce the error

* Setup TeamCity build config with sbt runner using custom config
* add build step running `sbt test`

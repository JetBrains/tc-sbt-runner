scalaVersion := "2.12.21"

val payloadMarker = "##tc-sbt-runner-payload##"

commands += Command.command("printNumber") { state =>
  println(s"$payloadMarker 123")
  state
}

commands += Command.single("printInputArg") { (state, input) =>
  println(s"$payloadMarker $input")
  state
}

package jetbrains.buildServer.sbt;

public interface SbtRunnerConstants {
  String RUNNER_TYPE = "SBT";
  String RUNNER_DISPLAY_NAME = "Simple Build Tool (Scala)";
  String RUNNER_DESCRIPTION = RUNNER_DISPLAY_NAME;

  String SBT_HOME_PARAM = "sbt.home";
  String SBT_ARGS_PARAM = "sbt.args";
  String SBT_VERSION_PARAM = "sbt.version";
}

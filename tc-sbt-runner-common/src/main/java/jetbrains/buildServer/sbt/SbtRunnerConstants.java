package jetbrains.buildServer.sbt;

public interface SbtRunnerConstants {

  String RUNNER_TYPE = "SBT";
  String RUNNER_DISPLAY_NAME = "Simple Build Tool (Scala)";
  String RUNNER_DESCRIPTION = RUNNER_DISPLAY_NAME;

  String SBT_HOME_PARAM = "sbt.home";
  String SBT_ARGS_PARAM = "sbt.args";
  String SBT_INSTALLATION_MODE_PARAM = "sbt.installationMode";

  public static final String AUTO_INSTALL_FLAG = "auto";

  String SBT_HOME = "SBT_HOME";
  String SBT_ARGS = "clean compile test";
  String SBT_DEFAULT_INSTALLATION_MODE = "Auto";
  String SBT_CUSTOM_INSTALLATION_MODE = "Custom";
}

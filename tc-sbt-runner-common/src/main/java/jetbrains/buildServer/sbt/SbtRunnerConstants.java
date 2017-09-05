package jetbrains.buildServer.sbt;

public interface SbtRunnerConstants {

  String RUNNER_TYPE = "SBT";
  String RUNNER_DISPLAY_NAME = "Simple Build Tool (Scala)";
  String RUNNER_DESCRIPTION = "Runs SBT builds";

  String SBT_HOME_PARAM = "sbt.home";
  String SBT_ARGS_PARAM = "sbt.args";
  String SBT_INSTALLATION_MODE_PARAM = "sbt.installationMode";

  public static final String AUTO_INSTALL_FLAG = "auto";

  String SBT_HOME = "SBT_HOME";
  String SBT_ARGS = "clean compile test";
  String SBT_DEFAULT_INSTALLATION_MODE = "Auto";
  String SBT_CUSTOM_INSTALLATION_MODE = "Custom";
  String DEFAULT_VALUE = "-Xmx512m -XX:ReservedCodeCacheSize=128m";
  String TEAMCITY_SBT_DEFAULT_JVM_ARGS_PROPERTY = "teamcity.sbt.defaultJvmArgs";
  String DEFAULT_SBT_COMMANDS = "clean compile test";
}

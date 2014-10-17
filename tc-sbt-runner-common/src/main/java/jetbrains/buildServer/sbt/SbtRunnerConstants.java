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
  String DEFAULT_VALUE = "-Xmx512m -XX:MaxPermSize=256m -XX:ReservedCodeCacheSize=128m " +
          "-Dsbt.log.noformat=true -Dsbt.log.format=false";
  String TEAMCITY_SBT_DEFAULT_JVM_ARGS_PROPERTY = "teamcity.sbt.defaultJvmArgs";
  String DEFAULT_SBT_COMMANDS = "clean compile test";
  String DEFAULT_SBT_JDK = "%env.JDK_16%";
}

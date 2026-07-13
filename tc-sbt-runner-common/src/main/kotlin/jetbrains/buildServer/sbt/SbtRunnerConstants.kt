package jetbrains.buildServer.sbt

object SbtRunnerConstants {
    const val RUNNER_TYPE = "SBT"
    const val RUNNER_DISPLAY_NAME = "Simple Build Tool (Scala)"
    const val RUNNER_DESCRIPTION = "Runs SBT builds"

    const val SBT_HOME_PARAM = "sbt.home"
    const val SBT_ARGS_PARAM = "sbt.args"
    const val SBT_INSTALLATION_MODE_PARAM = "sbt.installationMode"

    const val AUTO_INSTALL_FLAG = "auto"

    const val SBT_HOME = "SBT_HOME"
    const val SBT_ARGS = "clean compile test"
    const val SBT_DEFAULT_INSTALLATION_MODE = "Auto"
    const val SBT_CUSTOM_INSTALLATION_MODE = "Custom"
    const val DEFAULT_VALUE = "-Xmx512m -XX:ReservedCodeCacheSize=128m"
    const val TEAMCITY_SBT_DEFAULT_JVM_ARGS_PROPERTY = "teamcity.sbt.defaultJvmArgs"
    const val DEFAULT_SBT_COMMANDS = "clean compile test"
}

package jetbrains.buildServer.sbt

import jetbrains.buildServer.requirements.Requirement
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.RunType
import jetbrains.buildServer.serverSide.RunTypeRegistry
import jetbrains.buildServer.serverSide.TeamCityProperties
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.web.openapi.PluginDescriptor

/**
 * TeamCity loads this class by FQCN from the Spring plugin descriptor, so the class name and
 * constructor shape are the runtime compatibility boundary. Internal helpers can be idiomatic Kotlin.
 */
class SbtRunnerRunType(
    runTypeRegistry: RunTypeRegistry,
    private val pluginDescriptor: PluginDescriptor,
) : RunType() {

    init {
        runTypeRegistry.registerRunType(this)
    }

    override fun getRunnerPropertiesProcessor(): PropertiesProcessor = PropertiesProcessor { props ->
        val errors = mutableListOf<InvalidProperty>()
        val isAutoInstall = SbtRunnerConstants.AUTO_INSTALL_FLAG.equals(
            props[SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM],
            ignoreCase = true,
        )

        if (!isAutoInstall && StringUtil.isEmptyOrSpaces(props[SbtRunnerConstants.SBT_HOME_PARAM])) {
            errors += InvalidProperty(SbtRunnerConstants.SBT_HOME_PARAM, "SBT home path must be specified")
        }
        if (isAutoInstall) {
            props.remove(SbtRunnerConstants.SBT_HOME_PARAM)
        }
        errors
    }

    override fun getDescription(): String = SbtRunnerConstants.RUNNER_DESCRIPTION

    override fun getEditRunnerParamsJspFilePath(): String =
        pluginDescriptor.getPluginResourcesPath("editSbtRunParams.jsp")

    override fun getViewRunnerParamsJspFilePath(): String =
        pluginDescriptor.getPluginResourcesPath("viewSbtRunParams.jsp")

    override fun getDefaultRunnerProperties(): Map<String, String> = hashMapOf(
        SbtRunnerConstants.SBT_ARGS_PARAM to SbtRunnerConstants.DEFAULT_SBT_COMMANDS,
        SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM to SbtRunnerConstants.AUTO_INSTALL_FLAG,
        "jvmArgs" to SBT_JVM_ARGS,
    )

    override fun getType(): String = SbtRunnerConstants.RUNNER_TYPE

    override fun getDisplayName(): String = SbtRunnerConstants.RUNNER_DISPLAY_NAME

    override fun describeParameters(parameters: Map<String, String>): String =
        parameters[SbtRunnerConstants.SBT_ARGS_PARAM].takeUnless(StringUtil::isEmptyOrSpaces).orEmpty()

    override fun getRunnerSpecificRequirements(runParameters: Map<String, String>): List<Requirement> = emptyList()

    companion object {
        @JvmField
        val SBT_JVM_ARGS: String = TeamCityProperties.getProperty(
            SbtRunnerConstants.TEAMCITY_SBT_DEFAULT_JVM_ARGS_PROPERTY,
            SbtRunnerConstants.DEFAULT_VALUE,
        )
    }
}

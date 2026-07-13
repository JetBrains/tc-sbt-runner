package jetbrains.buildServer.sbt

import jetbrains.buildServer.agent.AgentBuildRunnerInfo
import jetbrains.buildServer.agent.BuildAgentConfiguration
import jetbrains.buildServer.agent.runner.CommandLineBuildService
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory

/**
 * TeamCity loads this factory by FQCN from the agent Spring descriptor. The class name and
 * constructor are the runtime compatibility boundary; the created service remains internal.
 */
class SbtRunnerBuildServiceFactory(
    private val ivyCacheProvider: IvyCacheProvider,
) : CommandLineBuildServiceFactory {

    override fun createService(): CommandLineBuildService = SbtRunnerBuildService(ivyCacheProvider)

    override fun getBuildRunnerInfo(): AgentBuildRunnerInfo = object : AgentBuildRunnerInfo {
        override fun getType(): String = SbtRunnerConstants.RUNNER_TYPE

        override fun canRun(agentConfiguration: BuildAgentConfiguration): Boolean = true
    }
}

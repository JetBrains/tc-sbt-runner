package jetbrains.buildServer.sbt;


import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class SbtRunnerBuildServiceFactory implements CommandLineBuildServiceFactory {

    private IvyCacheProvider myIvyCacheProvider;
    private static final Logger LOG = Logger.getLogger(SbtRunnerBuildServiceFactory.class.getName());

    public SbtRunnerBuildServiceFactory(@NotNull IvyCacheProvider ivyCacheProvider) {
        myIvyCacheProvider = ivyCacheProvider;
        LOG.info("SbtRunnerBuildServiceFactory.constructor");
    }

    @NotNull
    public CommandLineBuildService createService() {
        LOG.info("SbtRunnerBuildServiceFactory.createService");
        return new SbtRunnerBuildService(myIvyCacheProvider);
    }

    @NotNull
    public AgentBuildRunnerInfo getBuildRunnerInfo() {
        LOG.debug("SbtRunnerBuildServiceFactory.getBuildRunnerInfo");
        return new AgentBuildRunnerInfo() {
            @NotNull
            public String getType() {
                return SbtRunnerConstants.RUNNER_TYPE;
            }

            public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
                return true;
            }
        };
    }


}

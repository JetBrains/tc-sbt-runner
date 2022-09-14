package jetbrains.buildServer.sbt;


import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;


public class SbtRunnerBuildServiceFactory implements CommandLineBuildServiceFactory {

    private IvyCacheProvider myIvyCacheProvider;
    private static final Logger LOG = Logger.getLogger(SbtRunnerBuildServiceFactory.class.getName());

    public SbtRunnerBuildServiceFactory(@NotNull IvyCacheProvider ivyCacheProvider) {
        myIvyCacheProvider = ivyCacheProvider;
    }

    @NotNull
    public CommandLineBuildService createService() {
        return new SbtRunnerBuildService(myIvyCacheProvider);
    }

    @NotNull
    public AgentBuildRunnerInfo getBuildRunnerInfo() {
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

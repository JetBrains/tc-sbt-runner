package jetbrains.buildServer.sbt.test.integration;

import jetbrains.buildServer.RunnerTest2Base;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import jetbrains.buildServer.sbt.SbtRunnerConstants;
import jetbrains.buildServer.sbt.SbtRunnerRunType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;


public class SbtRunnerTest extends RunnerTest2Base {
    @NotNull
    @Override
    protected String getRunnerType() {
        return SbtRunnerConstants.RUNNER_TYPE;
    }

    @Override
    protected String getTestDataSuffixPath() {
        return "";
    }


    @Override
    protected String getTestDataPrefixPath() {
        return "testData/";
    }


    @Override
    protected final void addConfigLocations(final List<String> list) {
        list.add("classpath*:/META-INF/build-agent-plugin-tc-sbt*.xml");
    }

    @Override
    @BeforeMethod
    protected void setUp1() throws Throwable {
        super.setUp1();

        final String testBasedir = FileUtil.getCanonicalFile(getCurrentDir()).getAbsolutePath();
        getBuildType().setCheckoutDirectory(testBasedir);

        setBuildSystemProperty(AgentRuntimeProperties.AGENT_WORK_DIR, testBasedir);

        setPartialMessagesChecker();

    }

    public void testAutoInstallation() throws Throwable {
        setRunnerParameter(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, "auto");
        setRunnerParameter(SbtRunnerConstants.SBT_ARGS_PARAM, "clean compile test");
        setRunnerParameter("teamcity.build.workingDir", getTestDataPath("base").getPath());
        setRunnerParameter("jvmArgs", SbtRunnerRunType.SBT_JVM_ARGS);
        final SFinishedBuild build = doTest(null);
        dumpBuildLogLocally(build);
        Assert.assertTrue(getBuildLog(build).contains("SBT installation"));
        Assert.assertTrue(build.getBuildStatus().isSuccessful());
    }

    @Test
    public void testCompileError() throws Throwable {
        setRunnerParameter(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, "auto");
        setRunnerParameter(SbtRunnerConstants.SBT_ARGS_PARAM, "clean compile test");
        setRunnerParameter("teamcity.build.workingDir", getTestDataPath("compileerror").getPath());
        setRunnerParameter("jvmArgs", SbtRunnerRunType.SBT_JVM_ARGS);
        final SFinishedBuild build = doTest(null);
        dumpBuildLogLocally(build);
        Assert.assertTrue(build.getBuildStatus().isFailed());
        Assert.assertEquals(build.getFailureReasons().get(0).getDescription(), "Compilation error: Scala compiler");
    }


    public void testCompileSubProject() throws Throwable {
        setRunnerParameter(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, "auto");
        setRunnerParameter(SbtRunnerConstants.SBT_ARGS_PARAM, "backend/compile");
        setRunnerParameter("teamcity.build.workingDir", getTestDataPath("subproject").getPath());
        setRunnerParameter("jvmArgs", SbtRunnerRunType.SBT_JVM_ARGS);
        final SFinishedBuild build = doTest(null);
        dumpBuildLogLocally(build);
        Assert.assertTrue(build.getBuildStatus().isSuccessful());
    }


}

package jetbrains.buildServer.sbt.test.server;

import jetbrains.buildServer.sbt.SbtRunnerBuildService;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.testng.annotations.Test;

public class OutputFilterTest {

    @Test
    public void checkLineExcludePattern_1() {
        Assert.assertFalse(checkFind("##teamcity[compilationStarted compiler='Scala compiler']"));
    }

    @Test
    public void checkLineExcludePattern_2() {
        Assert.assertFalse(checkFind("##teamcity[message status='NORMAL' flowId='26' text='All initially invalidated sources: Set()|n']"));
    }

    @Test
    public void checkLineExcludePattern_3() {
        Assert.assertFalse(checkFind("blah-blah-blah [warn] warn message"));
    }

    @Test
    public void checkLineExcludePattern_4() {
        Assert.assertTrue(checkFind("[info]"));
    }

    @Test
    public void checkLineExcludePattern_5() {
        Assert.assertTrue(checkFind("[info] Loading global plugins from /private/var/folders/9t/wh8psrsd0jg5z8h7wp_ss39h0000gn/T/test-2137710771/agentTmp/agent-sbt/plugins"));
    }

    @Test
    public void checkLineExcludePattern_Info() {
        Assert.assertTrue(checkFind("[info] checking for changes"));
    }

    @Test
    public void checkLineExcludePattern_Debug() {
        Assert.assertTrue(checkFind("[debug] All initially invalidated sources: Set()"));
    }


    @Test
    public void checkLineExcludePattern_DebugMultiLine() {
        Assert.assertTrue(checkFind("[debug] \n" +
                "[debug] Initial source changes: \n" +
                "[debug] \tremoved:Set()\n" +
                "[debug] \tadded: Set()\n" +
                "[debug] \tmodified: Set()\n" +
                "[debug] Removed products: Set()\n" +
                "[debug] Modified external sources: Set()\n" +
                "[debug] Modified binary dependencies: Set()\n" +
                "[debug] Initial directly invalidated sources: Set()\n" +
                "[debug] \n" +
                "[debug] Sources indirectly invalidated by:\n" +
                "[debug] \tproduct: Set()\n" +
                "[debug] \tbinary dep: Set()\n" +
                "[debug] \texternal source: Set()"));
    }

    @Test
    public void checkLineExcludePattern_Error() {
        Assert.assertTrue(checkFind("[error] error message"));
    }

    @Test
    public void checkLineExcludePattern_Error_MultiLine() {
        Assert.assertTrue(checkFind("[error] error message\n [error] \n [error] \n"));
    }

    @Test
    public void checkLineExcludePattern_Warn() {
        Assert.assertTrue(checkFind("[warn] warn message"));
    }


    private boolean checkFind(@NotNull String example) {
        return SbtRunnerBuildService.LINES_TO_EXCLUDE.matcher(example).find();
    }
}

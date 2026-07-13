package jetbrains.buildServer.sbt.test.server

import jetbrains.buildServer.sbt.SbtRunnerBuildService
import org.testng.Assert
import org.testng.annotations.Test

class OutputFilterTest {

    @Test
    fun checkLineExcludePattern_1() {
        Assert.assertFalse(checkFind("##teamcity[compilationStarted compiler='Scala compiler']"))
    }

    @Test
    fun checkLineExcludePattern_2() {
        Assert.assertFalse(checkFind("##teamcity[message status='NORMAL' flowId='26' text='All initially invalidated sources: Set()|n']"))
    }

    @Test
    fun checkLineExcludePattern_3() {
        Assert.assertFalse(checkFind("blah-blah-blah [warn] warn message"))
    }

    @Test
    fun checkLineExcludePattern_4() {
        Assert.assertFalse(checkFind("[info]"))
    }

    @Test
    fun checkLineExcludePattern_5() {
        Assert.assertFalse(checkFind("[info] Loading global plugins from /private/var/folders/9t/wh8psrsd0jg5z8h7wp_ss39h0000gn/T/test-2137710771/agentTmp/agent-sbt/plugins"))
    }

    @Test
    fun checkLineExcludePattern_Info() {
        Assert.assertFalse(checkFind("[info] checking for changes"))
    }

    @Test
    fun checkLineExcludePattern_Debug() {
        Assert.assertFalse(checkFind("[debug] All initially invalidated sources: Set()"))
    }

    @Test
    fun checkLineExcludePattern_DebugMultiLine() {
        Assert.assertFalse(
            checkFind(
                """
                [debug] 
                [debug] Initial source changes: 
                [debug] 	removed:Set()
                [debug] 	added: Set()
                [debug] 	modified: Set()
                [debug] Removed products: Set()
                [debug] Modified external sources: Set()
                [debug] Modified binary dependencies: Set()
                [debug] Initial directly invalidated sources: Set()
                [debug] 
                [debug] Sources indirectly invalidated by:
                [debug] 	product: Set()
                [debug] 	binary dep: Set()
                [debug] 	external source: Set()
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun checkLineExcludePattern_Error() {
        Assert.assertTrue(checkFind("[error] error message"))
    }

    @Test
    fun checkLineExcludePattern_Error_MultiLine() {
        Assert.assertTrue(checkFind("[error] error message\n [error] \n [error] \n"))
    }

    @Test
    fun checkLineExcludePattern_Warn() {
        Assert.assertTrue(checkFind("[warn] warn message"))
    }

    private fun checkFind(example: String): Boolean =
        SbtRunnerBuildService.LINES_TO_EXCLUDE.matcher(example).find()
}

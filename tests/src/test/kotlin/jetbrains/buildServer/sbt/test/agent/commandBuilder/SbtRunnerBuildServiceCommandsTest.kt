package jetbrains.buildServer.sbt.test.agent.commandBuilder

import jetbrains.buildServer.sbt.SbtCommandFileContentBuilder
import jetbrains.buildServer.sbt.SbtRunnerBuildService
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Fast command-file formatting coverage for [jetbrains.buildServer.sbt.SbtRunnerBuildService.Companion.prepareArgs].
 *
 * These tests assert the generated command-file text without starting sbt. For end-to-end checks that
 * compare the generated command file with direct sbt behavior, see [SbtRunnerBuildServiceCommandParsingIntegrationTest].
 *
 * @see SbtRunnerBuildServiceCommandParsingIntegrationTest
 */
class SbtRunnerBuildServiceCommandsTest {

    @Test
    fun prepareArgs_keeps_simple_commands_as_separate_sbt_commands() {
        assertPrepareArgs(
            """clean compile test""",
            """
            ; clean
            ; compile
            ; test
            """.trimIndent(),
        )
    }

    @Test
    fun prepareArgs_keeps_semicolon_prefixed_commands_unchanged() {
        assertPrepareArgs(
            """;clean;set scalaVersion:="3.3.8";compile;test""",
            """;clean;set scalaVersion:="3.3.8";compile;test""",
        )
    }

    @Test
    fun prepareArgs_keeps_semicolon_prefixed_commands_unchanged_with_spaces() {
        assertPrepareArgs(
            """; clean ; set scalaVersion := "3.3.8" ; compile ; test""",
            """; clean ; set scalaVersion := "3.3.8" ; compile ; test""",
        )
    }

    @Test
    fun prepareArgs_trims_but_does_not_rewrite_semicolon_prefixed_commands() {
        assertPrepareArgs(
            """ ;clean;compile;test """.trimIndent(),
            """;clean;compile;test""",
        )
    }

    @Test
    fun prepareArgs_adds_missing_leading_semicolon_to_semicolon_chain() {
        assertPrepareArgs(
            """printNumber;printInputArg inline-value""",
            """;printNumber;printInputArg inline-value""",
        )
    }

    @Test
    fun prepareArgs_adds_missing_leading_semicolon_to_spaced_semicolon_chain() {
        assertPrepareArgs(
            """clean ; set scalaVersion:="2.11.6" ; compile ; test""",
            """;clean ; set scalaVersion:="2.11.6" ; compile ; test""",
        )
    }

    @Test
    fun prepareArgs_preserves_quotes_inside_simple_command_tokens_unquoted() {
        assertPrepareArgs(
            """ set scalaVersion:="3.3.8" """.trimIndent(),
            """
            ; set
            ; scalaVersion:="3.3.8"
            """.trimIndent(),
        )
    }

    @Test
    fun prepareArgs_preserves_quotes_inside_simple_command_tokens() {
        assertPrepareArgs(
            """ "set scalaVersion := \"3.3.8\"" """.trimIndent(),
            """
            ; set scalaVersion := "3.3.8"
            """.trimIndent(),
        )
    }

    // [TW-40945](https://youtrack.jetbrains.com/issue/TW-40945)
    @Test
    fun prepareArgs_treats_single_quoted_commands_with_spaces_as_single_sbt_commands() {
        assertPrepareArgs(
            """'project toto' 'set scalaVersion := "3.3.8"' clean compile test""",
            """
            ; project toto
            ; set scalaVersion := "3.3.8"
            ; clean
            ; compile
            ; test
            """.trimIndent(),
        )
    }

    @Test
    fun prepareArgs_treats_quoted_command_with_spaces_as_single_sbt_command() {
        assertPrepareArgs(
            """ "testOnly org.example.MyTest -- -t \"my test name\" """",
            """
            ; testOnly org.example.MyTest -- -t "my test name"
            """.trimIndent(),
        )
    }

    @Test
    fun prepareArgs_keeps_semicolon_inside_quoted_command_as_part_of_command() {
        assertPrepareArgs(
            """ "set name :=\"name with ; inside\"" """,
            """
            ; set name :="name with ; inside"
            """.trimIndent(),
        )
    }

    @Test
    fun prepareArgs_allows_mixing_simple_and_quoted_commands() {
        assertPrepareArgs(
            """clean "testOnly example.Test -- -t \"specific test\"" compile""",
            """
            ; clean
            ; testOnly example.Test -- -t "specific test"
            ; compile
            """.trimIndent(),
        )
    }

    @Test
    fun build_separates_runner_prologue_from_user_commands_with_newline() {
        val actual = SbtCommandFileContentBuilder.build(
            sbtCommands = "clean \"testOnly example.Test -- -t \\\"specific test\\\"\"",
            prologueCommands = listOf(
                """apply -cp "/agent temp/tc_plugin/sbt-teamcity-logger.jar" jetbrains.buildServer.sbtlogger.SbtTeamCityLogger""",
                "sbt-teamcity-logger",
            ),
        )

        Assert.assertEquals(
            actual,
            """
            ; apply -cp "/agent temp/tc_plugin/sbt-teamcity-logger.jar" jetbrains.buildServer.sbtlogger.SbtTeamCityLogger ; sbt-teamcity-logger
            ; clean
            ; testOnly example.Test -- -t "specific test"
            """.trimIndent(),
        )
    }

    private fun assertPrepareArgs(sbtCommandsInput: String, expectedPreparedArgs: String) {
        val actual = SbtRunnerBuildService.prepareArgs(sbtCommandsInput.trimIndent())
        Assert.assertEquals(actual, expectedPreparedArgs)
    }
}

package jetbrains.buildServer.sbt.test.agent.commandBuilder

import jetbrains.buildServer.sbt.SbtCommandFileContentBuilder
import jetbrains.buildServer.sbt.test.agent.commandBuilder.SbtRunnerBuildServiceCommandParsingIntegrationTest.Companion.Markers.PAYLOAD_MARKER
import org.testng.Assert
import org.testng.Reporter
import org.testng.annotations.Test
import java.io.File

/**
 * End-to-end coverage for command files produced by [jetbrains.buildServer.sbt.SbtCommandFileContentBuilder].
 *
 * Each scenario runs real sbt twice:
 * 1. with direct sbt arguments that represent what a terminal shell would pass after parsing quotes and whitespace
 * 2. with the command file generated from the TeamCity runner input.
 *
 * A lightweight command-file parsing & formatting coverage is located in [SbtRunnerBuildServiceCommandsTest]
 *
 * Add cases here when the generated file needs to be checked against real sbt behavior.
 *
 * @see SbtRunnerBuildServiceCommandsTest
 */
class SbtRunnerBuildServiceCommandParsingIntegrationTest {

    companion object {
        private object Markers {
            const val PRINT_INPUT_USAGE_MARKER = "printInputArg usage"


            // ATTENTION: should be the same as in tests/testdata/commandParsing/build.sbt
            private const val PAYLOAD_MARKER = "##tc-sbt-runner-payload##"

            /**
             * The fixture commands prefix relevant output with [PAYLOAD_MARKER]e so [extractPayloadLines] can ignore unrelated sbt log lines.
             * This test assumes only commands from the command-parsing fixture emit that marker.
             */
            fun extractPayloadLines(sbtProcessOutput: String): List<String> {
                val lines = sbtProcessOutput.lineSequence().map(String::trim)
                val linesWithPayload = lines.filter { it.startsWith(PAYLOAD_MARKER) }
                return linesWithPayload.map { it.removePrefix(PAYLOAD_MARKER).trimStart() }.toList()
            }
        }

        private val commandParsingSbtProject: File = File(RepoPaths.testsModule, "testdata/commandParsing")
        private val commandParsingTarget: File = File(RepoPaths.testsModule, "target/sbt-command-parsing")
        private val commandParsingCache: File = File(commandParsingTarget, "cache")
    }

    @Test
    fun `simple commands separated with spaces`() = assertSuccessfulCommand(
        runnerInput = "printNumber printNumber",
        directSbtArguments = listOf("printNumber", "printNumber"),
        expectedPayload = listOf("123", "123"),
    )

    @Test
    fun `double quoted command with spaces`() = assertSuccessfulCommand(
        runnerInput = """ "printInputArg arg1" """,
        directSbtArguments = listOf("printInputArg arg1"),
        expectedPayload = listOf("arg1"),
    )

    @Test
    fun `single quoted command with spaces`() = assertSuccessfulCommand(
        runnerInput = "'printInputArg arg1 arg2'",
        directSbtArguments = listOf("printInputArg arg1 arg2"),
        expectedPayload = listOf("arg1 arg2"),
    )

    @Test
    fun `mixed simple and quoted commands`() = assertSuccessfulCommand(
        runnerInput = """printNumber "printInputArg quoted value" printNumber""",
        directSbtArguments = listOf("printNumber", "printInputArg quoted value", "printNumber"),
        expectedPayload = listOf("123", "quoted value", "123"),
    )

    @Test
    fun `leading semicolon chain`() = assertSuccessfulCommand(
        runnerInput = ";printNumber;printInputArg semicolon value;printNumber",
        directSbtArguments = listOf("printNumber", "printInputArg semicolon value", "printNumber"),
        expectedPayload = listOf("123", "semicolon value", "123"),
    )

    @Test
    fun `inline semicolon without leading semicolon`() = assertSuccessfulCommand(
        runnerInput = "printNumber;printInputArg inline-value",
        directSbtArguments = listOf("printNumber", "printInputArg inline-value"),
        expectedPayload = listOf("123", "inline-value"),
    )

    @Test
    fun `semicolon chain with quoted assignment`() = assertSuccessfulCommand(
        runnerInput = """;set scalaVersion:="2.12.21";printNumber""",
        directSbtArguments = listOf("""set scalaVersion:="2.12.21"""", "printNumber"),
        expectedPayload = listOf("123"),
    )

    @Test
    fun `unquoted argument form`() = assertFailingCommand(
        runnerInput = "printInputArg hello",
        directSbtArguments = listOf("printInputArg", "hello"),
    )

    @Test
    fun `mixed split after valid command`() = assertFailingCommand(
        runnerInput = "printNumber printInputArg hello",
        directSbtArguments = listOf("printNumber", "printInputArg", "hello"),
    )

    /**
     * Runs the same scenario through direct sbt [directSbtArguments] and through the
     * runner-generated command file built by [jetbrains.buildServer.sbt.SbtCommandFileContentBuilder], then verifies both
     * executions succeed and produce the same [expectedPayload].
     *
     * @param runnerInput Raw value of the TeamCity `SBT commands:` UI field, converted into a command file.
     * @param directSbtArguments Equivalent direct sbt argv used as the behavioral baseline.
     * @param expectedPayload Unmarked output lines that identify the relevant successful command effects.
     */
    private fun assertSuccessfulCommand(
        runnerInput: String,
        directSbtArguments: List<String>,
        expectedPayload: List<String>,
    ) {
        val name = currentTestName()
        val baseline = runSbtDirectly(name, SbtRunVariant.BASELINE, directSbtArguments)
        Assert.assertEquals(
            baseline.exitCode,
            0,
            "Baseline sbt run failed for '$name'.\n${baseline.output}",
        )

        val actual = runSbtWithCommandFile(name, SbtRunVariant.RUNNER, SbtCommandFileContentBuilder.build(runnerInput))
        Assert.assertEquals(
            actual.exitCode,
            0,
            "Runner-style sbt run failed for '$name'.\n${actual.output}",
        )

        val baselinePayload = Markers.extractPayloadLines(baseline.output)
        val actualPayload = Markers.extractPayloadLines(actual.output)

        Assert.assertEquals(
            baselinePayload,
            expectedPayload,
            "Baseline payload did not match expected output for '$name'.\n${baseline.output}",
        )
        Assert.assertEquals(
            actualPayload,
            baselinePayload,
            "Runner-style payload did not match direct sbt behavior for '$name'.\n" +
                    "Actual output:\n${actual.output}\nBaseline output:\n${baseline.output}",
        )
    }

    private fun assertFailingCommand(
        runnerInput: String,
        directSbtArguments: List<String>,
    ) {
        val name = currentTestName()
        val baseline = runSbtDirectly(name, SbtRunVariant.BASELINE, directSbtArguments)
        val actual = runSbtWithCommandFile(name, SbtRunVariant.RUNNER, SbtCommandFileContentBuilder.build(runnerInput))

        Assert.assertNotEquals(
            baseline.exitCode,
            0,
            "Baseline sbt run unexpectedly succeeded for '$name'.\n${baseline.output}",
        )
        Assert.assertNotEquals(
            actual.exitCode,
            0,
            "Runner-style sbt run unexpectedly succeeded for '$name'.\n${actual.output}",
        )
        Assert.assertEquals(
            Markers.extractPayloadLines(actual.output),
            Markers.extractPayloadLines(baseline.output),
            """Runner-style successful payload before failure differed from baseline for '$name'.
              |Actual output:
              |${actual.output}
              |Baseline output:
              |${baseline.output}
              |""".trimMargin(),
        )
        Assert.assertTrue(
            baseline.output.contains(Markers.PRINT_INPUT_USAGE_MARKER),
            "Baseline failure did not contain expected marker '${Markers.PRINT_INPUT_USAGE_MARKER}'.\n${baseline.output}",
        )
        Assert.assertTrue(
            actual.output.contains(Markers.PRINT_INPUT_USAGE_MARKER),
            "Runner-style failure did not contain expected marker '${Markers.PRINT_INPUT_USAGE_MARKER}'.\n${actual.output}",
        )
    }

    private fun currentTestName(): String {
        val testResult = requireNotNull(Reporter.getCurrentTestResult()) {
            "Current TestNG result is not available"
        }
        return testResult.method.methodName
    }

    private enum class SbtRunVariant(val directorySuffix: String) {
        BASELINE("baseline"),
        RUNNER("runner"),
    }

    private fun runSbtDirectly(
        caseName: String,
        variant: SbtRunVariant,
        directSbtArguments: List<String>
    ): SbtProcessResult {
        val runDirectory = prepareRunDirectory(caseName, variant)
        return runSbt(caseName, variant) {
            SbtProcessRunner.run(runDirectory, commandParsingCache, directSbtArguments)
        }
    }

    private fun runSbtWithCommandFile(
        caseName: String,
        variant: SbtRunVariant,
        commandsContent: String
    ): SbtProcessResult {
        val runDirectory = prepareRunDirectory(caseName, variant)

        val commandsFile = File(runDirectory, "commands.file")
        commandsFile.writeText(commandsContent, Charsets.UTF_8)

        return runSbt(caseName, variant) {
            SbtProcessRunner.runWithCommandsFile(runDirectory, commandParsingCache, commandsFile)
        }
    }

    private fun runSbt(
        caseName: String,
        variant: SbtRunVariant,
        run: () -> SbtProcessResult
    ): SbtProcessResult {
        try {
            return run()
        } catch (e: SbtProcessTimeoutException) {
            Assert.fail(
                """sbt process timed out after ${e.timeout} for '$caseName' (${variant.directorySuffix}).
                  |Output so far:
                  |${e.output}
                  |""".trimMargin(),
            )
            throw e
        }
    }

    private fun prepareRunDirectory(caseName: String, variant: SbtRunVariant): File {
        val directory = File(commandParsingTarget, "${caseName.toSafeFileName()}-${variant.directorySuffix}")
        directory.deleteRecursively()
        commandParsingSbtProject.copyRecursively(directory, overwrite = true)
        return directory
    }

    private fun String.toSafeFileName(): String =
        replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_')
}

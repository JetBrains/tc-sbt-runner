package jetbrains.buildServer.sbt.test.agent.commandBuilder

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal object SbtProcessRunner {

    @Throws(SbtProcessTimeoutException::class)
    fun runWithCommandsFile(
        runDirectory: File,
        cacheDir: File,
        commandsFile: File,
        timeout: Duration = 2.minutes
    ): SbtProcessResult {
        val commandsFileAbsolutePath = commandsFile.absolutePath.replace('\\', '/')
        val sbtArguments = listOf("""< "$commandsFileAbsolutePath"""")
        return run(runDirectory, cacheDir, sbtArguments, timeout)
    }

    @Throws(SbtProcessTimeoutException::class)
    fun run(
        runDirectory: File,
        cacheDir: File,
        sbtArguments: List<String>,
        timeout: Duration = 2.minutes
    ): SbtProcessResult {
        val sbtRunConfiguration = SbtProcessRunConfiguration(
            javaExecutable = File(System.getProperty("java.home"), "bin/java"),
            sbtLauncher = RepoPaths.sbtLauncher,
            workingDirectory = runDirectory,
            globalBaseDirectory = File(runDirectory, "sbt-global"),
            bootDirectory = File(cacheDir, "boot"),
            ivyHome = File(cacheDir, "ivy"),
            outputFile = File(runDirectory, "sbt-output.log"),
            timeout = timeout,
            sbtArguments = sbtArguments
        )
        return run(sbtRunConfiguration)
    }

    private fun run(configuration: SbtProcessRunConfiguration): SbtProcessResult {
        val process = start(configuration)

        if (!process.waitFor(configuration.timeout.inWholeSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            process.waitFor(PROCESS_DESTROY_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            throw SbtProcessTimeoutException(
                timeout = configuration.timeout,
                output = configuration.outputFile.readText(Charsets.UTF_8),
            )
        }

        return SbtProcessResult(
            exitCode = process.exitValue(),
            output = configuration.outputFile.readText(Charsets.UTF_8),
        )
    }

    private fun start(configuration: SbtProcessRunConfiguration): Process {
        val processBuilder = ProcessBuilder()
        processBuilder
            .command(
                configuration.javaExecutable.absolutePath,
                "-Dsbt.global.base=${configuration.globalBaseDirectory.absolutePath}",
                "-Dsbt.boot.directory=${configuration.bootDirectory.absolutePath}",
                "-Dsbt.ivy.home=${configuration.ivyHome.absolutePath}",
                "-Dsbt.server.forcestart=true",
                "-jar",
                configuration.sbtLauncher.absolutePath,
                *configuration.sbtArguments.toTypedArray(),
            )
            .directory(configuration.workingDirectory)
            .redirectErrorStream(true)
            .redirectOutput(configuration.outputFile)
        return processBuilder.start()
    }

    private const val PROCESS_DESTROY_TIMEOUT_SECONDS = 5L
}

/**
 * Contains all parameters needed to run an sbt process (working dir, arguments, launcher path, etc...)
 */
internal data class SbtProcessRunConfiguration(
    val javaExecutable: File,
    val sbtLauncher: File,
    val workingDirectory: File,
    val globalBaseDirectory: File,
    val bootDirectory: File,
    val ivyHome: File,
    val outputFile: File,
    val timeout: Duration,
    val sbtArguments: List<String>,
)

internal data class SbtProcessResult(
    val exitCode: Int,
    val output: String,
)

internal class SbtProcessTimeoutException(
    val timeout: Duration,
    val output: String,
) : RuntimeException("sbt process timed out after $timeout")

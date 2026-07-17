package jetbrains.buildServer.sbt

import jetbrains.buildServer.RunBuildException
import jetbrains.buildServer.agent.runner.BuildServiceAdapter
import jetbrains.buildServer.agent.runner.CannotBuildCommandLineException
import jetbrains.buildServer.agent.runner.JavaCommandLineBuilder
import jetbrains.buildServer.agent.runner.JavaRunnerUtil
import jetbrains.buildServer.agent.runner.ProcessListener
import jetbrains.buildServer.agent.runner.ProcessListenerAdapter
import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.messages.ErrorData
import jetbrains.buildServer.runner.JavaRunnerConstants
import jetbrains.buildServer.util.FileUtil
import jetbrains.buildServer.util.StringUtil
import org.apache.commons.lang3.RandomStringUtils
import org.apache.log4j.Logger
import java.io.File
import java.io.IOException
import java.util.jar.JarFile
import java.util.regex.Pattern

class SbtRunnerBuildService(
    private val ivyCacheProvider: IvyCacheProvider,
) : BuildServiceAdapter() {
    private val filesToDelete = mutableListOf<File>()

    enum class SBTVersion {
        SBT_1_x,
        SBT_0_13_x,
    }

    override fun getListeners(): List<ProcessListener> = listOf(
        object : ProcessListenerAdapter() {
            private var knownSectionStarts = false

            override fun onStandardOutput(line: String) {
                if (!knownSectionStarts && KNOWN_SECTION_MESSAGE.matcher(line).find()) {
                    knownSectionStarts = true
                }
                if (LINES_TO_EXCLUDE.matcher(line).find() && knownSectionStarts) {
                    return
                }
                logger.message(line)
            }

            override fun onErrorOutput(line: String) {
                logger.warning(line)
            }
        },
    )

    @Throws(RunBuildException::class)
    override fun makeProgramCommandLine(): ProgramCommandLine {
        val jvmArgs = JavaRunnerUtil.extractJvmArgs(runnerParameters)
        val mainClassName = if (isAutoInstallMode()) installSbt() else mainClassName
        val sbtVersion = SbtVersionDetector.discoverSbtVersion(workingDirectory, sbtLauncher, jvmArgs, logger)

        copySbtTcLogger(sbtVersion)

        val javaHome = javaHome
        val sbtHome = sbtHome
        logger.message("Java home set to: $javaHome")
        logger.message("SBT home set to: $sbtHome")

        val envVars = HashMap(environmentVariables)
        envVars[SbtRunnerConstants.SBT_HOME] = sbtHome
        envVars[JavaRunnerConstants.JAVA_HOME] = javaHome
        val path = envVars[PATH]
        envVars[PATH] = javaHome + if (!StringUtil.isEmpty(path)) File.pathSeparator + path else ""

        fixSbtSocketLengthIssueIfNeeded(envVars)

        val cliBuilder = JavaCommandLineBuilder()
        cliBuilder.setJavaHome(javaHome)
        cliBuilder.setBaseDir(checkoutDirectory.absolutePath)
        cliBuilder.setSystemProperties(vmProperties)
        cliBuilder.setEnvVariables(envVars)
        cliBuilder.setJvmArgs(jvmArgs)
        cliBuilder.setClassPath(classpath)
        cliBuilder.setMainClass(mainClassName)
        cliBuilder.setProgramArgs(getCommands(sbtVersion))
        cliBuilder.setWorkingDir(workingDirectory.absolutePath)

        return buildCommandline(cliBuilder)
    }

    private fun getApplyCommand(sbtVersion: SBTVersion): String {
        val pathToPlugin = File(
            autoInstallSbtFolder +
                File.separator +
                getPatchFolder(sbtVersion) +
                File.separator +
                SBT_PATCH_JAR_NAME,
        ).absolutePath
        return "apply -cp \"${pathToPlugin.replace('\\', '/')}\" $SBT_PATCH_CLASS_NAME"
    }

    private fun getCheckStatusCommand(): String = "sbt-teamcity-logger"

    @get:Throws(RunBuildException::class)
    private val javaHome: String
        get() {
            val home = JavaRunnerUtil.findJavaHome(
                runnerParameters[JavaRunnerConstants.TARGET_JDK_HOME],
                buildParameters.allParameters,
                checkoutDirectory.absolutePath,
            ) ?: throw RunBuildException("Unable to find Java home")
            return FileUtil.getCanonicalFile(File(home)).path
        }

    private val autoInstallSbtFolder: String
        get() = agentTempDirectory.toString() + File.separator + SBT_AUTO_HOME_FOLDER

    private fun copyResources(sourcePathInJar: String, sourceName: String, destinationDir: File) {
        destinationDir.mkdirs()
        val destination = File(destinationDir, sourceName)
        FileUtil.copyResource(javaClass, sourcePathInJar + sourceName, destination)
        if (destination.exists() && destination.isFile) {
            logger.message("Resource was copied to: $destination")
        }
    }

    private fun installSbt(): String =
        try {
            logger.activityStarted(
                SBT_INSTALLATION_STEP_NAME,
                "'Auto' mode was selected in SBT runner plugin settings",
                BUILD_ACTIVITY_TYPE,
            )
            logger.message("SBT will be install to: $autoInstallSbtFolder")
            copyResources(
                "/$SBT_DISTRIB/",
                SBT_LAUNCHER_JAR_NAME,
                File(autoInstallSbtFolder + File.separator + "bin"),
            )
            logger.message("SBT home set to: $sbtHome")
            mainClassName
        } catch (e: Exception) {
            logger.internalError(ErrorData.PREPARATION_FAILURE_TYPE, "An error occurred during SBT installation", e)
            throw IllegalStateException(e)
        } finally {
            logger.activityFinished(SBT_INSTALLATION_STEP_NAME, BUILD_ACTIVITY_TYPE)
        }

    private fun copySbtTcLogger(sbtVersion: SBTVersion) {
        try {
            logger.activityStarted(SBT_TEAMCITY_LOGGER_INSTALLATION, BUILD_ACTIVITY_TYPE)
            val to = autoInstallSbtFolder + File.separator + getPatchFolder(sbtVersion)
            val from = "/$SBT_DISTRIB/" + if (sbtVersion == SBTVersion.SBT_1_x) "$SBT_1_0_PATCH_FOLDER_NAME/" else ""
            logger.message(String.format("SBT logger %s will be installed from %s to %s", SBT_PATCH_JAR_NAME, from, to))
            copyResources(from, SBT_PATCH_JAR_NAME, File(to))
        } catch (e: Exception) {
            logger.internalError(ErrorData.PREPARATION_FAILURE_TYPE, "An error occurred during SBT installation", e)
            throw IllegalStateException(e)
        } finally {
            logger.activityFinished(SBT_TEAMCITY_LOGGER_INSTALLATION, BUILD_ACTIVITY_TYPE)
        }
    }

    private fun getPatchFolder(sbtVersion: SBTVersion): String =
        if (sbtVersion == SBTVersion.SBT_1_x) {
            SBT_PATCH_FOLDER_NAME + File.separator + SBT_1_0_PATCH_FOLDER_NAME
        } else {
            SBT_PATCH_FOLDER_NAME
        }

    @get:Throws(RunBuildException::class)
    private val mainClassName: String
        get() =
            try {
                val launcher = sbtLauncher
                logger.message("SBT main class name will be retrieved from: $launcher")
                JarFile(launcher).use { jarFile ->
                    jarFile.manifest.mainAttributes.getValue("Main-Class")
                        ?: throw RunBuildException("SBT launcher manifest does not define Main-Class")
                }
            } catch (e: IOException) {
                throw RunBuildException("An error occurred during reading manifest in SBT launcher", e)
            }

    private val sbtLauncher: File
        get() = File(File(sbtHome, "bin"), SBT_LAUNCHER_JAR_NAME)

    @Throws(RunBuildException::class)
    private fun buildCommandline(cliBuilder: JavaCommandLineBuilder): ProgramCommandLine =
        try {
            cliBuilder.build()
        } catch (e: CannotBuildCommandLineException) {
            throw RunBuildException(e.message ?: "Cannot build command line")
        }

    @get:Throws(RunBuildException::class)
    private val vmProperties: Map<String, String>
        get() {
            val sysProps = HashMap<String, String>()
            sysProps["sbt.ivy.home"] = ivyCachePath
            sysProps.putAll(JavaRunnerUtil.composeSystemProperties(build, runnerContext))
            return sysProps
        }

    val classpath: String
        get() {
            val jarDir = File(sbtHome, "bin")
            return SBT_JARS.joinToString(separator = File.pathSeparator, postfix = File.pathSeparator) { jar ->
                File(jarDir, jar).absolutePath
            }
        }

    private val sbtHome: String
        get() = if (isAutoInstallMode()) {
            autoInstallSbtFolder
        } else {
            runnerParameters[SbtRunnerConstants.SBT_HOME_PARAM].orEmpty()
        }

    private fun isAutoInstallMode(): Boolean =
        SbtRunnerConstants.AUTO_INSTALL_FLAG.equals(
            runnerParameters[SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM],
            ignoreCase = true,
        )

    val ivyCachePath: String
        get() = ivyCacheProvider.cacheDir.absolutePath

    fun getCommands(sbtVersion: SBTVersion): List<String> {
        val sbtCommands = runnerParameters[SbtRunnerConstants.SBT_ARGS_PARAM]?.trim().orEmpty()
        if (StringUtil.isEmpty(sbtCommands)) {
            logger.warning("No commands specified.")
            return emptyList()
        }
        return getCommandsFromFile(sbtCommands, sbtVersion)
    }

    private fun getCommandsFromFile(sbtCommands: String, sbtVersion: SBTVersion): List<String> =
        try {
            val file = FileUtil.createTempFile(agentTempDirectory, "commands", ".file", true)
            val prologueCommands = listOf(
                getApplyCommand(sbtVersion),
                getCheckStatusCommand(),
            )
            val content = SbtCommandFileContentBuilder.build(
                sbtCommands,
                prologueCommands,
            )
            val name = file.absolutePath
            logger.activityStarted("Prepare SBT run", "Write commands to file.", BUILD_ACTIVITY_TYPE)
            logger.message("File name: $name; content: $content")
            logger.activityFinished("Prepare SBT run", BUILD_ACTIVITY_TYPE)
            FileUtil.writeFile(file, content, "UTF-8")
            val fileNameQuotes = if (File.separatorChar == '\\') "\"\"" else "\""
            listOf(String.format(RUN_INFILE_COMMANDS_FORMATTER, fileNameQuotes + name.replace('\\', '/') + fileNameQuotes))
        } catch (e: IOException) {
            LOG.warn(e.message, e)
            emptyList()
        }

    override fun afterProcessFinished() {
        super.afterProcessFinished()
        if ("false".equals(configParameters["teamcity.internal.sbt.tempFilesCleanup.enabled"], ignoreCase = true)) {
            return
        }
        filesToDelete.forEach(FileUtil::delete)
        filesToDelete.clear()
    }

    /**
     * SBT uses either XDG_RUNTIME_DIR env variable or java.io.tmpdir system property to determine its tmp dir.
     * java.io.tmpdir is equal to buildTmp and XDG_RUNTIME_DIR is usually empty, so buildTmp is used by default.
     * We use XDG_RUNTIME_DIR here to override buildTmp in some cases.
     *
     * This workaround is needed after the upgrade of sbt-launch.jar from 1.5.5 to 1.10.10.
     */
    @Throws(RunBuildException::class)
    private fun fixSbtSocketLengthIssueIfNeeded(envVars: MutableMap<String, String>) {
        if ("false".equals(configParameters["teamcity.internal.sbt.setXdgRuntimeDir.enabled"], ignoreCase = true)) {
            return
        }
        if (environmentVariables.containsKey(XDG_RUNTIME_DIR)) {
            return
        }
        if (File.separatorChar == '\\' || buildTempDirectory.absolutePath.length <= MAX_ALLOWED_TMP_DIR_LENGTH) {
            return
        }

        val subDir = RandomStringUtils.randomAlphanumeric(4).lowercase()
        val tmpDirEnv = firstNonEmptyEnvVariableOrNull("TMPDIR", "TEMP", "TMP")
        val tmpDirRoot = if (tmpDirEnv != null && tmpDirEnv.length <= MAX_ALLOWED_TMP_DIR_LENGTH - subDir.length) {
            tmpDirEnv.removeSuffix("/")
        } else {
            "/tmp"
        }
        val tmpDir = File(tmpDirRoot, subDir)
        try {
            FileUtil.createDir(tmpDir)
        } catch (e: IOException) {
            throw RunBuildException("Failed to create temp directory for SBT at ${tmpDir.absolutePath}", e)
        }
        filesToDelete.add(tmpDir)
        logger.message("$XDG_RUNTIME_DIR set to: ${tmpDir.absolutePath}")
        envVars[XDG_RUNTIME_DIR] = tmpDir.absolutePath
    }

    private fun firstNonEmptyEnvVariableOrNull(vararg envVars: String): String? =
        envVars.asSequence()
            .mapNotNull(System::getenv)
            .firstOrNull(String::isNotBlank)

    companion object {
        private val LOG = Logger.getLogger(SbtRunnerBuildServiceFactory::class.java.name)

        private const val SBT_LAUNCHER_JAR_NAME = "sbt-launch.jar"
        private const val SBT_PATCH_JAR_NAME = "sbt-teamcity-logger.jar"
        private const val SBT_PATCH_FOLDER_NAME = "tc_plugin"
        private const val SBT_1_0_PATCH_FOLDER_NAME = "1.0"
        private const val SBT_DISTRIB = "sbt-distrib"
        private const val SBT_AUTO_HOME_FOLDER = "agent-sbt"
        private const val RUN_INFILE_COMMANDS_FORMATTER = "< %s"
        private const val SBT_INSTALLATION_STEP_NAME = "SBT installation"
        private const val SBT_TEAMCITY_LOGGER_INSTALLATION = "SBT TeamCity logger installation"
        private const val PATH = "PATH"
        private const val XDG_RUNTIME_DIR = "XDG_RUNTIME_DIR"
        private const val MAX_ALLOWED_TMP_DIR_LENGTH = 54
        private val SBT_JARS = arrayOf(SBT_LAUNCHER_JAR_NAME, "classes")

        const val BUILD_ACTIVITY_TYPE = "BUILD_ACTIVITY_TYPE"

        @JvmField
        val LINES_TO_EXCLUDE: Pattern = Pattern.compile(
            "^\\[(error|warn)\\]",
            Pattern.CASE_INSENSITIVE + Pattern.MULTILINE,
        )

        @JvmField
        val KNOWN_SECTION_MESSAGE: Pattern = Pattern.compile(
            "^(##teamcity\\[compilationStarted|testSuiteStarted|testStarted)",
            Pattern.CASE_INSENSITIVE + Pattern.MULTILINE,
        )

        private const val SBT_PATCH_CLASS_NAME = "jetbrains.buildServer.sbtlogger.SbtTeamCityLogger"

        @JvmStatic
        fun prepareArgs(args: String): String = SbtCommandFileContentBuilder.prepareArgs(args)
    }
}

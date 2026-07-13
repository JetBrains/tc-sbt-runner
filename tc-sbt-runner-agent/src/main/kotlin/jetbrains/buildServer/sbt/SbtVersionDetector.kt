package jetbrains.buildServer.sbt

import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.util.FileUtil
import jetbrains.buildServer.util.PropertiesUtil
import jetbrains.buildServer.util.StringUtil
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.jar.JarFile
import java.util.regex.Pattern

/**
 * Inspired by https://github.com/JetBrains/intellij-scala/blob/idea172.x/src/org/jetbrains/sbt/SbtUtil.scala#L47-L57
 */
object SbtVersionDetector {
    private val PROPERTY = Pattern.compile("^\\s*(\\w+)\\s*:(.+)", Pattern.CASE_INSENSITIVE)
    private val SBT_VERSION = Pattern.compile("\\d+(\\.\\d+)+", Pattern.CASE_INSENSITIVE)
    private val SBT_VERSION_ARGS = Pattern.compile("^-Dsbt.version\\s*=\\s*(.+)", Pattern.CASE_INSENSITIVE)

    private const val SBT_VERSION_DISCOVERY_STEP_NAME = "Discovering SBT version"

    @JvmStatic
    fun discoverSbtVersion(
        workingDirectory: File,
        sbtLauncher: File,
        jvmArgs: List<String>,
        logger: BuildProgressLogger,
    ): SbtRunnerBuildService.SBTVersion {
        logger.activityStarted(SBT_VERSION_DISCOVERY_STEP_NAME, SbtRunnerBuildService.BUILD_ACTIVITY_TYPE)

        logger.message("Will read SBT version from project/build.properties file")
        var version = readFromProjectProperties(workingDirectory, logger)

        if (version == null) {
            logger.message("Will read SBT version from additional arguments")
            version = readFromJavaArguments(jvmArgs)
        }

        if (version == null) {
            logger.message("Will read SBT version from $sbtLauncher")
            val map = readSectionFromBootPropertiesOf(sbtLauncher, "app")
            map.forEach { (key, value) -> logger.message("$key : $value") }
            version = getSbtVersionFromProperties(map)
        }

        if (version == null) {
            logger.message("SBT version was not found")
        }
        val sbtVersion = getVersionFromString(version) ?: SbtRunnerBuildService.SBTVersion.SBT_0_13_x

        logger.message("Will use teamcity-sbt-logger for SBT version: $sbtVersion")
        logger.activityFinished(SBT_VERSION_DISCOVERY_STEP_NAME, SbtRunnerBuildService.BUILD_ACTIVITY_TYPE)

        return sbtVersion
    }

    @JvmStatic
    fun readFromJavaArguments(jvmArgs: List<String>): String? =
        jvmArgs
            .map { SBT_VERSION_ARGS.matcher(it.trim()) }
            .firstOrNull { it.find() }
            ?.group(1)
            ?.trim()

    private fun getVersionFromString(version: String?): SbtRunnerBuildService.SBTVersion? {
        if (StringUtil.isEmpty(version)) {
            return null
        }
        return if (version!!.trim().startsWith("1.")) {
            SbtRunnerBuildService.SBTVersion.SBT_1_x
        } else {
            SbtRunnerBuildService.SBTVersion.SBT_0_13_x
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readPropertiesFromStream(inputStream: InputStream, sectionName: String): Map<String, String> {
        val properties = HashMap<String, String>()
        @Suppress("DEPRECATION")
        val lines = IOUtils.readLines(inputStream)
        var take = false

        for (line in lines) {
            val str = line.trim()
            if (str.startsWith("[")) {
                take = str.contains("$sectionName]")
            } else if (take) {
                val matcher = PROPERTY.matcher(str)
                if (matcher.find()) {
                    properties[matcher.group(1)] = matcher.group(2).trim()
                }
            }
        }

        return properties
    }

    @JvmStatic
    fun getSbtVersionFromProperties(properties: Map<String, String>): String? {
        if (properties["name"] != "sbt") {
            return null
        }
        val version = properties["version"] ?: return null
        val matcher = SBT_VERSION.matcher(version)
        if (!matcher.find()) {
            return null
        }
        return matcher.group(0)
    }

    @JvmStatic
    fun readSectionFromBootPropertiesOf(launcherFile: File, sectionName: String): Map<String, String> {
        var jarFile: JarFile? = null
        var inputStream: InputStream? = null
        return try {
            jarFile = JarFile(launcherFile)
            val entry = jarFile.getEntry("sbt/sbt.boot.properties")
            inputStream = jarFile.getInputStream(entry)
            readPropertiesFromStream(inputStream, sectionName)
        } catch (e: Exception) {
            Collections.emptyMap()
        } finally {
            FileUtil.close(jarFile)
            FileUtil.close(inputStream)
        }
    }

    @JvmStatic
    fun readFromProjectProperties(workingDir: File, logger: BuildProgressLogger?): String? =
        try {
            val file = File(workingDir, "project${File.separator}build.properties")
            val properties = PropertiesUtil.loadProperties(file)
            properties.getProperty("sbt.version")
        } catch (e: FileNotFoundException) {
            logger?.message("project/build.properties not found")
            null
        } catch (e: Exception) {
            logger?.warning("An error occurred during SBT version check: ${e.message}")
            null
        }
}

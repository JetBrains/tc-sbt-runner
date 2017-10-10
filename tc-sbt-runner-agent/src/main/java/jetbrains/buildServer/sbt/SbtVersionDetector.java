package jetbrains.buildServer.sbt;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.PropertiesUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * inspired by https://github.com/JetBrains/intellij-scala/blob/idea172.x/src/org/jetbrains/sbt/SbtUtil.scala#L47-L57
 */
public class SbtVersionDetector {


    private static final Pattern PROPERTY = Pattern.compile("^\\s*(\\w+)\\s*:(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SBT_VERSION = Pattern.compile("\\d+(\\.\\d+)+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SBT_VERSION_ARGS = Pattern.compile("^-Dsbt.version\\s*=\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private static final String SBT_VERSION_DISCOVERY_STEP_NAME = "Discovering SBT version";

    public static SbtRunnerBuildService.SBTVersion discoverSbtVersion(@NotNull File workingDirectory,
                                                                      @NotNull File sbtLauncher,
                                                                      @NotNull List<String> jvmArgs,
                                                                      @NotNull BuildProgressLogger logger) {

        logger.activityStarted(SBT_VERSION_DISCOVERY_STEP_NAME, SbtRunnerBuildService.BUILD_ACTIVITY_TYPE);

        logger.message("Will read SBT version from project/build.properties file");

        String version = readFromProjectProperties(workingDirectory, logger);

        if (version == null) {
            logger.message("Will read SBT version from additional arguments");
            version = readFromJavaArguments(jvmArgs);
        }

        if (version == null) {
            logger.message("Will read SBT version from " + sbtLauncher);
            Map<String, String> map = readSectionFromBootPropertiesOf(sbtLauncher, "app");
            for (Map.Entry<String, String> entry : map.entrySet()) {
                logger.message(entry.getKey() + " : " + entry.getValue());
            }
            version = getSbtVersionFromProperties(map);
        }

        if (version == null) {
            logger.message("SBT version was not found");
        }
        SbtRunnerBuildService.SBTVersion sbtVersion = version != null ? getVersionFromString(version) : SbtRunnerBuildService.SBTVersion.SBT_0_13_x;

        logger.message("Will use teamcity-sbt-logger for SBT version: " + sbtVersion);

        logger.activityFinished(SBT_VERSION_DISCOVERY_STEP_NAME, SbtRunnerBuildService.BUILD_ACTIVITY_TYPE);

        return sbtVersion;
    }

    @Nullable
    public static String readFromJavaArguments(@NotNull List<String> jvmArgs) {
        for (String jvmArg : jvmArgs) {
            Matcher matcher = SBT_VERSION_ARGS.matcher(jvmArg.trim());
            if (matcher.find()){
                return matcher.group(1).trim();
            }
        }
        return null;
    }


    @Nullable
    private static SbtRunnerBuildService.SBTVersion getVersionFromString(@Nullable String version) {
        if (StringUtil.isEmpty(version)) {
            return null;
        }
        return version.trim().startsWith("1.") ? SbtRunnerBuildService.SBTVersion.SBT_1_x : SbtRunnerBuildService.SBTVersion.SBT_0_13_x;
    }

    @NotNull
    public static Map<String, String> readPropertiesFromStream(InputStream inputStream, @NotNull String sectionName) throws IOException {
        Map<String, String> properties = new HashMap<String, String>();
        List lines = IOUtils.readLines(inputStream);
        boolean take = false;
        for (Object line : lines) {
            String str = ((String) line).trim();
            if (str.startsWith("[")) {
                take = str.contains(sectionName + "]");
            } else if (take) {
                Matcher matcher = PROPERTY.matcher(str);
                boolean propertyLine = matcher.find();
                if (propertyLine) {
                    properties.put(matcher.group(1), matcher.group(2).trim());
                }

            }
        }
        return properties;
    }

    public static String getSbtVersionFromProperties(@NotNull Map<String, String> properties) {
        if (!"sbt".equals(properties.get("name"))) {
            return null;
        }
        String version = properties.get("version");
        Matcher matcher = SBT_VERSION.matcher(version);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(0);
    }

    @NotNull
    public static Map<String, String> readSectionFromBootPropertiesOf(@NotNull File launcherFile, @NotNull String sectionName) {
        JarFile jarFile = null;
        InputStream inputStream = null;
        try {
            jarFile = new JarFile(launcherFile);
            ZipEntry entry = jarFile.getEntry("sbt/sbt.boot.properties");
            inputStream = jarFile.getInputStream(entry);
            return readPropertiesFromStream(inputStream, sectionName);
        } catch (Exception e) {
            return Collections.emptyMap();
        } finally {
            FileUtil.close(jarFile);
            FileUtil.close(inputStream);
        }
    }


    public static String readFromProjectProperties(@NotNull File workingDir, @Nullable BuildProgressLogger logger) {
        try {
            File file = new File(workingDir.getAbsolutePath() + File.separator + "project" + File.separator + "build.properties");
            Properties properties = PropertiesUtil.loadProperties(file);
            return properties.getProperty("sbt.version");
        } catch (FileNotFoundException e) {
            if (logger != null) {
                logger.message("project/build.properties not found");
            }
            return null;
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("An error occurred during SBT version check: " + e.getMessage());
            }
            return null;
        }
    }

}

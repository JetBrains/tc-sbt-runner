package jetbrains.buildServer.sbt;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.runner.CommandLineArgumentsUtil;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SbtRunnerBuildService extends BuildServiceAdapter {

    private static final Logger LOG = Logger.getLogger(SbtRunnerBuildServiceFactory.class.getName());

    private static final String SBT_LAUNCHER_JAR_NAME = "sbt-launch.jar";

    private static final String SBT_PATCH_JAR_NAME = "sbt-teamcity-logger.jar";

    private static final String SBT_PATCH_FOLDER_NAME = "tc_plugin";

    private static final String SBT_DISTRIB = "sbt-distrib";

    private static final String SBT_AUTO_HOME_FOLDER = "agent-sbt";

    private static final String INLINE_COMMANDS_FORMATTER = "\"%s\" %s %s";

    private static final String INFILE_COMMANDS_FORMATTER = ";%s\n ;%s %s";

    private static final String RUN_INFILE_COMMANDS_FORMATTER = "< %s";


    private final static String[] SBT_JARS = new String[]{
            SBT_LAUNCHER_JAR_NAME,
            "classes"
    };
    public static final String BUILD_ACTIVITY_TYPE = "BUILD_ACTIVITY_TYPE";

    public static final Pattern LINES_TO_EXCLUDE = Pattern.compile("^\\[(error|warn)\\]",
            Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
    public static final Pattern KNOWN_SECTION_MESSAGE = Pattern.compile("^(##teamcity\\[compilationStarted|testSuiteStarted|testStarted)",
            Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
    private static final String SBT_PATCH_CLASS_NAME = "jetbrains.buildServer.sbtlogger.SbtTeamCityLogger";

    private final IvyCacheProvider myIvyCacheProvider;

    public SbtRunnerBuildService(IvyCacheProvider ivyCacheProvider) {
        myIvyCacheProvider = ivyCacheProvider;
    }

    @NotNull
    @Override
    public List<ProcessListener> getListeners() {
        return Collections.<ProcessListener>singletonList(new ProcessListenerAdapter() {
            boolean knownSectionStarts = false;

            @Override
            public void onStandardOutput(@NotNull String line) {
                if (!knownSectionStarts) {
                    //we need this otherwise we can hide important messages appeared before our own logger was started to print server messages
                    Matcher matcher = KNOWN_SECTION_MESSAGE.matcher(line);
                    if (matcher.find()) {
                        knownSectionStarts = true;
                    }
                }
                Matcher matcher = LINES_TO_EXCLUDE.matcher(line);
                if (matcher.find() && knownSectionStarts) {
                    //we don't want to duplicate lines
                    //sbt-tc-logger wraps WARN and ERROR messages
                    //we can exclude those messages from normal output
                    return;
                }
                getLogger().message(line);
            }

            public void onErrorOutput(@NotNull String line) {
                getLogger().warning(line);
            }

            @Override
            public void processFinished(int exitCode) {
                super.processFinished(exitCode);
            }
        });
    }


    @NotNull
    @Override
    public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {

        String mainClassName = isAutoInstallMode() ? installSbt() : getMainClassName();
        if (!isAutoInstallMode()) {
            copySbtTcLogger();
        }
        String javaHome = getJavaHome();
        String sbtHome = getSbtHome();
        getLogger().message("Java home set to: " + javaHome);
        getLogger().message("SBT home set to: " + sbtHome);

        JavaCommandLineBuilder cliBuilder = new JavaCommandLineBuilder();
        cliBuilder.setJavaHome(javaHome);
        cliBuilder.setBaseDir(getCheckoutDirectory().getAbsolutePath());

        cliBuilder.setSystemProperties(getVMProperties());
        Map<String, String> envVars = new HashMap<String, String>(getEnvironmentVariables());

        envVars.put(SbtRunnerConstants.SBT_HOME, sbtHome);
        envVars.put(JavaRunnerConstants.JAVA_HOME, javaHome);
        cliBuilder.setEnvVariables(envVars);

        cliBuilder.setJvmArgs(JavaRunnerUtil.extractJvmArgs(getRunnerParameters()));
        cliBuilder.setClassPath(getClasspath());
        cliBuilder.setMainClass(mainClassName);


        cliBuilder.setProgramArgs(getCommands());

        cliBuilder.setWorkingDir(getWorkingDirectory().getAbsolutePath());

        return buildCommandline(cliBuilder);
    }

    private String getApplyCommand() {
        String pathToPlugin = new File(getAutoInstallSbtFolder() + File.separator + SBT_PATCH_FOLDER_NAME + File.separator + SBT_PATCH_JAR_NAME).getAbsolutePath();
        return "apply -cp " + pathToPlugin + " " + SBT_PATCH_CLASS_NAME;
    }

    private String getCheckStatusCommand() {
        return "sbt-teamcity-logger";
    }

    private String getJavaHome() throws RunBuildException {
        String javaHome = JavaRunnerUtil.findJavaHome(getRunnerParameters().get(JavaRunnerConstants.TARGET_JDK_HOME),
                getBuildParameters().getAllParameters(),
                AgentRuntimeProperties.getCheckoutDir(getRunnerParameters()));
        if (javaHome == null) throw new RunBuildException("Unable to find Java home");
        javaHome = FileUtil.getCanonicalFile(new File(javaHome)).getPath();
        return javaHome;
    }

    @NotNull
    private String getAutoInstallSbtFolder() {
        return getAgentTempDirectory() + File.separator + SBT_AUTO_HOME_FOLDER;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void copyResources(String sourcePathInJar, String sourceName, File destinationDir) {
        destinationDir.mkdirs();
        File destination = new File(destinationDir, sourceName);
        FileUtil.copyResource(this.getClass(), sourcePathInJar + sourceName, destination);
        boolean done = destination.exists() && destination.isFile();
        if (done) {
            getLogger().message("Resource was copied to: " + destination);
        }
    }

    private String installSbt() {
        try {
            getLogger().activityStarted("SBT installation", "'Auto' mode was selected in SBT runner plugin settings", BUILD_ACTIVITY_TYPE);
            getLogger().message("SBT will be install to: " + getAutoInstallSbtFolder());
            copyResources("/" + SBT_DISTRIB + "/", SBT_LAUNCHER_JAR_NAME, new File(getAutoInstallSbtFolder() + File.separator + "bin"));
            copySbtTcLogger();
            getLogger().message("SBT home set to: " + getSbtHome());
            return getMainClassName();
        } catch (Exception e) {
            getLogger().internalError(ErrorData.PREPARATION_FAILURE_TYPE, "An error occurred during SBT installation", e);
            throw new IllegalStateException(e);
        } finally {
            getLogger().activityFinished("SBT installation", BUILD_ACTIVITY_TYPE);
        }

    }

    private void copySbtTcLogger() {
        try {
            getLogger().activityStarted("SBT TeamCity Logger installation", "'Custom' mode was selected in SBT runner plugin settings", BUILD_ACTIVITY_TYPE);
            String path = getAutoInstallSbtFolder() + File.separator + SBT_PATCH_FOLDER_NAME;
            getLogger().message("sbt-teamcity-logger.jar will be installed to: " + path);
            copyResources("/" + SBT_DISTRIB + "/", SBT_PATCH_JAR_NAME, new File(path));
        } catch (Exception e) {
            getLogger().internalError(ErrorData.PREPARATION_FAILURE_TYPE, "An error occurred during SBT installation", e);
            throw new IllegalStateException(e);
        } finally {
            getLogger().activityFinished("SBT TeamCity Logger installation", BUILD_ACTIVITY_TYPE);
        }

    }

    @NotNull
    private String getMainClassName() throws RunBuildException {
        try {
            File sbtLauncher = getSbtLauncher();
            getLogger().message("SBT main class name will be retrieved from: " + sbtLauncher);
            JarFile jf = new JarFile(sbtLauncher);
            return jf.getManifest().getMainAttributes().getValue("Main-Class");
        } catch (IOException e) {
            throw new RunBuildException("An error occurred during reading manifest in SBT launcher", e);
        }
    }

    @NotNull
    private File getSbtLauncher() {
        String sbtHome = getSbtHome();
        File jarDir = new File(sbtHome, "bin");
        return new File(jarDir, SBT_LAUNCHER_JAR_NAME);
    }


    @NotNull
    private ProgramCommandLine buildCommandline(@NotNull final JavaCommandLineBuilder cliBuilder) throws
            RunBuildException {
        try {
            return cliBuilder.build();
        } catch (CannotBuildCommandLineException e) {
            throw new RunBuildException(e.getMessage());
        }
    }

    @NotNull
    private Map<String, String> getVMProperties() throws RunBuildException {
        Map<String, String> sysProps = new HashMap<String, String>();

        String ivyCachePath = getIvyCachePath();
        sysProps.put("sbt.ivy.home", ivyCachePath);

        sysProps.putAll(JavaRunnerUtil.composeSystemProperties(getBuild(), getRunnerContext()));
        return sysProps;
    }

    @NotNull
    public String getClasspath() {
        String sbtHome = getSbtHome();
        File jarDir = new File(sbtHome, "bin");
        StringBuilder sb = new StringBuilder();
        for (String jar : SBT_JARS) {
            sb.append(new File(jarDir, jar).getAbsolutePath()).append(File.pathSeparator);
        }
        return sb.toString();
    }


    @NotNull
    private String getSbtHome() {
        String userSbtFolder = getRunnerParameters().get(SbtRunnerConstants.SBT_HOME_PARAM);
        if (isAutoInstallMode()) {
            return getAutoInstallSbtFolder();
        }
        return userSbtFolder;
    }

    private boolean isAutoInstallMode() {
        String sbtInstallationMode = getRunnerParameters().get(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM);
        return SbtRunnerConstants.AUTO_INSTALL_FLAG.equalsIgnoreCase(sbtInstallationMode);
    }

    @NotNull
    public String getIvyCachePath() {
        return myIvyCacheProvider.getCacheDir().getAbsolutePath();
    }

    @NotNull
    public List<String> getCommands() {
        String args = getRunnerParameters().get(SbtRunnerConstants.SBT_ARGS_PARAM).trim();
        if (StringUtil.isEmpty(args)) {
            getLogger().warning("No commands specified.");
            return Collections.emptyList();
        }

        if (args.startsWith(";")) {
            return getCommandsFromFile(args);
        }
        return getInlinedCommands(args);
    }

    @NotNull
    private List<String> getInlinedCommands(@NotNull String args) {
        return CommandLineArgumentsUtil.extractArguments(String.format(INLINE_COMMANDS_FORMATTER, getApplyCommand(), getCheckStatusCommand(), args));
    }

    @NotNull
    private List<String> getCommandsFromFile(@NotNull String args) {
        try {
            File file = FileUtil.createTempFile(getAgentTempDirectory(), "commands", ".file", true);
            String content = String.format(INFILE_COMMANDS_FORMATTER, getApplyCommand(), getCheckStatusCommand(), args);
            String name = file.getAbsolutePath();
            getLogger().activityStarted("Prepare SBT run", "Write commands to file.", BUILD_ACTIVITY_TYPE);
            getLogger().message("File name: " + name);
            getLogger().message("File content:\n" + content);
            getLogger().activityFinished("Prepare SBT run", BUILD_ACTIVITY_TYPE);
            FileUtil.writeFile(file, content, "UTF-8");
            List<String> commands = new ArrayList<String>();
            commands.add(String.format(RUN_INFILE_COMMANDS_FORMATTER, name));
            return commands;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}

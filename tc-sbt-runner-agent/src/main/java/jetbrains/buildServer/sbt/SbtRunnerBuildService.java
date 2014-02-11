package jetbrains.buildServer.sbt;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.runner.CommandLineArgumentsUtil;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;

public class SbtRunnerBuildService extends BuildServiceAdapter {

    private static final String SBT_LAUNCHER_JAR_NAME = "sbt-launch.jar";

    private static final String SBT_PATCH_JAR_NAME = "sbt-teamcity-logger.jar";

    private static final String SBT_DISTRIB = "sbt-distrib";

    private static final String SBT_AUTO_HOME_FOLDER = "agent-sbt";


    private final static String[] SBT_JARS = new String[]{
            SBT_LAUNCHER_JAR_NAME,
            "classes"
    };
    public static final String BUILD_ACTIVITY_TYPE = "BUILD_ACTIVITY_TYPE";

    private final IvyCacheProvider myIvyCacheProvider;

    public SbtRunnerBuildService(IvyCacheProvider ivyCacheProvider) {
        myIvyCacheProvider = ivyCacheProvider;
    }

    @NotNull
    @Override
    public List<ProcessListener> getListeners() {
        return Collections.<ProcessListener>singletonList(new ProcessListenerAdapter() {
            @Override
            public void onStandardOutput(@NotNull String line) {
                getLogger().message(line);
            }

            public void onErrorOutput(@NotNull String line) {
                getLogger().error(line);
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

        String mainClassName = isAutoInstallMode() ? installAndPatchSbt() : getMainClassName();

        JavaCommandLineBuilder cliBuilder = new JavaCommandLineBuilder();
        String javaHome = getRunnerParameters().get(JavaRunnerConstants.TARGET_JDK_HOME);
        cliBuilder.setJavaHome(javaHome);
        cliBuilder.setBaseDir(getCheckoutDirectory().getAbsolutePath());

        cliBuilder.setSystemProperties(getVMProperties());
        Map<String, String> envVars = new HashMap<String, String>(getEnvironmentVariables());
        envVars.put("SBT_HOME", getSbtHome());
        envVars.put("JAVA_HOME", javaHome);
        cliBuilder.setEnvVariables(envVars);

        cliBuilder.setJvmArgs(JavaRunnerUtil.extractJvmArgs(getRunnerParameters()));
        cliBuilder.setClassPath(getClasspath());
        cliBuilder.setMainClass(mainClassName);

        List<String> programParameters = getProgramParameters();
        if (isAutoInstallMode()) {
            programParameters = addDirectoriesParameters(programParameters);
        }
        cliBuilder.setProgramArgs(programParameters);

        cliBuilder.setWorkingDir(getWorkingDirectory().getAbsolutePath());

        return buildCommandline(cliBuilder);
    }

    private List<String> addDirectoriesParameters(@NotNull List<String> programParameters) {
        List<String> params = new ArrayList<String>();
        params.add(String.format("-Dsbt.global.base=%s", getAutoInstallSbtFolder()));
        params.addAll(programParameters);
        //-Dsbt.log.format=false
        return params;
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

    private String installAndPatchSbt() {
        try {
            getLogger().activityStarted("SBT installation", "'Auto' mode was selected in SBT runner plugin settings", BUILD_ACTIVITY_TYPE);
            getLogger().message("SBT will be install to: " + getAutoInstallSbtFolder());
            copyResources("/" + SBT_DISTRIB + "/", SBT_LAUNCHER_JAR_NAME, new File(getAutoInstallSbtFolder() + File.separator + "bin"));
            copyResources("/" + SBT_DISTRIB + "/", SBT_PATCH_JAR_NAME, new File(getAutoInstallSbtFolder() + File.separator + "plugins" + File.separator + "lib"));
            getLogger().message("SBT home set to: " + getSbtHome());
            return getMainClassName();
        } catch (Exception e) {
            getLogger().internalError(ErrorData.PREPARATION_FAILURE_TYPE,"An error occurred during SBT installation",e);
            throw new IllegalStateException(e);
        } finally {
            getLogger().activityFinished("SBT installation", BUILD_ACTIVITY_TYPE);
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
    public List<String> getProgramParameters() {
        String args = getRunnerParameters().get(SbtRunnerConstants.SBT_ARGS_PARAM);
        if (StringUtil.isEmptyOrSpaces(args)) {
            return Collections.emptyList();
        }
        return CommandLineArgumentsUtil.extractArguments(args);
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
}

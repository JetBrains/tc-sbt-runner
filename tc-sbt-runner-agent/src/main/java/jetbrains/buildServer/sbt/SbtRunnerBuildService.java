package jetbrains.buildServer.sbt;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.runner.CommandLineArgumentsUtil;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;

public class SbtRunnerBuildService extends BuildServiceAdapter {

    private static final String SBT_LAUNCHER_JAR_NAME = "sbt-launch.jar";

    private static final String SBT_PATCH_JAR_NAME = "sbt-teamcity-logger.jar";

    private static final String AUTO_INSTALL_FOLDER = "tc-sbt";

    private static final String AUTO_INSTALL_GLOBALS_FOLDER = "tc-sbt-globals";

    private static final String AUTO_INSTALL_FLAG = "auto";


    private final static String[] SBT_JARS = new String[]{
            SBT_LAUNCHER_JAR_NAME,
            "classes"
    };
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
        boolean autoInstall = AUTO_INSTALL_FLAG.equalsIgnoreCase(getRunnerParameters().get(SbtRunnerConstants.SBT_HOME_PARAM));

        if (autoInstall) {
            getLogger().message("SBT will be installed automatically");
            installAndPatchSbt();
            getLogger().message("SBT successfully installed");
        }

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
        cliBuilder.setMainClass(getMainClassName());

        List<String> programParameters = getProgramParameters();
        if (autoInstall) {
            programParameters = addDirectoriesParameters(programParameters);
        }
        cliBuilder.setProgramArgs(programParameters);

        cliBuilder.setWorkingDir(getWorkingDirectory().getAbsolutePath());

        return buildCommandline(cliBuilder);
    }

    private List<String> addDirectoriesParameters(@NotNull List<String> programParameters) {
        List<String> params = new ArrayList<String>();
        params.add(String.format("-Dsbt.global.base=%s", getAutoInstallSbtFolder()));
        params.add(String.format("-Dsbt.global.plugins=%s", getAutoInstallSbtGlobalsFolder()));
        params.addAll(programParameters);
        //-Dsbt.log.format=false
        return params;
    }


    @NotNull
    private String getAutoInstallSbtFolder() {
        return getWorkingDirectory() + File.separator + AUTO_INSTALL_FOLDER;
    }

    @NotNull
    private String getAutoInstallSbtGlobalsFolder() {
        return getWorkingDirectory() + File.separator + AUTO_INSTALL_GLOBALS_FOLDER;
    }

    private void installAndPatchSbt() {
        copyFiles(AUTO_INSTALL_FOLDER + File.separator + SBT_LAUNCHER_JAR_NAME, getAutoInstallSbtFolder() + File.separator + "bin" + File.separator + SBT_LAUNCHER_JAR_NAME);
        copyFiles(AUTO_INSTALL_FOLDER + File.separator + SBT_PATCH_JAR_NAME, getAutoInstallSbtGlobalsFolder() + File.separator
                + "lib" + File.separator + SBT_PATCH_JAR_NAME);
    }


    private void copyFiles(@NotNull String name, @NotNull String destination) {
        try {
            URL inputUrl = getClass().getClassLoader().getResource(name);
            getLogger().message(String.format("Copying from %s to %s", name, destination));
            FileUtils.copyURLToFile(inputUrl, new File(destination));
        } catch (IOException e) {
            getLogger().error(e.getMessage());
        }
    }

    @NotNull
    private String getMainClassName() throws RunBuildException {
        try {
            File sbtLauncher = getSbtLauncher();
            getLogger().message("Retrieve SBT main class name from: " + sbtLauncher);
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
    private ProgramCommandLine buildCommandline(@NotNull final JavaCommandLineBuilder cliBuilder) throws RunBuildException {
        try {
            return cliBuilder.build();
        } catch (CannotBuildCommandLineException e) {
            throw new RunBuildException(e.getMessage());
        }
    }

    @NotNull
    private Map<String, String> getVMProperties() throws RunBuildException {
        String sbtVersion = getRunnerParameters().get(SbtRunnerConstants.SBT_VERSION_PARAM);

        Map<String, String> sysProps = new HashMap<String, String>();
        if (!StringUtil.isEmptyOrSpaces(sbtVersion)) {
            sysProps.put("sbt.version", sbtVersion);
        }

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
        boolean autoInstall = AUTO_INSTALL_FLAG.equalsIgnoreCase(userSbtFolder);
        if (autoInstall) {
            return getAutoInstallSbtFolder();
        }
        return userSbtFolder;
    }

    @NotNull
    public String getIvyCachePath() {
        return myIvyCacheProvider.getCacheDir().getAbsolutePath();
    }
}

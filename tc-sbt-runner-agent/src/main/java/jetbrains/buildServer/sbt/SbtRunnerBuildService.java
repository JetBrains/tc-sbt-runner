package jetbrains.buildServer.sbt;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.runner.CommandLineArgumentsUtil;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class SbtRunnerBuildService extends BuildServiceAdapter {

    private static final Logger LOG = Logger.getLogger(SbtRunnerBuildService.class.getName());

    private final static String[] SBT_JARS = new String[]{
            "jansi.jar",
            "sbt-launch.jar",
            "classes"
    };
    private final IvyCacheProvider myIvyCacheProvider;

    public SbtRunnerBuildService(IvyCacheProvider ivyCacheProvider) {
        LOG.info("SbtRunnerBuildService.constructor");
        myIvyCacheProvider = ivyCacheProvider;
    }

    @NotNull
    @Override
    public List<ProcessListener> getListeners() {
        return Collections.<ProcessListener>singletonList(new ProcessListenerAdapter() {
            private final List<String> myLastErrors = new ArrayList<String>();
            public String myCompilingBlock;

            @Override
            public void onStandardOutput(@NotNull String line) {
                //String trimmed = line.trim();

                /*boolean newCompileBlock = trimmed.startsWith("[info] Compiling ");
                if (newCompileBlock) {
                    openCompileBlock(line);
                } else {
                    if (myCompilingBlock != null && !(trimmed.startsWith("[error] ") || trimmed.startsWith("[warn] "))) {
                        closeCompileBlock();
                    }

                    if (myCompilingBlock != null) {
                        for (Pattern p : compileFinished) {
                            if (p.matcher(trimmed).find()) {
                                closeCompileBlock();
                                break;
                            }
                        }
                    }
                }

                for (Pattern p : errorPatterns) {
                    if (p.matcher(trimmed).find()) {
                        myLastErrors.add(line);
                        return;
                    }
                }

                flushErrors();

                if (trimmed.startsWith("[warn] ")) {
                    logWarning(line);
                    return;
                }

                if (actionPattern.matcher(trimmed).find()) {
                    logProgress(line);
                } else {
                    logMessage(line);
                }*/
                logMessage(line);
            }

            private void openCompileBlock(@NotNull String line) {
                closeCompileBlock();

                String[] splitted = line.split(" sources? to ");
                if (splitted.length > 1) {
                    myCompilingBlock = splitted[0];
                    if (line.contains("sources to")) {
                        myCompilingBlock += " sources";
                    } else {
                        myCompilingBlock += " source";
                    }
                } else {
                    myCompilingBlock = line;
                }

                myCompilingBlock = removePrefix(myCompilingBlock);

                //getLogger().logMessage(DefaultMessagesInfo.createCompilationBlockStart(myCompilingBlock));
            }

            private void closeCompileBlock() {
                if (myCompilingBlock != null) {
                    if (myLastErrors.isEmpty()) {
                        doCloseCompileBlock();
                    } else {
                        flushErrors();
                    }
                }
                myCompilingBlock = null;
            }

            private void doCloseCompileBlock() {
                //getLogger().logMessage(DefaultMessagesInfo.createCompilationBlockEnd(myCompilingBlock));
            }

            @Override
            public void onErrorOutput(@NotNull String line) {
                logWarning(line);
            }

            @Override
            public void processFinished(int exitCode) {
                flushErrors();
                closeCompileBlock();
                super.processFinished(exitCode);
            }

            private void logProgress(@NotNull final String message) {
                getLogger().progressMessage(removePrefix(message));
            }

            private void logMessage(@NotNull final String message) {

               getLogger().message(message);
            }

            private void logWarning(@NotNull final String message) {
                //getLogger().warning(removePrefix(message));
            }

            private void logError(@NotNull final String message) {
                //getLogger().error(removePrefix(message));
            }

            private String removePrefix(@NotNull String message) {
                if (message.startsWith("[warn] ") || message.startsWith("[info] ")) {
                    return message.substring("[warn] ".length());
                }
                if (message.startsWith("[error] ")) {
                    return message.substring("[error] ".length());
                }
                return message;
            }

            private boolean flushErrors() {
                if (myLastErrors.isEmpty()) return false;

                for (String err : myLastErrors) {
                    logError(err);
                }

                if (myCompilingBlock != null) {
                    doCloseCompileBlock();

                    String id = myLastErrors.get(0);
                    if (id.length() > 60) {
                        id = id.substring(0, 60); // can't be longer than 60 chars
                    }

                    /*getLogger().logBuildProblem(
                            BuildProblemData.createBuildProblem(id,
                                    BuildProblemData.TC_COMPILATION_ERROR_TYPE,
                                    "Sbt reported compilation errors found"));    */
                }

                myLastErrors.clear();
                return true;
            }

        });
    }


    @NotNull
    @Override
    public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
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
        cliBuilder.setMainClass("xsbt.boot.Boot");
        cliBuilder.setProgramArgs(getProgramParameters());
        cliBuilder.setWorkingDir(getWorkingDirectory().getAbsolutePath());

        return buildCommandline(cliBuilder);
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
        return getRunnerParameters().get(SbtRunnerConstants.SBT_HOME_PARAM);
    }

    @NotNull
    public String getIvyCachePath() {
        return myIvyCacheProvider.getCacheDir().getAbsolutePath();
    }
}

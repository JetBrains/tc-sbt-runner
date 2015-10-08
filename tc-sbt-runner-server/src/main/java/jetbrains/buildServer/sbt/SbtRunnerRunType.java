package jetbrains.buildServer.sbt;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SbtRunnerRunType extends RunType {


    public static final String SBT_JVM_ARGS = TeamCityProperties.getProperty(
            SbtRunnerConstants.TEAMCITY_SBT_DEFAULT_JVM_ARGS_PROPERTY,
            SbtRunnerConstants.DEFAULT_VALUE);

    private PluginDescriptor myPluginDescriptor;

    public SbtRunnerRunType(@NotNull final RunTypeRegistry runTypeRegistry,
                            @NotNull final PluginDescriptor pluginDescriptor) {
        myPluginDescriptor = pluginDescriptor;
        runTypeRegistry.registerRunType(this);
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new PropertiesProcessor() {
            public Collection<InvalidProperty> process(Map<String, String> props) {
                List<InvalidProperty> errors = new ArrayList<InvalidProperty>();
                if (!SbtRunnerConstants.AUTO_INSTALL_FLAG.equalsIgnoreCase(props.get(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM)) && StringUtil.isEmptyOrSpaces(props.get(SbtRunnerConstants.SBT_HOME_PARAM))) {
                    errors.add(new InvalidProperty(SbtRunnerConstants.SBT_HOME_PARAM, "Sbt home path must be specified"));
                }
                return errors;
            }
        };
    }

    @NotNull
    @Override
    public String getDescription() {
        return SbtRunnerConstants.RUNNER_DESCRIPTION;
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return myPluginDescriptor.getPluginResourcesPath("editSbtRunParams.jsp");
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return myPluginDescriptor.getPluginResourcesPath("viewSbtRunParams.jsp");
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<String, String>() {{
            put(SbtRunnerConstants.SBT_ARGS_PARAM, SbtRunnerConstants.DEFAULT_SBT_COMMANDS);
            put(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, SbtRunnerConstants.AUTO_INSTALL_FLAG);
            put("jvmArgs", SBT_JVM_ARGS);
        }};
    }


    @NotNull
    @Override
    public String getType() {
        return SbtRunnerConstants.RUNNER_TYPE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return SbtRunnerConstants.RUNNER_DISPLAY_NAME;
    }


    @NotNull
    @Override
    public String describeParameters(@NotNull final Map<String, String> parameters) {
        String args = parameters.get(SbtRunnerConstants.SBT_ARGS_PARAM);
        if (!StringUtil.isEmptyOrSpaces(args)) {
            return args;
        }
        return "";
    }

    @NotNull
    @Override
    public List<Requirement> getRunnerSpecificRequirements(@NotNull final Map<String, String> runParameters) {
        return Collections.emptyList();
    }

}

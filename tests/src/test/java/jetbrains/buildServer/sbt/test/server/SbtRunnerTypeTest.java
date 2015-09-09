package jetbrains.buildServer.sbt.test.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.sbt.SbtRunnerConstants;
import jetbrains.buildServer.sbt.SbtRunnerRunType;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

@SuppressWarnings("ConstantConditions")
public class SbtRunnerTypeTest extends BaseTestCase {

    protected Mockery m;
    protected PluginDescriptor descriptor;
    protected RunTypeRegistry typeRegistry;
    protected SbtRunnerRunType runType;

    @BeforeMethod
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m = new Mockery();
        descriptor = m.mock(PluginDescriptor.class);
        typeRegistry = m.mock(RunTypeRegistry.class);
        m.checking(new Expectations() {{
            ignoring(typeRegistry);
        }});
        runType = new SbtRunnerRunType(typeRegistry, descriptor);
    }


    protected void doTestValidator(@NotNull Map<String, String> parameters, @NotNull Set<String> errors) {
        Collection<InvalidProperty> errs = runType.getRunnerPropertiesProcessor().process(parameters);

        Set<String> actualErrors = new TreeSet<String>();
        for (InvalidProperty err : errs) {
            actualErrors.add(err.getPropertyName());
        }

        Assert.assertEquals(new TreeSet<String>(errors), actualErrors);
    }

    @NotNull
    protected Map<String, String> params(String... kvs) {
        Map<String, String> m = new TreeMap<String, String>();
        for (int i = 0; i < kvs.length; i += 2) {
            m.put(kvs[i], kvs[i + 1]);
        }
        return m;
    }

    @NotNull
    protected Set<String> s(String... kvs) {
        Set<String> m = new TreeSet<String>();
        Collections.addAll(m, kvs);
        return m;
    }

    @Test
    public void test_no_parameters() {
        doTestValidator(params(), s("sbt.home"));
    }

    @Test
    public void test_defaults() {
        doTestValidator(runType.getDefaultRunnerProperties(), s());
    }

    @Test
    public void test_custom_mode_no_home() {
        doTestValidator(params(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, "custom"), s("sbt.home"));
    }

    @Test
    public void test_custom_mode() {
        doTestValidator(params(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, "custom", SbtRunnerConstants.SBT_HOME_PARAM, "dir"), s());
    }

    @Test
    public void test_auto_mode() {
        doTestValidator(params(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, "auto"), s());
    }

}
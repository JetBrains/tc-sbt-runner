package jetbrains.buildServer.sbt.test.server

import jetbrains.buildServer.BaseTestCase
import jetbrains.buildServer.sbt.SbtRunnerConstants
import jetbrains.buildServer.sbt.SbtRunnerRunType
import jetbrains.buildServer.serverSide.RunTypeRegistry
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.jmock.Expectations
import org.jmock.Mockery
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.util.TreeMap
import java.util.TreeSet

class SbtRunnerTypeTest : BaseTestCase() {

    private lateinit var mockery: Mockery
    private lateinit var descriptor: PluginDescriptor
    private lateinit var typeRegistry: RunTypeRegistry
    private lateinit var runType: SbtRunnerRunType

    @BeforeMethod
    public override fun setUp() {
        super.setUp()
        mockery = Mockery()
        descriptor = mockery.mock(PluginDescriptor::class.java)
        typeRegistry = mockery.mock(RunTypeRegistry::class.java)
        mockery.checking(
            object : Expectations() {
                init {
                    ignoring(typeRegistry)
                }
            },
        )
        runType = SbtRunnerRunType(typeRegistry, descriptor)
    }

    private fun doTestValidator(parameters: MutableMap<String, String>, errors: Set<String>) {
        val errs = runType.runnerPropertiesProcessor.process(parameters)
        val actualErrors = errs.mapTo(TreeSet()) { it.propertyName }

        Assert.assertEquals(TreeSet(errors), actualErrors)
    }

    private fun params(vararg kvs: String): MutableMap<String, String> {
        val result = TreeMap<String, String>()
        for (i in kvs.indices step 2) {
            result[kvs[i]] = kvs[i + 1]
        }
        return result
    }

    private fun s(vararg kvs: String): Set<String> = TreeSet<String>().apply {
        addAll(kvs)
    }

    @Test
    fun test_no_parameters() {
        doTestValidator(params(), s("sbt.home"))
    }

    @Test
    fun test_defaults() {
        doTestValidator(runType.defaultRunnerProperties.toMutableMap(), s())
    }

    @Test
    fun test_custom_mode_no_home() {
        doTestValidator(params(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, "custom"), s("sbt.home"))
    }

    @Test
    fun test_custom_mode() {
        doTestValidator(
            params(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, "custom", SbtRunnerConstants.SBT_HOME_PARAM, "dir"),
            s(),
        )
    }

    @Test
    fun test_auto_mode() {
        doTestValidator(params(SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM, "auto"), s())
    }

    @Test
    fun test_LogLevel() {
    }

    @Test
    fun test_remove_sbt_home_in_auto_mode() {
        val params = params(
            SbtRunnerConstants.SBT_INSTALLATION_MODE_PARAM,
            "auto",
            SbtRunnerConstants.SBT_HOME_PARAM,
            "home",
        )
        runType.runnerPropertiesProcessor.process(params)
        Assert.assertFalse(params.containsKey(SbtRunnerConstants.SBT_HOME_PARAM))
    }
}

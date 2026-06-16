package jetbrains.buildServer.sbt.test.agent

import jetbrains.buildServer.sbt.SbtVersionDetector
import jetbrains.buildServer.util.FileUtil
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class SbtVersionDetectorTest {

    @Test
    fun readVersionFromSbtBootProperties() {
        var stream: InputStream? = null
        try {
            stream = FileInputStream(File("testdata/sbtVersionDiscovery/fromApp/sbt.boot.properties"))
            val map = SbtVersionDetector.readPropertiesFromStream(stream, "app")
            Assert.assertEquals(map.size, 5)
            Assert.assertEquals(map["version"], "\${sbt.version-read(sbt.version)[1.3.8]}")
            val version = SbtVersionDetector.getSbtVersionFromProperties(map)
            Assert.assertEquals(version, "1.3.8")
        } catch (e: Exception) {
            Assert.fail("Failed to read SBT boot properties", e)
        } finally {
            FileUtil.close(stream)
        }
    }

    @Test
    fun readVersionFromLauncher() {
        val file = File("testdata/sbtVersionDiscovery/fromLauncher/sbt-launch.jar")
        val map = SbtVersionDetector.readSectionFromBootPropertiesOf(file, "app")
        val version = SbtVersionDetector.getSbtVersionFromProperties(map)
        Assert.assertEquals(version, "1.10.10")
    }

    @Test
    fun foundInProjectProperties() {
        val file = File("testdata/sbtVersionDiscovery/fromProjectProperties")
        val version = SbtVersionDetector.readFromProjectProperties(file, null)
        Assert.assertEquals(version, "0.13.16")
    }

    @Test
    fun notFoundInProjectProperties() {
        val file = File("testdata/sbtVersionDiscovery/fromApp")
        val version = SbtVersionDetector.readFromProjectProperties(file, null)
        Assert.assertNull(version)
    }

    @Test
    fun foundInJavaArg() {
        val version = SbtVersionDetector.readFromJavaArguments(listOf("-Dsbt.version = 0.13.16 "))
        Assert.assertEquals(version, "0.13.16")
    }

    @Test
    fun notFoundInJavaArg() {
        val version = SbtVersionDetector.readFromJavaArguments(listOf("-XX:ReservedCodeCacheSize=128m"))
        Assert.assertNull(version)
    }
}

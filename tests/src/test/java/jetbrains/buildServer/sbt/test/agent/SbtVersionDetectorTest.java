package jetbrains.buildServer.sbt.test.agent;

import jetbrains.buildServer.sbt.SbtVersionDetector;
import jetbrains.buildServer.util.FileUtil;
import org.junit.Test;
import org.testng.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.fail;

public class SbtVersionDetectorTest {

    @Test
    public void readVersionFromSbtBootProperties() {
        InputStream stream = null;
        try {
            File file = new File("testdata/sbtVersionDiscovery/fromApp/sbt.boot.properties");
            stream = new FileInputStream(file);
            Map<String, String> map = SbtVersionDetector.readPropertiesFromStream(stream, "app");
            Assert.assertEquals(map.size(), 5);
            Assert.assertEquals(map.get("version"), "${sbt.version-read(sbt.version)[1.3.8]}");
            String version = SbtVersionDetector.getSbtVersionFromProperties(map);
            Assert.assertEquals(version, "1.3.8");
        } catch (Exception e) {
            fail();
        } finally {
            FileUtil.close(stream);
        }

    }


    @Test
    public void readVersionFromLauncher() {
        File file = new File("testdata/sbtVersionDiscovery/fromLauncher/sbt-launch.jar");
        Map<String, String> map = SbtVersionDetector.readSectionFromBootPropertiesOf(file, "app");
        String version = SbtVersionDetector.getSbtVersionFromProperties(map);
        Assert.assertEquals(version, "1.10.10");
    }


    @Test
    public void foundInProjectProperties() {
        File file = new File("testdata/sbtVersionDiscovery/fromProjectProperties");
        String version = SbtVersionDetector.readFromProjectProperties(file, null);
        Assert.assertEquals(version, "0.13.16");
    }

    @Test
    public void notFoundInProjectProperties() {
        File file = new File("testdata/sbtVersionDiscovery/fromApp");
        String version = SbtVersionDetector.readFromProjectProperties(file, null);
        Assert.assertNull(version);
    }


    @Test
    public void foundInJavaArg() {
        String version = SbtVersionDetector.readFromJavaArguments(Collections.singletonList("-Dsbt.version = 0.13.16 "));
        Assert.assertEquals(version, "0.13.16");
    }

    @Test
    public void notFoundInJavaArg() {
        String version = SbtVersionDetector.readFromJavaArguments(Collections.singletonList("-XX:ReservedCodeCacheSize=128m"));
        Assert.assertNull(version);
    }
}

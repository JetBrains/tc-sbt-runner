package jetbrains.buildServer.sbt;

import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.DirectoryCleanersProvider;
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext;
import jetbrains.buildServer.agent.DirectoryCleanersRegistry;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Date;


public class IvyCacheProvider implements DirectoryCleanersProvider {

    private static final Logger LOG = Logger.getLogger(IvyCacheProvider.class.getName());

    private final File myCacheDir;

    public IvyCacheProvider(@NotNull BuildAgentConfiguration agentConfiguration) {
        LOG.debug("IvyCacheProvider.constructor");
        myCacheDir = agentConfiguration.getCacheDirectory("sbt_ivy");
    }

    @NotNull
    public File getCacheDir() {
        return myCacheDir;
    }

    @NotNull
    public String getCleanerName() {
        return "Sbt Ivy cache cleaner";
    }

    public void registerDirectoryCleaners(@NotNull DirectoryCleanersProviderContext directoryCleanersProviderContext,
                                          @NotNull DirectoryCleanersRegistry registry) {
        File curCacheDir = new File(getCacheDir(), "cache");
        if (curCacheDir.isDirectory()) {
            File[] subDirs = curCacheDir.listFiles();
            if (subDirs != null) {
                for (File dir : subDirs) {
                    if (!dir.isDirectory()) continue;
                    registry.addCleaner(dir, new Date(dir.lastModified()));
                }
            }
        }

    }
}

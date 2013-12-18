package jetbrains.buildServer.sbt;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.DirectoryCleanersProvider;
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext;
import jetbrains.buildServer.agent.DirectoryCleanersRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Date;
import java.util.logging.Logger;

public class IvyCacheProvider {

    private static final Logger LOG = Logger.getLogger(IvyCacheProvider.class.getName());

    private final File myCacheDir;

    public IvyCacheProvider(@NotNull BuildAgentConfiguration agentConfiguration, @NotNull ExtensionHolder extensionHolder) {
        LOG.info("IvyCacheProvider.constructor");
        myCacheDir = agentConfiguration.getCacheDirectory("sbt_ivy");
        extensionHolder.registerExtension(DirectoryCleanersProvider.class, getClass().getName(), new DirectoryCleanersProvider() {
            @NotNull
            public String getCleanerName() {
                return "Sbt Ivy cache cleaner";
            }

            public void registerDirectoryCleaners(@NotNull final DirectoryCleanersProviderContext context,
                                                  @NotNull final DirectoryCleanersRegistry registry) {
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
        });
    }

    @NotNull
    public File getCacheDir() {
        return myCacheDir;
    }
}

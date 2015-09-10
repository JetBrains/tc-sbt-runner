package jetbrains.buildServer.sbt;

import jetbrains.buildServer.serverSide.discovery.BreadthFirstRunnerDiscoveryExtension;
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject;
import jetbrains.buildServer.util.browser.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SbtRunnerDiscoveryExtension extends BreadthFirstRunnerDiscoveryExtension {

    private SbtRunnerRunType sbtRunnerRunType;

    public SbtRunnerDiscoveryExtension(SbtRunnerRunType sbtRunnerRunType) {
        this.sbtRunnerRunType = sbtRunnerRunType;
    }

    public SbtRunnerDiscoveryExtension(int depthLimit, SbtRunnerRunType sbtRunnerRunType) {
        super(depthLimit);
        this.sbtRunnerRunType = sbtRunnerRunType;
    }

    @NotNull
    @Override
    protected List<DiscoveredObject> discoverRunnersInDirectory(@NotNull final Element dir, @NotNull final List<Element> filesAndDirs) {
        List<DiscoveredObject> res = new ArrayList<DiscoveredObject>();
        for (Element file : filesAndDirs) {
            if (file.isLeaf() && file.getName().endsWith(".sbt")) {
                res.add(new DiscoveredObject(SbtRunnerConstants.RUNNER_TYPE, sbtRunnerRunType.getDefaultRunnerProperties()));
            }
        }
        return res;
    }


}

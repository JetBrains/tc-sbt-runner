package jetbrains.buildServer.sbt

import jetbrains.buildServer.serverSide.discovery.BreadthFirstRunnerDiscoveryExtension
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject
import jetbrains.buildServer.util.browser.Element

/**
 * TeamCity loads this class by FQCN from the Spring plugin descriptor, so keep these constructors
 * stable while allowing the implementation itself to use Kotlin collection idioms.
 */
class SbtRunnerDiscoveryExtension : BreadthFirstRunnerDiscoveryExtension {
    private val sbtRunnerRunType: SbtRunnerRunType

    constructor(sbtRunnerRunType: SbtRunnerRunType) : super() {
        this.sbtRunnerRunType = sbtRunnerRunType
    }

    constructor(depthLimit: Int, sbtRunnerRunType: SbtRunnerRunType) : super(depthLimit) {
        this.sbtRunnerRunType = sbtRunnerRunType
    }

    override fun discoverRunnersInDirectory(dir: Element, filesAndDirs: List<Element>): List<DiscoveredObject> =
        filesAndDirs
            .filter { it.isLeaf && it.name.endsWith(".sbt") }
            .map {
                DiscoveredObject(
                    SbtRunnerConstants.RUNNER_TYPE,
                    sbtRunnerRunType.defaultRunnerProperties,
                )
            }
}

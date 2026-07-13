package jetbrains.buildServer.sbt

import jetbrains.buildServer.agent.BuildAgentConfiguration
import jetbrains.buildServer.agent.DirectoryCleanersProvider
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import org.apache.log4j.Logger
import java.io.File
import java.util.Date

/**
 * TeamCity loads this class by FQCN from the agent Spring descriptor, so keep the constructor
 * stable while allowing the implementation itself to use Kotlin idioms.
 */
class IvyCacheProvider(agentConfiguration: BuildAgentConfiguration) : DirectoryCleanersProvider {

    val cacheDir: File = agentConfiguration.getCacheDirectory("sbt_ivy")

    init {
        LOG.debug("IvyCacheProvider.constructor")
    }

    override fun getCleanerName(): String = "Sbt Ivy cache cleaner"

    override fun registerDirectoryCleaners(
        directoryCleanersProviderContext: DirectoryCleanersProviderContext,
        registry: DirectoryCleanersRegistry,
    ) {
        File(cacheDir, "cache")
            .listFiles()
            ?.asSequence()
            ?.filter(File::isDirectory)
            ?.forEach { registry.addCleaner(it, Date(it.lastModified())) }
    }

    companion object {
        private val LOG = Logger.getLogger(IvyCacheProvider::class.java.name)
    }
}

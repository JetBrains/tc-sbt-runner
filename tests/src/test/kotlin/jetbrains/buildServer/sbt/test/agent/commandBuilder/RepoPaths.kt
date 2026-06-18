package jetbrains.buildServer.sbt.test.agent.commandBuilder

import java.io.File

/**
 * Contains paths to common dirs in the current repository that are needed in some tests
 */
internal object RepoPaths {
    /** Repository root, resolved from either the root itself or the tests module working directory. */
    val root: File by lazy {
        val workingDirectory = File("").absoluteFile

        // Depending on form where the tests are launched (from the root of form the subproject)
        // current working dir can point to the subproject, so we might need to get a parent dir
        val rootDirectory = if (File(workingDirectory, "tc-sbt-runner-agent").isDirectory)
            workingDirectory
        else
            workingDirectory.parentFile

        rootDirectory.canonicalFile
    }

    val testsModule: File = File(root, "tests")

    val sbtLauncher: File = File(root, "tc-sbt-runner-agent/src/main/resources/sbt-distrib/sbt-launch.jar")
}

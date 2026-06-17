package jetbrains.buildServer.sbt

object SbtCommandFileContentBuilder {

    @JvmStatic
    fun build(sbtCommands: String, prologueCommands: List<String> = emptyList()): String {
        val preparedArgs = prepareArgs(sbtCommands)
        val prologCommandsText = if (prologueCommands.isEmpty())
            ""
        else
            prologueCommands.joinToString(separator = " ") { ";$it" } + " "
        return prologCommandsText + preparedArgs
    }

    @JvmStatic
    fun prepareArgs(sbtCommands: String): String {
        if (sbtCommands.startsWith(";")) {
            return sbtCommands
        }
        return sbtCommands
            .split(" ")
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString(separator = "") { "\n; $it" }
    }
}

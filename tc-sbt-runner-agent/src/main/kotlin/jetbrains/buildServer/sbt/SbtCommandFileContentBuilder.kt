package jetbrains.buildServer.sbt

import jetbrains.buildServer.util.StringUtil

object SbtCommandFileContentBuilder {

    /**
     * Builds the text written to sbt's redirected command file.
     *
     * Inferred historical intent: print the user commands from a new line for better visibility of the
     * commands in the TeamCity build output.
     *
     * Example:
     * ```
     * ; apply -cp "/agent temp/tc_plugin/sbt-teamcity-logger.jar" jetbrains.buildServer.sbtlogger.SbtTeamCityLogger ; sbt-teamcity-logger
     * ; clean
     * ; testOnly example.Test -- -t "specific test"
     * ```
     */
    @JvmStatic
    fun build(sbtCommands: String, prologueCommands: List<String> = emptyList()): String {
        val preparedArgs = prepareArgs(sbtCommands)
        val prologCommandsText = prologueCommands.asSequence().joinWithSemicolon(separator = " ")
        val separator = if (prologCommandsText.isEmpty() || preparedArgs.isEmpty()) "" else "\n"
        return prologCommandsText + separator + preparedArgs
    }

    /**
     * Converts the TeamCity `SBT commands:` field into commands for sbt's redirected command-file mode.
     *
     * The field is intentionally close to, but not exactly, a terminal prompt.
     *
     * Space-separated input such as `clean compile test` is treated like `sbt clean compile test`:
     * each shell word becomes a separate sbt command.
     *
     * Quoted input such as `"testOnly Foo -- -z \"name\""` is treated like one shell-quoted sbt argument,
     * so the surrounding quotes group the command and are not sent to sbt.
     *
     * Raw semicolon chains are the compatibility exception.
     * TeamCity accepts `;clean;compile;test` directly because the field is not parsed by a shell;
     * in a terminal the same chain would need quoting, for example `sbt ';clean;compile;test'`.
     *
     * The command file remains an implementation detail that lets the runner
     * prepend TeamCity logger setup commands before feeding the user's commands to sbt.
     */
    @JvmStatic
    fun prepareArgs(sbtCommands: String): String {
        val trimmedArgs = sbtCommands.trim()
        if (trimmedArgs.startsWith(";")) {
            return trimmedArgs
        }
        if (containsSemicolonOutsideQuotes(sbtCommands)) {
            return ";$trimmedArgs"
        }

        val commandsNormalised: Sequence<String> = splitCommandArgumentsAndUnquoteAll(sbtCommands)
            // The inner content of command arguments can contain escaped double quotes
            // Example: "testOnly org.MyTest -- -t \"my test name\"
            .map(::unescapeDoubleQuotes)
            .map(String::trim)
            .filter(String::isNotEmpty)
        return commandsNormalised.joinWithSemicolon(separator = "\n")
    }

    private fun containsSemicolonOutsideQuotes(text: String): Boolean =
        StringUtil.splitHonorQuotes(" $text ", ';').size > 1

    /**
     * Similar to [[StringUtil.splitCommandArgumentsAndUnquote]] but keeps single-quoted and double-quoted runner input equivalent.
     *
     * [StringUtil.splitCommandArgumentsAndUnquote] uses both quote characters for grouping but removes
     * only surrounding double quotes (") from the returned tokens.
     * The SBT runner treats surrounding single quotes as the same kind of command grouping,
     * so strip them here and unescape inner double quotes left by quoted command input.
     */
    private fun splitCommandArgumentsAndUnquoteAll(text: String): Sequence<String> =
        StringUtil.splitCommandArgumentsAndUnquote(text)
            .asSequence()
            .map(::unquoteSingleQuote)

    private fun unescapeDoubleQuotes(token: String): String {
        return token.replace("\\\"", "\"")
    }

    private fun unquoteSingleQuote(token: String): String {
        val isWithSingleQuotes = token.length >= 2 && token.first() == '\'' && token.last() == '\''
        val result = if (isWithSingleQuotes)
            token.substring(1, token.lastIndex)
        else
            token
        return result
    }

    // Input  : Seq(one, two, tree), separator = " "
    // Output : one ; two ; tree
    private fun Sequence<String>.joinWithSemicolon(separator: String): String =
        joinToString(separator = separator) { "; $it" }
}

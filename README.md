[![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

SBT runner for TeamCity
=============

This TeamCity plugin provides support for running SBT build on the TeamCity build server. 
More information could be found [here](https://www.jetbrains.com/help/teamcity/simple-build-tool-scala.html).

## Project structure
This project contains 3 modules:
   - 'tc-sbt-runner-agent'
   - 'tc-sbt-runner-common'.
   - 'tc-sbt-runner-server'

They contain code for server and agent parts of your plugin and a common part, available for both (agent and server).
When implementing components for server and agent parts, remember to update spring context files under 'main/resources/META-INF'.
Otherwise, your component may be not loaded. 
 
See TeamCity documentation for details on plugin development.

### Kotlin DSL descriptor
`kotlin-dsl/SBT.xml` describes the generated TeamCity Kotlin DSL API for the SBT runner. 
It defines the DSL build step type, class and function descriptions, parameter names, parameter descriptions, options,
and examples that Kotlin DSL users see in generated DSL documentation and IDE assistance.

The file is copied into the plugin ZIP under `kotlin-dsl/SBT.xml` by `build/plugin-assembly.xml`.
TeamCity consumes it as a DSL descriptor, not as the build step edit UI. The contents of `<description>`
elements are XML text and should be written as Markdown/KDoc-style documentation text. 
Existing descriptions already use Markdown-style links. 
Use Markdown block syntax, including blank lines before lists, when rendered line breaks or lists matter.

### Server module
`tc-sbt-runner-server` is the TeamCity server-side part of the plugin. It is built as a JAR and packaged
under the plugin ZIP `server` directory. It depends on TeamCity server APIs and `server-web-api`, and it
contains the server extension classes plus the JSP resources used by the TeamCity web UI.

The module is wired through `tc-sbt-runner-server/src/main/resources/META-INF/build-server-plugin-tc-sbt-runner.xml`.
That Spring descriptor registers:
- `SbtRunnerRunType`, which registers the SBT runner type, default parameters, validation, displayed
  parameter summary, and paths to the edit/view JSP files.
- `SbtRunnerDiscoveryExtension`, which lets TeamCity suggest the SBT runner when `.sbt` files are found.

The UI resources live in `tc-sbt-runner-server/src/main/resources/buildServerResources`:
- `editSbtRunParams.jsp` is displayed on the TeamCity build step edit page. It is processed as JSP and rendered as HTML in the browser.
- `viewSbtRunParams.jsp` is displayed on the read-only build step parameters view.

## IDE setup
Open the root `pom.xml` as a Maven project in IntelliJ IDEA. Maven is the source of truth for modules,
dependencies, repositories, and compiler settings; generated `.iml` files and local IDEA import state are
ignored.

## Building the plugin
Run `./mvnw package` from the root project to build your plugin.
The resulting package `tc-sbt-runner.zip` will be placed in the `target` directory.

The project compiles Java 8-compatible bytecode. By default, the Maven build uses TeamCity `2026.1`
artifacts and a timestamped plugin version.
Override them when building against another TeamCity baseline:

```
./mvnw package -DteamcityVersion=2026.1 -DteamcityPluginVersion=dev
```

### Building on the CI
On CI this command is used
`clean test package license:aggregate-add-third-party`~~~~

## Installing the plugin
To install the plugin, put the zip archive to the 'plugins' dir under the TeamCity data directory.
If you only changed agent-side code of your plugin, the upgrade will be performed 'on the fly' (agents will upgrade when idle).
If the common or server-side code has changed, restart the server.


## Useful links
https://plugins.jetbrains.com/docs/teamcity/getting-started-with-plugin-development.html

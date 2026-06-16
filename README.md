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

## IDE setup
Open the root `pom.xml` as a Maven project in IntelliJ IDEA. Maven is the source of truth for modules,
dependencies, repositories, and compiler settings; generated `.iml` files and local IDEA import state are
ignored.

## Building the plugin
Run 'mvn package' command from the root project to build your plugin.
The resulting package tc-sbt-runner.zip will be placed in the 'target' directory.

The project compiles Java 8-compatible bytecode. By default, the Maven build uses TeamCity `2023.05`
artifacts and a timestamped plugin version.
Override them when building against another TeamCity baseline:

```
mvn package -DteamcityVersion=2024.12 -DteamcityPluginVersion=dev
```

## Installing the plugin
To install the plugin, put the zip archive to the 'plugins' dir under the TeamCity data directory.
If you only changed agent-side code of your plugin, the upgrade will be performed 'on the fly' (agents will upgrade when idle).
If the common or server-side code has changed, restart the server.


## Useful links
https://plugins.jetbrains.com/docs/teamcity/getting-started-with-plugin-development.html
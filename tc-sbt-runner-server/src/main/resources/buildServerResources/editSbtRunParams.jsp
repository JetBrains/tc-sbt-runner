<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<l:settingsGroup title="SBT Parameters">
    <tr>
        <th>
            <label for="sbt.args">SBT commands:</label>
        </th>
        <td>
            <props:textProperty name="sbt.args" className="longField" expandable="true"/>
            <div class="smallNote">
                <p>Commands to execute.</p>
                <p>
                    Whitespace-separated arguments are treated as separate sbt commands.<br>
                    Quote a whole command when it contains spaces using <code>'...'</code> or <code>"..."</code>.
                </p>
                <p>Examples:</p>
                <ul style="margin: 0.25em 0; padding-left: 1.5em;">
                    <li><code>clean compile test</code> (runs 3 commands).</li>
                    <li><code>'set scalaVersion := "3.8.3"'</code> (runs 1 command).</li>
                    <li><code>"testOnly example.Test1 example.Test2"</code> (runs 1 command).</li>
                    <li><code>"testOnly example.Test1" "testOnly example.Test2"</code> (runs 2 commands).</li>
                    <li>
                        <code>"testOnly example.Test -- -t \"specific test\""</code> (runs 1 command)
                        (notice that the test case name escapes quotes as the command is inside quotes already).
                    </li>
                </ul>
                <p>
                    An alternative to using quotes can be using <code>;</code>.<br>
                    If a semicolon is detected in the input outside the quoted content, the input is treated as an sbt command chain.<br>
                    The leading <code>;</code> is optional.
                </p>
                <p>Examples:</p>
                <ul style="margin: 0.25em 0; padding-left: 1.5em;">
                    <li><code>clean ; set scalaVersion:="2.11.6" ; compile ; test</code> (sbt will run 4 commands).</li>
                    <li><code>;clean;compile;test</code> (sbt will run 3 commands).</li>
                    <li><code>"set name :=\"name with ; inside\""</code> (sbt runs 1 command, <code>;</code> is part of the quoted content).</li>
                </ul>
            </div>
        </td>
    </tr>
    <tr>
        <th><label for="sbt.installationMode">SBT installation mode:<l:star/></label></th>
        <td>
            <props:selectProperty name="sbt.installationMode" className="shortField" id="sbtInstallationSelection"
                                  onchange="syncSBTInstMode(); return true;">
                <props:option value="auto">&lt;Auto&gt;</props:option>
                <props:option value="custom">&lt;Custom&gt;</props:option>
            </props:selectProperty>
            <span id="sbt_installation_info" class="smallNote" style="display: inline;">TeamCity bundled SBT launcher will be used (version 1.10.10)</span>
            <span><bs:help file="Simple+Build+Tool+(Scala)"/></span>
        </td>
    </tr>
    <tr id="sbt.home_selection">
        <th><label for="sbt.home">SBT home path:<l:star/></label></th>
        <td>
            <props:textProperty name="sbt.home" className="longField"/>
            <span class="smallNote">The path to the existing SBT home directory</span>
            <span class="error" id="error_sbt.home"></span>
        </td>
    </tr>

    <forms:workingDirectory/>
    <script type="text/javascript">
        window.syncSBTInstMode = function () {
            if ($("sbtInstallationSelection").value == 'custom') {
                BS.Util.show("sbt.home_selection");
                $("sbt_installation_info").innerHTML = "The installed SBT will the launched from the SBT home"
            }
            else {
                BS.Util.hide("sbt.home_selection");
                $("sbt_installation_info").innerHTML = "TeamCity bundled SBT launcher will be used (version 1.10.10)"
            }
            BS.MultilineProperties.updateVisible();
        };
        window.syncSBTInstMode();
    </script>

</l:settingsGroup>
<l:settingsGroup title="Java Parameters" className="advancedSetting">
    <props:editJavaHome/>
    <props:editJvmArgs/>
</l:settingsGroup>

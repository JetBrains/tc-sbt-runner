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
            <span class="smallNote">Commands to execute, e.g. <i>clean "set scalaVersion="2.11.6"" compile test</i> or <i>;clean;set scalaVersion="2.11.6";compile;test</i></span>
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
            <span id="sbt_installation_info" class="smallNote">In 'Auto' mode the latest SBT version will be automatically installed</span>
        </td>
    </tr>
    <tr id="sbt.home_selection">
        <th><label for="sbt.home">SBT home path:<l:star/></label></th>
        <td>
            <props:textProperty name="sbt.home" className="longField"/>
            <span class="smallNote">Path to existed SBT directory.</span>
            <span class="error" id="error_sbt.home"></span>
        </td>
    </tr>

    <forms:workingDirectory/>
    <script type="text/javascript">
        window.syncSBTInstMode = function () {
            if ($("sbtInstallationSelection").value == 'custom') {
                BS.Util.show("sbt.home_selection");
                $("sbt_installation_info").innerHTML = "In 'Custom' mode sbt-launch.jar from \\bin folder under SBT home will be launched"
            }
            else {
                BS.Util.hide("sbt.home_selection");
                $("sbt_installation_info").innerHTML = "In 'Auto' mode the latest SBT version will be automatically installed"
            }
            BS.MultilineProperties.updateVisible();
        };
        window.syncSBTInstMode();
    </script>

</l:settingsGroup>
<l:settingsGroup title="Java Parameters">
    <props:editJavaHome/>
    <props:editJvmArgs/>
</l:settingsGroup>


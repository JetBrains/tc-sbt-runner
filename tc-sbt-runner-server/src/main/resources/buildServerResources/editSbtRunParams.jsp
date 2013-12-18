<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Sbt Parameters">

    <tr>
        <th>
            <label for="sbt.args">Sbt commands:</label>
        </th>
        <td>
            <props:textProperty name="sbt.args" className="longField"/>
            <span class="smallNote">Commands to execute.</span>
        </td>
    </tr>

    <tr>
        <th><label for="sbt.home">Sbt home path: <l:star/></label></th>
        <td>
            <props:textProperty name="sbt.home" className="longField"/>
            <span class="error" id="error_sbt.home"></span>
        </td>
    </tr>

    <tr>
        <th><label for="sbt.version">Sbt version:</label></th>
        <td>
            <props:textProperty name="sbt.version" className="longField"/>
            <span class="smallNote">Sbt version to use, optional, usually taken from project/build.properties file</span>
        </td>
    </tr>

    <forms:workingDirectory />

</l:settingsGroup>
<l:settingsGroup title="Java Parameters">
    <props:editJavaHome/>
    <props:editJvmArgs/>
</l:settingsGroup>


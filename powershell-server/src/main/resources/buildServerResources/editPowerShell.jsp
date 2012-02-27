<%--
  ~ Copyright 2000-2010 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<jsp:useBean id="bean" class="jetbrains.buildServer.powershell.server.PowerShellBean"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<tr>
  <th><label for="${bean.bitnessKey}">Powershell run mode</label></th>
  <td>
    <props:selectProperty name="${bean.bitnessKey}">
      <c:forEach var="val" items="${bean.bitnessValues}">
        <props:option value="${val.value}"><c:out value="${val.description}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="error" id="error_${bean.bitnessKey}"></span>
  </td>
</tr>

<forms:workingDirectory/>

<tr>
  <th>Script:</th>
  <td>
    <props:selectProperty name="${bean.scriptModeKey}" id="powershell_option" className="longField" onchange="BS.PowerShell.updateScriptType()">
      <props:option value="${bean.scriptModeFileValue}">File</props:option>
      <props:option value="${bean.scriptModeCodeValue}">Source code</props:option>
    </props:selectProperty>
    <span class="error" id="error_${bean.scriptModeKey}"></span>
  </td>
</tr>

<tr id="powershell_scriptFile">
  <th><label for="${bean.scriptFileKey}">Script file:</label></th>
  <td>
    <props:textProperty name="${bean.scriptFileKey}" className="longField"/>
    <span class="smallNote">Path to the Powershell script, relative to the checkout directory</span>
    <span class="error" id="error_${bean.scriptFileKey}"></span>
  </td>
</tr>

<tr id="powershell_sourceCode">
  <th><label for="${bean.scriptCodeKey}">Script source:</label></th>
  <td>
    <props:multilineProperty name="${bean.scriptCodeKey}"
                             linkTitle="Enter Powershell script content"
                             cols="58" rows="10"
                             expanded="${true}"/>
    <span class="smallNote">Enter contents of a Powershell script. TeamCity references will be replaced in the code</span>
    <span class="error" id="error_${bean.scriptCodeKey}"></span>
  </td>
</tr>

<tr>
    <th><label for="${bean.executionModeKey}">Script execution mode:</label></th>
    <td>
        <props:selectProperty name="${bean.executionModeKey}" id="powershell_execution_mode" className="longField" onchange="BS.PowerShell.updateScriptMode()">
            <props:option value="${bean.executionModeAsFileValue}">Execute .ps1 script with "-File" argument</props:option>
            <props:option value="${bean.executionModeStdinValue}" selected="${empty propertiesBean.properties[bean.executionModeKey] or propertiesBean.properties[bean.executionModeKey] eq bean.executionModeStdinValue}">Put script into PowerShell stdin with "-Command -" arguments</props:option>
        </props:selectProperty>
        <span class="smallNote">
            Specify Powershell script execution mode. By default, Powershell may not allow
            execution of arbitrary .ps1 files. Select 'Put script into powershell stdin' mode to avoid this.
        </span>
        <span class="error" id="error_${bean.executionModeKey}"></span>
    </td>
</tr>

<tr id="powershell_scriptArguments">
  <th><label for="${bean.scriptArgmentsKey}">Script arguments:</label></th>
  <td>
    <props:multilineProperty name="${bean.scriptArgmentsKey}" cols="58" linkTitle="Expand" rows="5"/>
    <span class="smallNote">Enter script arguments</span>
    <span class="error" id="error_${bean.scriptArgmentsKey}"></span>
  </td>
</tr>

<tr>
  <th><label for="${bean.argumentsKey}">Additional command line parameters:</label></th>
  <td>
    <props:multilineProperty name="${bean.argumentsKey}"  cols="58" linkTitle="Expand" rows="5"/>
    <span class="smallNote">Enter additional command line parameters to powershell.exe.</span>
    <span class="error" id="error_${bean.argumentsKey}"></span>
  </td>
</tr>


<script type="text/javascript">
  BS.PowerShell = {
     updateScriptType : function() {
       var val = $('powershell_option').value;
       if (val == '${bean.scriptModeFileValue}') {
         BS.Util.hide($('powershell_sourceCode'));
         BS.Util.show($('powershell_scriptFile'));
       }
       if (val == '${bean.scriptModeCodeValue}') {
         BS.Util.show($('powershell_sourceCode'));
         BS.Util.hide($('powershell_scriptFile'));
       }
       BS.MultilineProperties.updateVisible();
     },

     updateScriptMode : function() {
       var val = $('powershell_execution_mode').value;
       if (val == '${bean.executionModeAsFileValue}') {
         BS.Util.show($('powershell_scriptArguments'));
       }
       if (val == '${bean.executionModeStdinValue}') {
         BS.Util.hide($('powershell_scriptArguments'));
       }
       BS.MultilineProperties.updateVisible();
     }
  };

  BS.PowerShell.updateScriptType();
  BS.PowerShell.updateScriptMode();
</script>
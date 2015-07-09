<%--
  ~ Copyright 2000-2014 JetBrains s.r.o.
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
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="bean" class="jetbrains.buildServer.powershell.server.PowerShellBean"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<tr>
  <th rowspan="2">Powershell run mode:</th>
  <td>
    <label for="${bean.minVersionKey}">Version: </label>
    <props:selectProperty name="${bean.minVersionKey}" id="powershell_minVersion" onchange="BS.PowerShell.updateScriptMode()">
      <props:option value="">Any</props:option>
      <c:forEach var="it" items="${bean.versions}">
        <props:option value="${it.version}"><c:out value="${it.version}"/></props:option>
      </c:forEach>
    </props:selectProperty>
  </td>
</tr>

<tr>
  <td>
    <label for="${bean.bitnessKey}">Bitness: </label>
    <props:selectProperty name="${bean.bitnessKey}">
      <c:forEach var="val" items="${bean.bitnessValues}">
        <props:option value="${val.value}"><c:out value="${val.description}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="error" id="error_${bean.bitnessKey}"></span>
  </td>
</tr>

<tr class="advancedSetting">
  <th><label for="${bean.errorToErrorKey}">Format stderr output as:</label></th>
  <td>
    <props:selectProperty name="${bean.errorToErrorKey}">
      <props:option value="">warning</props:option>
      <props:option value="true">error</props:option>
    </props:selectProperty>
    <span class="smallNote">Specify how error output is processed</span>
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
    <bs:vcsTree fieldId="${bean.scriptFileKey}"/>
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
                             expanded="${true}"
                             note="Enter contents of a Powershell script. TeamCity references will be replaced in the code"/>
  </td>
</tr>

<tr>
  <th><label for="${bean.executionModeKey}">Script execution mode:</label></th>
  <td>
    <props:selectProperty name="${bean.executionModeKey}" id="powershell_execution_mode" className="longField" onchange="BS.PowerShell.updateScriptMode()">
      <props:option value="${bean.executionModeAsFileValue}">Execute .ps1 from external file</props:option>
      <props:option value="${bean.executionModeStdinValue}" selected="${empty propertiesBean.properties[bean.executionModeKey] or propertiesBean.properties[bean.executionModeKey] eq bean.executionModeStdinValue}">Put script into PowerShell stdin with "-Command -" argument</props:option>
    </props:selectProperty>
        <span class="smallNote">
            Specify Powershell script execution mode. By default, Powershell may not allow
            execution of arbitrary .ps1 files. TeamCity will try to supply -ExecutionPolicy ByPass argument.
        </span>
    <span class="error" id="error_${bean.executionModeKey}"></span>
    <div class="attentionComment" id="warn_executionMode">
      Executing scripts from stdin with "-Command -" is unstable and can result in build failures. Consider executing PowerShell script from external file. <bs:help file="PowerShell"/>
    </div>
  </td>
</tr>

<tr id="powershell_scriptArguments">
  <th><label for="${bean.scriptArgmentsKey}">Script arguments:</label></th>
  <td>
    <props:multilineProperty name="${bean.scriptArgmentsKey}" cols="58" linkTitle="Expand" rows="5" note="Enter script arguments"/>
  </td>
</tr>

<tr class="advancedSetting">
  <th>Options:</th>
  <td>
    <props:checkboxProperty name="${bean.noProfileKey}"/>
    <label for="${bean.noProfileKey}">Add -NoProfile argument</label>
  </td>
</tr>

<tr class="advancedSetting">
  <th><label for="${bean.argumentsKey}">Additional command line parameters:</label></th>
  <td>
    <props:textProperty name="${bean.argumentsKey}" className="longField" expandable="true"/>
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
      var ver  = $('powershell_minVersion').value;

      if (val == '${bean.executionModeAsFileValue}') {
        BS.Util.show($('powershell_scriptArguments'));
        BS.Util.hide($('warn_executionMode'));
      }
      if (val == '${bean.executionModeStdinValue}') {
        BS.Util.hide($('powershell_scriptArguments'));
        if (ver != '1.0') {
          BS.Util.show($('warn_executionMode'));
        } else {
          BS.Util.hide($('warn_executionMode'));
        }
      }

      BS.MultilineProperties.updateVisible();
    }
  };

  BS.PowerShell.updateScriptType();
  BS.PowerShell.updateScriptMode();
</script>
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

<tr class="advancedSetting">
  <th rowspan="3">PowerShell run mode:</th>
  <td>
    <label for="${bean.minVersionKey}" class="fixedLabel">Version: </label>
    <props:textProperty name="${bean.minVersionKey}" className="smallField " onkeyup="BS.PowerShell.updateScriptMode()"/>
  </td>
</tr>

<tr class="advancedSetting">
  <td class="noBorder">
    <label for="${bean.bitnessKey}" class="fixedLabel">Platform: </label>
    <props:selectProperty name="${bean.bitnessKey}" className="smallField ">
      <c:forEach var="val" items="${bean.bitnessValues}">
        <props:option value="${val.value}"><c:out value="${val.key}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="error" id="error_${bean.bitnessKey}"></span>
  </td>
</tr>

<tr class="advancedSetting">
  <td class="noBorder">
    <label for="${bean.editionKey}" class="fixedLabel">Edition: </label>
    <props:selectProperty name="${bean.editionKey}" className="smallField ">
      <c:forEach var="val" items="${bean.editionValues}">
        <props:option value="${val.value}"><c:out value="${val.key}"/></props:option>
      </c:forEach>
    </props:selectProperty>
  </td>
</tr>

<tr class="advancedSetting">
  <th><label for="${bean.errorToErrorKey}">Format stderr output as:</label></th>
  <td>
    <props:selectProperty name="${bean.errorToErrorKey}" className="smallField">
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
    <props:selectProperty name="${bean.scriptModeKey}" id="powershell_option" className="shortField" onchange="BS.PowerShell.updateScriptType()">
      <props:option value="${bean.scriptModeFileValue}">File</props:option>
      <props:option value="${bean.scriptModeCodeValue}">Source code</props:option>
    </props:selectProperty>
    <span class="error" id="error_${bean.scriptModeKey}"></span>
  </td>
</tr>

<tr id="powershell_scriptFile">
  <th><label for="${bean.scriptFileKey}">Script file: <l:star/></label></th>
  <td>
    <props:textProperty name="${bean.scriptFileKey}" className="longField"/>
    <bs:vcsTree fieldId="${bean.scriptFileKey}"/>
    <span class="smallNote">Path to the PowerShell script, relative to the checkout directory</span>
    <span class="error" id="error_${bean.scriptFileKey}"></span>
  </td>
</tr>

<tr id="powershell_sourceCode">
  <th><label for="${bean.scriptCodeKey}">Script source: <l:star/></label></th>
  <td>
    <props:multilineProperty name="${bean.scriptCodeKey}"
                             linkTitle="Enter PowerShell script content"
                             cols="58" rows="10"
                             expanded="${true}"
                             note="Enter contents of a PowerShell script. TeamCity references will be replaced in the code"/>
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
            Specify PowerShell script execution mode. By default, PowerShell may not allow
            execution of arbitrary .ps1 files. TeamCity will try to supply -ExecutionPolicy ByPass argument.
        </span>
    <span class="error" id="error_${bean.executionModeKey}"></span>
    <forms:attentionComment id="warn_executionMode">
      Executing scripts from stdin with "-Command -" is unstable and can result in build failures. Consider executing PowerShell script from external file. <bs:help file="PowerShell"/>
    </forms:attentionComment>
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
      var ver  = $('${bean.minVersionKey}').value;

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
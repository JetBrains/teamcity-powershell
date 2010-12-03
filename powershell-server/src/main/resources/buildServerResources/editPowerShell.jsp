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
<jsp:useBean id="bean" scope="request" type="jetbrains.buildServer.powershell.server.PowerShellBean"/>

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
    <props:selectProperty name="${bean.scriptModeKey}">
      <props:option value="${bean.scriptModeFileValue}">File</props:option>
      <props:option value="${bean.scriptModeCodeValue}">Source code</props:option>
    </props:selectProperty>
    <span class="error" id="error_${bean.scriptModeKey}"></span>
  </td>
</tr>

<tr>
  <th><label for="${bean.scriptFileKey}">Script file:</label></th>
  <td>
    <props:textProperty name="${bean.scriptFileKey}" className="longField"/>
    <span class="smallNote">Enter Powershell file path relative to checkout directory</span>
    <span class="error" id="error_${bean.scriptFileKey}"></span>
  </td>
</tr>

<tr>
  <th><label for="${bean.scriptCodeKey}">Script source:</label></th>
  <td>
    <props:multilineProperty name="${bean.scriptCodeKey}"
                             linkTitle="Enter Powershell script content" cols="58" rows="10" />
    <span class="smallNote">Enter Powershell script</span>
    <span class="error" id="error_${bean.scriptCodeKey}"></span>
  </td>
</tr>

<tr>
  <th><label for="${bean.argumentsKey}">Additional command line parameters:</label></th>
  <td>
    <props:textProperty name="${bean.argumentsKey}" className="longField"/>
    <span class="smallNote">Enter additional command line parameters to powershell.exe.</span>
    <span class="error" id="error_${bean.argumentsKey}"></span>
  </td>
</tr>

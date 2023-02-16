<%--
  ~ Copyright 2000-2023 JetBrains s.r.o.
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


<div class="parameter">
  PowerShell run mode:
  <props:displayValue name="${bean.minVersionKey}" emptyValue="Any version"/>
  <strong>
    <c:set var="type" value="${propertiesBean.properties[bean.bitnessKey]}"/>
    <c:forEach var="val" items="${bean.bitnessValues}">
      <c:if test="${type eq val.value or empty type and empty val.value}"><c:out value="bitness: ${val.key}"/></c:if>
    </c:forEach>
    <c:set var="edition" value="${propertiesBean.properties[bean.editionKey]}"/>
    <c:forEach var="val" items="${bean.editionValues}">
      <c:if test="${edition eq val.value or empty edition and empty val.value}"><c:out value="edition: ${val.key}"/></c:if>
    </c:forEach>
  </strong>
</div>

<div class="parameter">
  Treat error output as: <props:displayCheckboxValue name="${bean.errorToErrorKey}" checkedValue="error" uncheckedValue="warning"/>
</div>

<props:viewWorkingDirectory />

<c:if test="${propertiesBean.properties[bean.scriptModeKey] eq bean.scriptModeFileValue}">
  <div class="parameter">
    Script: <props:displayValue name="${bean.scriptFileKey}"/>
  </div>
</c:if>

<c:if test="${propertiesBean.properties[bean.scriptModeKey] eq bean.scriptModeCodeValue}">
  <div class="parameter">
    Custom script: <props:displayValue name="jetbrains_powershell_script_code" emptyValue="<empty>" showInPopup="true" popupTitle="Script content" popupLinkText="view script content"/>
  </div>
</c:if>

<div class="parameter">
  Add -NoProfile argument: <props:displayCheckboxValue name="${bean.noProfileKey}" checkedValue="Yes" uncheckedValue="No"/>
</div>

<div class="parameter">
  Additional command line arguments: <props:displayValue name="${bean.argumentsKey}"/>
</div>

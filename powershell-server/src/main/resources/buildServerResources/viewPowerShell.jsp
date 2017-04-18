<%--
  ~  Copyright 2000 - 2017 JetBrains s.r.o.
  ~
  ~  Licensed under the Apache License, Version 2.0 (the "License").
  ~  See LICENSE in the project root for license information.
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
      <c:if test="${type eq val.value}"><c:out value="${val.key}"/></c:if>
    </c:forEach>
    <c:set var="edition" value="${propertiesBean.properties[bean.editionKey]}"/>
    <c:forEach var="val" items="${bean.editionValues}">
      <c:if test="${edition eq val.value}"><c:out value="${val.key} Edition"/></c:if>
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
    Script: <strong>custom</strong>
  </div>
</c:if>

<div class="parameter">
  Add -NoProfile argument: <props:displayCheckboxValue name="${bean.noProfileKey}"/>
</div>

<div class="parameter">
  Additional command line arguments: <props:displayValue name="${bean.argumentsKey}"/>
</div>

<%--
  ~ Copyright 2000-2013 JetBrains s.r.o.
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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/include-internal.jsp" %>
<%@ page import="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" %>

<jsp:useBean id="healthStatusItem" type="jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem" scope="request"/>
<jsp:useBean id="showMode" type="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" scope="request"/>

<%--@elvariable id="buildType" type="jetbrains.buildServer.serverSide.SBuildType"--%>
<c:set var="buildType" value="${healthStatusItem.additionalData['build_type']}"/>
<%--@elvariable id="steps" type="java.util.List<java.lang.String>"--%>
<c:set var="steps" value="${healthStatusItem.additionalData['steps']}"/>

<c:set var="inplaceMode" value="<%=HealthStatusItemDisplayMode.IN_PLACE%>"/>

<admin:editBuildTypeNavSteps settings="${buildType}"/>
<%--@elvariable id="buildConfigSteps" type="type="java.util.ArrayList<jetbrains.buildServer.controllers.admin.projects.ConfigurationStep>"--%>

<c:set var="numSteps" value="${fn:length(steps)}"/>

<div>
  <admin:editBuildTypeLink buildTypeId="${buildType.externalId}" step="${buildConfigSteps[2].stepId}">
    <span style="white-space: nowrap">${buildType.extendedFullName}</span>
  </admin:editBuildTypeLink>
  contains <bs:out value="${numSteps}"/> PowerShell step<bs:s val="${numSteps}"/> that use<bs:s val="${numSteps}"/> <strong>-Command</strong> mode
  to execute scripts. Consider using external file mode instead.<bs:help file="PowerShell"/>
</div>
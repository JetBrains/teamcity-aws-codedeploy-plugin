<%--
  ~ Copyright 2000-2016 JetBrains s.r.o.
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

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<%@include file="paramsConstants.jspf"%>

<div class="parameter">
    ${revision_path_label}: <strong><props:displayValue name="${revision_path_param}" emptyValue="empty"/></strong>
</div>

<div class="parameter">
    ${region_name_label}: <strong><props:displayValue name="${region_name_param}" emptyValue="empty"/></strong>
</div>

<div class="parameter">
    ${access_key_id_label}: <strong><props:displayValue name="${access_key_id_param}" emptyValue="empty"/></strong>
</div>

<div class="parameter">
    ${bucket_name_label}: <strong><props:displayValue name="${bucket_name_param}" emptyValue="empty"/></strong>
</div>

<div class="parameter">
    ${app_name_label}: <strong><props:displayValue name="${app_name_param}" emptyValue="empty"/></strong>
</div>

<div class="parameter">
    ${dep_group_name_label}: <strong><props:displayValue name="${dep_group_name_param}" emptyValue="empty"/></strong>
</div>

<div class="parameter">
    ${dep_config_name_label}: <strong><props:displayValue name="${dep_config_name_param}" emptyValue="default"/></strong>
</div>

<div class="parameter">
    ${wait_flag_label}: <strong><props:displayCheckboxValue name="${wait_flag_param}"/></strong>
</div>

<c:set var="wait_flag" value="${propertiesBean.properties[wait_flag_param]}"/>
<c:if test="${empty wait_flag or ('true' eq wait_flag)}">
    <div class="parameter">
        ${wait_timeout_label}: <strong><props:displayValue name="${wait_timeout_param}" emptyValue="empty"/></strong>
    </div>

    <div class="parameter">
        ${wait_poll_interval_label}: <strong><props:displayValue name="${wait_poll_interval_param}" emptyValue="default"/></strong>
    </div>
</c:if>


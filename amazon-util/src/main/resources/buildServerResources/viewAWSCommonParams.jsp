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

<%@include file="constantsAWSCommonParams.jspf"%>

<c:set var="region" value="${allRegions[propertiesBean.properties[region_name_param]]}"/>
<div class="parameter">
    ${region_name_label}: <strong>${empty region ? 'empty' : region}</strong>
</div>

<c:set var="cred_type" value="${propertiesBean.properties[credentials_type_param]}"/>
<c:if test="${temp_credentials_option eq cred_type}">
    <div class="parameter">
            ${iam_role_arn_label}: <props:displayValue name="${iam_role_arn_param}" emptyValue="empty"/>
    </div>
    <div class="parameter">
            ${external_id_label}: <props:displayValue name="${external_id_param}" emptyValue="empty"/>
    </div>
</c:if>

<div class="parameter">
    ${use_default_cred_chain_label}: <strong><props:displayCheckboxValue name="${use_default_cred_chain_param}"/></strong>
</div>

<c:set var="use_default" value="${propertiesBean.properties[use_default_cred_chain_param]}"/>
<c:if test="${empty use_default or ('false' eq use_default)}">
    <div class="parameter">
            ${access_key_id_label}: <props:displayValue name="${access_key_id_param}" emptyValue="empty"/>
    </div>
</c:if>

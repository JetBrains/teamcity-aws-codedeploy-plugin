

<%@include file="paramsConstants.jspf"%>

<c:set var="deploymentSteps" value="${deploymentScenarios[propertiesBean.properties[deployment_steps_param]]}"/>
<div class="parameter">
    ${deployment_steps_label}: <strong>${empty deploymentSteps ? 'empty' : deploymentSteps}</strong>
</div>

<jsp:include page="viewAWSCommonParams.jsp"/>

<div class="parameter">
    ${revision_path_label}: <props:displayValue name="${revision_path_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${bucket_name_label}: <props:displayValue name="${bucket_name_param}" emptyValue="empty"/>
</div>

<c:set var="s3_object_key" value="${propertiesBean.properties[s3_object_key_param]}"/>
<c:if test="${not empty s3_object_key}">
    <div class="parameter">
            ${s3_object_key_label}: <props:displayValue name="${s3_object_key_param}" emptyValue="empty"/>
    </div>
</c:if>

<div class="parameter">
    ${app_name_label}: <props:displayValue name="${app_name_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${dep_group_name_label}: <props:displayValue name="${dep_group_name_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${green_fleet_label}: <props:displayValue name="${green_fleet_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${dep_config_name_label}: <props:displayValue name="${dep_config_name_param}" emptyValue="default"/>
</div>

<div class="parameter">
    ${rollback_on_failure_label}: <strong><props:displayCheckboxValue name="${rollback_on_failure_param}"/></strong>
</div>
<div class="parameter">
    ${rollback_on_alarm_label}: <strong><props:displayCheckboxValue name="${rollback_on_alarm_param}"/></strong>
</div>
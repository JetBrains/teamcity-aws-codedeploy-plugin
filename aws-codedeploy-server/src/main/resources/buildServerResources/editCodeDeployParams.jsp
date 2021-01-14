<%--
  ~ Copyright 2000-2021 JetBrains s.r.o.
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

<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<style type="text/css">
    .runnerFormTable span.facultativeNote {
        display: none;
    }

    .runnerFormTable span.facultativeAsterix {
        display: none;
    }
</style>


<%@include file="paramsConstants.jspf"%>

<tr>
    <th><label for="${deployment_steps_param}">${deployment_steps_label}: <l:star/></label></th>
    <td><props:selectProperty name="${deployment_steps_param}" className="longField" enableFilter="true" onchange="codeDeployUpdateStepNote();codeDeployUpdateVisibility()">
            <props:option id="${upload_register_deploy_steps}" value="${upload_register_deploy_steps}">${deploymentScenarios[upload_register_deploy_steps]}</props:option>
            <props:option id="${upload_register_steps}" value="${upload_register_steps}">${deploymentScenarios[upload_register_steps]}</props:option>
            <props:option id="${register_deploy_steps}" value="${register_deploy_steps}">${deploymentScenarios[register_deploy_steps]}</props:option>
            <props:option id="${deploy_step}" value="${deploy_step}">${deploymentScenarios[deploy_step]}</props:option>
            <props:option id="${upload_step}" value="${upload_step}">${deploymentScenarios[upload_step]}</props:option>
        </props:selectProperty>
        <span class="smallNote stepNote facultativeNote" id="${upload_register_deploy_steps}_note">Upload revision to S3, register it in CodeDeploy application and start deployment</span>
        <span class="smallNote stepNote facultativeNote" id="${upload_register_steps}_note">Upload revision to S3 and register it in CodeDeploy application</span>
        <span class="smallNote stepNote facultativeNote" id="${register_deploy_steps}_note">Register previously uploaded revision in CodeDeploy application and starts deployment</span>
        <span class="smallNote stepNote facultativeNote" id="${deploy_step}_note">Deploy previously uploaded and registered application revision</span>
        <span class="smallNote stepNote facultativeNote" id="${upload_step}_note">Upload application revision to S3</span>
        <span class="error" id="error_${deployment_steps_param}"></span>
    </td>
</tr>

<jsp:include page="editAWSCommonParams.jsp"/>

<l:settingsGroup title="Revision Location">
    <tr data-steps="${upload_step}">
        <th><label for="${revision_path_param}">${revision_path_label}: <l:star/></label></th>
        <td><props:textProperty name="${revision_path_param}" className="longField" maxlength="256" expandable="true"/>
            <span class="smallNote">Path to a ready-made revision archive or newline-separated list of files to package into revision</span>
            <span class="smallNote">${revision_path_note}</span>
            <span class="smallNote">Must include appspec.yml</span>
            <span class="error" id="error_${revision_path_param}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${bucket_name_param}">${bucket_name_label}: <l:star/></label></th>
        <td><props:textProperty name="${bucket_name_param}" className="longField" maxlength="256"/><a href="https://console.aws.amazon.com/s3" target="_blank" rel="noopener noreferrer">Open S3 Console</a>
            <span class="smallNote">Existing S3 bucket name</span><span class="error" id="error_${bucket_name_param}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${s3_object_key_param}">${s3_object_key_label}: <span id="${s3_object_key_param}_star" class="mandatoryAsterix facultativeAsterix" title="Mandatory field">*</span></label></th>
        <td><props:textProperty name="${s3_object_key_param}" className="longField" maxlength="256"/>
            <span id="${s3_object_key_param}_note" class="smallNote facultativeNote">Leave empty to use application revision archive name as a key</span>
            <span id="${s3_object_key_param}_mandatory_note" class="smallNote facultativeNote">Unique path inside the bucket</span>
            <span class="error" id="error_${s3_object_key_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<tr class="groupingTitle" data-steps="${register_deploy_steps}">
    <td colspan="2">CodeDeploy Application</td>
</tr>
<tr data-steps="${register_deploy_steps}">
    <th><label for="${app_name_param}">${app_name_label}: <l:star/></label></th>
    <td><props:textProperty name="${app_name_param}" className="longField" maxlength="256"/><a href="https://console.aws.amazon.com/codedeploy" target="_blank" rel="noopener noreferrer">Open CodeDeploy Console</a>
        <span class="smallNote">Pre-configured CodeDeploy application name</span><span class="error" id="error_${app_name_param}"></span>
    </td>
</tr>
<tr data-steps="${deploy_step}">
    <th><label for="${dep_group_name_param}">${dep_group_name_label}: <l:star/></label></th>
    <td><props:textProperty name="${dep_group_name_param}" className="longField" maxlength="256"/>
        <span class="smallNote">Pre-configured instances, must be running for deployment to succeed</span><span class="error" id="error_${dep_group_name_param}"></span>
    </td>
</tr>
<tr data-steps="${deploy_step}">
    <th><label for="${green_fleet_param}">${green_fleet_label}: </label></th>
    <td><props:textProperty name="${green_fleet_param}" className="longField" maxlength="256" expandable="true"/>
        <span class="smallNote">For blue/green deployments with manual replacement instances provision: newline-separated list of EC2 tag key/value pairs or auto scaling group names</span>
        <span class="error" id="error_${green_fleet_param}"></span>
    </td>
</tr>
<tr data-steps="${deploy_step}">
    <th><label for="${dep_config_name_param}">${dep_config_name_label}: </label></th>
    <td><props:textProperty name="${dep_config_name_param}" className="longField" maxlength="256"/>
        <span class="smallNote">e.g. "CodeDeployDefault.OneAtATime", "CodeDeployDefault.AllAtOnce" or a custom one, leave blank for default configuration</span><span class="error" id="error_${dep_config_name_param}"></span>
    </td>
</tr>
<tr class="groupingTitle" data-steps="${deploy_step}">
    <td colspan="2">Wait and Rollback</td>
</tr>
<tr data-steps="${deploy_step}">
    <th><label for="${wait_flag_param}">${wait_flag_label}: </label></th>
    <td><props:checkboxProperty name="${wait_flag_param}" uncheckedValue="false" onclick="codeDeployUpdateVisibility()"/></td>
</tr>
<tr id="${wait_timeout_param}_row" data-steps="${deploy_step}">
    <th><label for="${wait_timeout_param}">${wait_timeout_label}: <l:star/></label></th>
    <td><props:textProperty name="${wait_timeout_param}" maxlength="256"/>
        <span class="smallNote">Build will fail if the timeout is exceeded</span><span class="error" id="error_${wait_timeout_param}"></span>
    </td>
</tr>
<tr data-steps="${deploy_step}">
    <th><label for="${rollback_on_failure_param}">${rollback_on_failure_label}: </label></th>
    <td><props:checkboxProperty name="${rollback_on_failure_param}" uncheckedValue="false"/></td>
</tr>
<tr data-steps="${deploy_step}">
    <th><label for="${rollback_on_alarm_param}">${rollback_on_alarm_label}: </label></th>
    <td><props:checkboxProperty name="${rollback_on_alarm_param}" uncheckedValue="false"/></td>
</tr>

<tr class="groupingTitle" data-steps="${deploy_step}">
    <td colspan="2">File Exists Behavior</td>
</tr>

<tr data-steps="${deploy_step}">
    <th><label for="${file_exists_behavior_param}">${file_exists_behavior_label}: </label></th>
    <td><props:selectProperty name="${file_exists_behavior_param}" className="mediumField">
        <props:option value="">&lt;Default&gt;</props:option>
        <props:option value="OVERWRITE">Overwrite</props:option>
        <props:option value="RETAIN">Retain</props:option>
    </props:selectProperty>
    </td>
</tr>

<script type="application/javascript">
    window.codeDeployUpdateStepNote = function () {
        $j('#runnerParams .stepNote.facultativeNote').each(function() {
            BS.Util.hide(this);
        });
        BS.Util.show($j(BS.Util.escapeId('${deployment_steps_param}') + ' option:selected').attr('id') + '_note');
    };

    window.codeDeployUpdateVisibility = function () {
        // hide all and then show necessary
        $j('#runnerParams tr[data-steps]').each(function() {
            BS.Util.hide(this);
        });

        var deploySteps = $j(BS.Util.escapeId('${deployment_steps_param}') + ' option:selected').val();
        $j.each(deploySteps.split('${step_separator}'), function(i, val) {
            if (val) {
                $j("#runnerParams tr[data-steps*='" + val + "']").each(function() {
                    BS.Util.show(this);
                });
            }
        });

        if (deploySteps.indexOf('${deploy_step}') >= 0) {
            if ($j(BS.Util.escapeId('${wait_flag_param}')).is(':checked')) {
                BS.Util.show('${wait_timeout_param}_row');
            } else {
                BS.Util.hide('${wait_timeout_param}_row');
            }
        }

        if (deploySteps.indexOf('${upload_step}') < 0) {
            BS.Util.show('${s3_object_key_param}_star');
            BS.Util.show('${s3_object_key_param}_mandatory_note');
            BS.Util.hide('${s3_object_key_param}_note');
        } else {
            BS.Util.hide('${s3_object_key_param}_star');
            BS.Util.hide('${s3_object_key_param}_mandatory_note');
            BS.Util.show('${s3_object_key_param}_note');
        }

        BS.VisibilityHandlers.updateVisibility('runnerParams');
    };

    codeDeployUpdateStepNote();
    codeDeployUpdateVisibility();
</script>


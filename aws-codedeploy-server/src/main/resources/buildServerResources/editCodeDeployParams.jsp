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

<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<style type="text/css">
    .runnerFormTable span.stepNote {
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
        <span class="smallNote stepNote" id="${upload_register_deploy_steps}_note">Upload revision to S3, register it in CodeDeploy application and start deployment</span>
        <span class="smallNote stepNote" id="${upload_register_steps}_note">Upload revision to S3 and register it in CodeDeploy application</span>
        <span class="smallNote stepNote" id="${register_deploy_steps}_note">Register previously uploaded revision in CodeDeploy application and starts deployment</span>
        <span class="smallNote stepNote" id="${deploy_step}_note">Deploy previously uploaded and registered application revision</span>
        <span class="smallNote stepNote" id="${upload_step}_note">Upload application revision to S3</span>
        <span class="error" id="error_${deployment_steps_param}"></span>
    </td>
</tr>
<tr>
    <th><label for="${region_name_param}">${region_name_label}: <l:star/></label></th>
    <td><props:selectProperty name="${region_name_param}" className="longField" enableFilter="true">
        <props:option value="${null}">-- Select region --</props:option>
        <c:forEach var="region" items="${allRegions.keySet()}">
            <props:option value="${region}"><c:out value="${allRegions[region]}"/></props:option>
        </c:forEach>
    </props:selectProperty>
        <span class="smallNote">All CodeDeploy and S3 resources must be located in this region</span><span class="error" id="error_${region_name_param}"></span>
    </td>
</tr>

<l:settingsGroup title="AWS Security Credentials">
    <tr>
        <th><label for="${credentials_type_param}">${credentials_type_label}: <l:star/></label></th>
        <td><props:radioButtonProperty name="${credentials_type_param}" value="${access_keys_option}" id="${access_keys_option}" onclick="codeDeployUpdateVisibility()"/>
            <label for="${access_keys_option}">${access_keys_label}</label>
            <span class="smallNote">Use pre-configured AWS account access keys</span>
            <br/>
            <props:radioButtonProperty name="${credentials_type_param}" value="${temp_credentials_option}" id="${temp_credentials_option}" onclick="codeDeployUpdateVisibility()"/>
            <label for="${temp_credentials_option}">${temp_credentials_label}</label>
            <span class="smallNote">Get temporary access keys via AWS STS</span>
            <span class="error" id="error_${credentials_type_param}"></span>
            <br/>
            <a href="http://console.aws.amazon.com/iam" target="_blank">Open IAM Console</a>
        </td>
        <td></td>
    </tr>
    <tr id="${iam_role_arn_param}_row">
        <th><label for="${iam_role_arn_param}">${iam_role_arn_label}: <l:star/></label></th>
        <td><props:textProperty name="${iam_role_arn_param}" className="longField" maxlength="256"/>
        <span class="smallNote">Pre-configured IAM role with necessary permissions</span><span class="error" id="error_${iam_role_arn_param}"></span>
        </td>
    </tr>
    <tr id="${external_id_param}_row">
        <th><label for="${external_id_param}">${external_id_label}: </label></th>
        <td><props:textProperty name="${external_id_param}" className="longField" maxlength="256"/>
        <span class="smallNote">External ID is strongly recommended to be used in role trust relationship condition</span><span class="error" id="error_${external_id_param}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${use_default_cred_chain_param}">${use_default_cred_chain_label}: </label></th>
        <td><props:checkboxProperty name="${use_default_cred_chain_param}" onclick="codeDeployUpdateVisibility()"/></td>
    </tr>
    <tr id="${access_key_id_param}_row">
        <th><label for="${access_key_id_param}">${access_key_id_label}: <l:star/></label></th>
        <td><props:textProperty name="${access_key_id_param}" className="longField" maxlength="256"/>
        <span class="smallNote">AWS account access key ID</span><span class="error" id="error_${access_key_id_param}"></span>
        </td>
    </tr>
    <tr id="${secret_access_key_param}_row">
        <th><label for="${secret_access_key_param}">${secret_access_key_label}: <l:star/></label></th>
        <td><props:passwordProperty name="${secret_access_key_param}" className="longField" maxlength="256"/>
            <span class="smallNote">AWS account secret access key</span><span class="error" id="error_${secret_access_key_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

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
        <td><props:textProperty name="${bucket_name_param}" className="longField" maxlength="256"/><a href="http://console.aws.amazon.com/s3" target="_blank">Open S3 Console</a>
            <span class="smallNote">Existing S3 bucket name</span><span class="error" id="error_${bucket_name_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${s3_object_key_param}">${s3_object_key_label}: </label></th>
        <td><props:textProperty name="${s3_object_key_param}" className="longField" maxlength="256"/>
            <span class="smallNote">Leave empty to use application revision archive name as a key</span><span class="error" id="error_${s3_object_key_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<tr class="groupingTitle" data-steps="${register_deploy_steps}">
    <td colspan="2">CodeDeploy Application</td>
</tr>
<tr data-steps="${register_deploy_steps}">
    <th><label for="${app_name_param}">${app_name_label}: <l:star/></label></th>
    <td><props:textProperty name="${app_name_param}" className="longField" maxlength="256"/><a href="http://console.aws.amazon.com/codedeploy" target="_blank">Open CodeDeploy Console</a>
        <span class="smallNote">Pre-configured CodeDeploy application name</span><span class="error" id="error_${app_name_param}"></span>
    </td>
</tr>
<tr data-steps="${deploy_step}">
    <th><label for="${dep_group_name_param}">${dep_group_name_label}: <l:star/></label></th>
    <td><props:textProperty name="${dep_group_name_param}" className="longField" maxlength="256"/>
        <span class="smallNote">Pre-configured EC2 instances, must be running for deployment to succeed</span><span class="error" id="error_${dep_group_name_param}"></span>
    </td>
</tr>
<tr data-steps="${deploy_step}">
    <th><label for="${dep_config_name_param}">${dep_config_name_label}: </label></th>
    <td><props:textProperty name="${dep_config_name_param}" className="longField" maxlength="256"/>
        <span class="smallNote">e.g. "CodeDeployDefault.OneAtATime", "CodeDeployDefault.AllAtOnce" or a custom one, leave blank for default configuration</span><span class="error" id="error_${dep_config_name_param}"></span>
    </td>
</tr>
<tr data-steps="${deploy_step}">
    <th><label for="${wait_flag_param}">${wait_flag_label}: </label></th>
    <td><props:checkboxProperty name="${wait_flag_param}" onclick="codeDeployUpdateVisibility()"/></td>
</tr>
<tr id="${wait_timeout_param}_row" data-steps="${deploy_step}">
    <th><label for="${wait_timeout_param}">${wait_timeout_label}: <l:star/></label></th>
    <td><props:textProperty name="${wait_timeout_param}" maxlength="256"/>
        <span class="smallNote">Build will fail if timeout is exceeded</span><span class="error" id="error_${wait_timeout_param}"></span>
    </td>
</tr>

<script type="application/javascript">
    window.codeDeployUpdateStepNote = function () {
        $j('#runnerParams .stepNote').each(function() {
            BS.Util.hide(this);
        });
        BS.Util.show($j('#${deployment_steps_param} option:selected').attr('id') + '_note');
    };

    window.codeDeployUpdateVisibility = function () {
        if ($j('#${access_keys_option}').is(':checked')) {
            BS.Util.hide('${iam_role_arn_param}_row', '${external_id_param}_row');
        } else {
            BS.Util.show('${iam_role_arn_param}_row', '${external_id_param}_row');
        }

        if ($j('#${use_default_cred_chain_param}').is(':checked')) {
            BS.Util.hide('${access_key_id_param}_row', '${secret_access_key_param}_row');
        } else {
            BS.Util.show('${access_key_id_param}_row', '${secret_access_key_param}_row');
        }

        if ($j('#${wait_flag_param}').is(':checked')) {
            BS.Util.show('${wait_timeout_param}_row');
        } else {
            BS.Util.hide('${wait_timeout_param}_row');
        }

        // hide all and then show necessary
        $j('#runnerParams tr[data-steps]').each(function() {
            BS.Util.hide(this);
        });

        var deploySteps = $j('#${deployment_steps_param} option:selected').val();
        $j.each(deploySteps.split('${step_separator}'), function(i, val) {
            if (val) {
                $j("#runnerParams tr[data-steps*='" + val + "']").each(function() {
                    BS.Util.show(this);
                });
            }
        });

        BS.VisibilityHandlers.updateVisibility('runnerParams');
    };

    codeDeployUpdateStepNote();
    codeDeployUpdateVisibility();
</script>


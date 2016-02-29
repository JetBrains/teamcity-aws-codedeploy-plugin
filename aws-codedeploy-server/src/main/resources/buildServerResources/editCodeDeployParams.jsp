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
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<%@include file="paramsConstants.jspf"%>

<l:settingsGroup title="AWS Security Credentials">
    <tr>
        <th><label for="${credentials_type_param}">${credentials_type_label}: <l:star/></label></th>
        <td><props:radioButtonProperty name="${credentials_type_param}" value="${temp_credentials_option}" id="${temp_credentials_option}" onclick="codeDeployUpdateVisibility()"/>
            <label for="${temp_credentials_option}">${temp_credentials_label}</label>
            <span class="smallNote">Get temporary access keys via AWS STS</span>
            <br/>
            <props:radioButtonProperty name="${credentials_type_param}" value="${access_keys_option}" id="${access_keys_option}" onclick="codeDeployUpdateVisibility()"/>
            <label for="${access_keys_option}">${access_keys_label}</label>
            <span class="smallNote">Use pre-configured AWS account access keys</span>
            <span class="error" id="error_${credentials_type_param}"></span>
        </td>
    </tr>
    <tr id="${iam_role_arn_param}_row">
        <th><label for="${iam_role_arn_param}">${iam_role_arn_label}: <l:star/></label></th>
        <td><props:textProperty name="${iam_role_arn_param}" className="longField" maxlength="256"/>
        <span class="smallNote">Pre-configured IAM role with necessary permissions</span><span class="error" id="error_${iam_role_arn_param}"></span>
        </td>
    </tr>
    <tr id="${external_id_param}_row">
        <th><label for="${external_id_param}">${external_id_label}: </label></th>
        <td><props:textProperty name="${external_id_param}" className="longField" maxlength="256" disabled="true"/>
        <span class="smallNote">External ID to be used in role trust relationship condition</span><span class="error" id="error_${external_id_param}"></span>
        </td>
    </tr>
    <tr id="${access_key_id_param}_row">
        <th><label for="${access_key_id_param}">${access_key_id_label}: </label></th>
        <td><props:textProperty name="${access_key_id_param}" className="longField" maxlength="256"/>
        <span class="smallNote">AWS account access key ID, leave blank to use default credentials provider chain</span><span class="error" id="error_${access_key_id_param}"></span>
        </td>
    </tr>
    <tr id="${secret_access_key_param}_row">
        <th><label for="${secret_access_key_param}">${secret_access_key_label}: </label></th>
        <td><props:passwordProperty name="${secret_access_key_param}" className="longField" maxlength="256"/>
            <span class="smallNote">AWS account secret access key, leave blank to use default credentials provider chain</span><span class="error" id="error_${secret_access_key_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<tr>
    <th><label for="${region_name_param}">${region_name_label}: <l:star/></label></th>
    <td><props:textProperty name="${region_name_param}" maxlength="256"/>
        <span class="smallNote">e.g. "us-east-1", "eu-west-1"</span><span class="error" id="error_${region_name_param}" ?></span>
    </td>
</tr>

<l:settingsGroup title="Revision location">
    <tr>
        <th><label for="${revision_path_param}">${revision_path_label}: <l:star/></label></th>
        <td><props:textProperty name="${revision_path_param}" className="longField" maxlength="256"/>
            <span class="smallNote">Path to a valid application revision archive (including appspec.yml) that should be deployed</span><span class="error" id="error_${revision_path_param}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${bucket_name_param}">${bucket_name_label}: <l:star/></label></th>
        <td><props:textProperty name="${bucket_name_param}" className="longField" maxlength="256"/>
            <span class="smallNote">Existing S3 bucket name</span><span class="error" id="error_${bucket_name_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="CodeDeploy Application">
    <tr>
        <th><label for="${app_name_param}">${app_name_label}: <l:star/></label></th>
        <td><props:textProperty name="${app_name_param}" className="longField" maxlength="256"/>
            <span class="smallNote">Pre-configured CodeDeploy application name</span><span class="error" id="error_${app_name_param}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${dep_group_name_param}">${dep_group_name_label}: <l:star/></label></th>
        <td><props:textProperty name="${dep_group_name_param}" className="longField" maxlength="256"/>
            <span class="smallNote">Pre-configured EC2 instances, must be running for deployment to succeed</span><span class="error" id="error_${dep_group_name_param}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${dep_config_name_param}">${dep_config_name_label}: </label></th>
        <td><props:textProperty name="${dep_config_name_param}" className="longField" maxlength="256"/>
            <span class="smallNote">e.g. "CodeDeployDefault.OneAtATime", "CodeDeployDefault.AllAtOnce" or a custom one, leave blank for default configuration</span><span class="error" id="error_${dep_config_name_param}"></span>
        </td>
    </tr>
</l:settingsGroup>
<l:settingsGroup title="Deployment">
    <tr>
        <th><label for="${wait_flag_param}">${wait_flag_label}: </label></th>
        <td><props:checkboxProperty name="${wait_flag_param}" onclick="codeDeployUpdateVisibility()"/></td>
    </tr>
    <tr id="${wait_timeout_param}_row">
        <th><label for="${wait_timeout_param}">${wait_timeout_label}: <l:star/></label></th>
        <td><props:textProperty name="${wait_timeout_param}" maxlength="256"/>
            <span class="smallNote">Build will fail if timeout is exceeded</span><span class="error" id="error_${wait_timeout_param}"></span>
        </td>
    </tr>
    <tr id="${wait_poll_interval_param}_row">
        <th><label for="${wait_poll_interval_param}">${wait_poll_interval_label}: </label></th>
        <td><props:textProperty name="${wait_poll_interval_param}" maxlength="256"/>
            <span class="smallNote">Default value is ${wait_poll_interval_default} seconds</span><span class="error" id="error_${wait_poll_interval_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<script type="application/javascript">
    window.codeDeployUpdateVisibility = function () {
        if ($('${wait_flag_param}').checked) {
            BS.Util.show('${wait_timeout_param}_row', '${wait_poll_interval_param}_row');
        } else {
            BS.Util.hide('${wait_timeout_param}_row', '${wait_poll_interval_param}_row');
        }

        if ($('${access_keys_option}').checked) {
            BS.Util.hide('${iam_role_arn_param}_row', '${external_id_param}_row');
        } else {
            BS.Util.show('${iam_role_arn_param}_row', '${external_id_param}_row');
        }

        BS.VisibilityHandlers.updateVisibility($('runnerParams'))
    };
    codeDeployUpdateVisibility();
</script>


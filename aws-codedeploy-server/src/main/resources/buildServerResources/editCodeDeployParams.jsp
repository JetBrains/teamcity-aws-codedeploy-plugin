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

<tr>
    <th><label for="${revision_path_param}">${revision_path_label}: <l:star/></label></th>
    <td><props:textProperty name="${revision_path_param}" className="longField" maxlength="256"/>
        <span class="smallNote">Path to a valid application revision (including appspec.yml) that should be deployed</span><span class="error" id="error_${revision_path_param}"></span>
    </td>
</tr>

<tr>
    <th><label for="${region_name_param}">${region_name_label}: <l:star/></label></th>
    <td><props:textProperty name="${region_name_param}" maxlength="256"/>
        <span class="smallNote">e.g. "us-east-1", "eu-west-1"</span><span class="error" id="error_${region_name_param}" ?></span>
    </td>
</tr>

<l:settingsGroup title="AWS Security Credentials">
    <tr>
        <th><label for="${access_key_id_param}">${access_key_id_label}: <l:star/></label></th>
        <td><props:textProperty name="${access_key_id_param}" className="longField" maxlength="256"/>
            <span class="smallNote">AWS account access key id</span><span class="error" id="error_${access_key_id_param}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${secret_access_key_param}">${secret_access_key_label}: <l:star/></label></th>
        <td><props:passwordProperty name="${secret_access_key_param}" className="longField" maxlength="256"/>
            <span class="smallNote">AWS account secret access key</span><span class="error" id="error_${secret_access_key_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Revision Location">
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
        var wait = $('${wait_flag_param}');
        if (wait.checked) {
            BS.Util.show('${wait_timeout_param}_row', '${wait_poll_interval_param}_row');
        } else {
            BS.Util.hide('${wait_timeout_param}_row', '${wait_poll_interval_param}_row');
        }
        BS.VisibilityHandlers.updateVisibility($('runnerParams'))
    };
    codeDeployUpdateVisibility();
</script>


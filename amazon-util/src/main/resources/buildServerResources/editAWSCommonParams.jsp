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

<%@include file="constantsAWSCommonParams.jspf"%>

<tr>
    <th><label for="${region_name_param}">${region_name_label}: <l:star/></label></th>
    <td><props:selectProperty name="${region_name_param}" className="longField" enableFilter="true">
        <props:option value="${null}">-- Select region --</props:option>
        <c:forEach var="region" items="${allRegions.keySet()}">
            <props:option value="${region}"><c:out value="${allRegions[region]}"/></props:option>
        </c:forEach>
    </props:selectProperty>
        <span class="smallNote">All resources must be located in this region</span><span class="error" id="error_${region_name_param}"></span>
    </td>
</tr>

<l:settingsGroup title="AWS Security Credentials">
    <tr>
        <th><label for="${credentials_type_param}">${credentials_type_label}: <l:star/></label></th>
        <td><props:radioButtonProperty name="${credentials_type_param}" value="${access_keys_option}" id="${access_keys_option}" onclick="awsCommonParamsUpdateVisibility()"/>
            <label for="${access_keys_option}">${access_keys_label}</label>
            <span class="smallNote">Use pre-configured AWS account access keys</span>
            <br/>
            <props:radioButtonProperty name="${credentials_type_param}" value="${temp_credentials_option}" id="${temp_credentials_option}" onclick="awsCommonParamsUpdateVisibility()"/>
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
        <td><props:checkboxProperty name="${use_default_cred_chain_param}" onclick="awsCommonParamsUpdateVisibility()"/></td>
    </tr>
    <tr id="${access_key_id_param}_row">
        <th><label for="${access_key_id_param}">${access_key_id_label}: <l:star/></label></th>
        <td><props:textProperty name="${access_key_id_param}" className="longField" maxlength="256"/>
            <span class="smallNote">AWS account access key ID</span><span class="error" id="error_${access_key_id_param}"></span>
        </td>
    </tr>
    <tr id="${secret_access_key_param}_row">
        <th><label for="${secure_secret_access_key_param}">${secret_access_key_label}: <l:star/></label></th>
        <td><props:passwordProperty name="${secure_secret_access_key_param}" className="longField" maxlength="256"/>
            <span class="smallNote">AWS account secret access key</span><span class="error" id="error_${secure_secret_access_key_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<script type="application/javascript">
    window.awsCommonParamsUpdateVisibility = function () {
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
        BS.VisibilityHandlers.updateVisibility('runnerParams');
    };

    awsCommonParamsUpdateVisibility();
</script>


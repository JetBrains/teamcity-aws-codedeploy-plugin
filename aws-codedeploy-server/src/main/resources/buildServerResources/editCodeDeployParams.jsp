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

<%@ page import="jetbrains.buildServer.runner.codedeploy.CodeDeployConstants" %>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<c:set var="revision_path_param" value="<%=CodeDeployConstants.READY_REVISION_PATH_PARAM%>"/>
<c:set var="revision_path_label" value="<%=CodeDeployConstants.READY_REVISION_PATH_LABEL%>"/>

<c:set var="region_name_param" value="<%=CodeDeployConstants.REGION_NAME_PARAM%>"/>
<c:set var="region_name_label" value="<%=CodeDeployConstants.REGION_NAME_LABEL%>"/>

<c:set var="access_key_id_param" value="<%=CodeDeployConstants.ACCESS_KEY_ID_PARAM%>"/>
<c:set var="access_key_id_label" value="<%=CodeDeployConstants.ACCESS_KEY_ID_LABEL%>"/>

<c:set var="secret_access_key_param" value="<%=CodeDeployConstants.SECRET_ACCESS_KEY_PARAM%>"/>
<c:set var="secret_access_key_label" value="<%=CodeDeployConstants.SECRET_ACCESS_KEY_LABEL%>"/>

<c:set var="bucket_name_param" value="<%=CodeDeployConstants.S3_BUCKET_NAME_PARAM%>"/>
<c:set var="bucket_name_label" value="<%=CodeDeployConstants.S3_BUCKET_NAME_LABEL%>"/>

<c:set var="app_name_param" value="<%=CodeDeployConstants.APP_NAME_PARAM%>"/>
<c:set var="app_name_label" value="<%=CodeDeployConstants.APP_NAME_LABEL%>"/>

<c:set var="dep_group_name_param" value="<%=CodeDeployConstants.DEPLOYMENT_GROUP_NAME_PARAM%>"/>
<c:set var="dep_group_name_label" value="<%=CodeDeployConstants.DEPLOYMENT_GROUP_NAME_LABEL%>"/>

<c:set var="dep_config_name_param" value="<%=CodeDeployConstants.DEPLOYMENT_CONFIG_NAME_PARAM%>"/>
<c:set var="dep_config_name_label" value="<%=CodeDeployConstants.DEPLOYMENT_CONFIG_NAME_LABEL%>"/>

<tr>
    <th><label for="${revision_path_param}">${revision_path_label}: <l:star/></label></th>
    <td><props:textProperty name="${revision_path_param}" className="longField" maxlength="256"/>
        <span class="smallNote">Path to a valid application revision including appspec.yml that should be deployed</span><span class="error" id="error_${revision_path_param}"></span>
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
        <th><label for="${region_name_param}">${region_name_label}: <l:star/></label></th>
        <td><props:textProperty name="${region_name_param}" maxlength="256"/>
            <span class="smallNote">e.g. "us-east-1", "eu-west-1"</span><span class="error" id="error_${region_name_param}" ?></span>
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
            <span class="smallNote">e.g. "CodeDeployDefault.OneAtATime", "CodeDeployDefault.AllAtOnce" or a custom one. Leave blank for a default configuration.</span><span class="error" id="error_${dep_config_name_param}"></span>
        </td>
    </tr>
</l:settingsGroup>


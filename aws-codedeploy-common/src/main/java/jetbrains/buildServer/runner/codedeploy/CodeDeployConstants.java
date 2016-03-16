/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author vbedrosova
 */
public interface CodeDeployConstants {
  String RUNNER_TYPE = "codedeploy";
  String RUNNER_DISPLAY_NAME = "AWS CodeDeploy";
  String RUNNER_DESCR = "Prepare, upload, register and deploy application revision using CodeDeploy";


  String DEPLOYMENT_ID_BUILD_CONFIG_PARAM = "codedeploy.deploment.id";
  String S3_OBJECT_VERSION_CONFIG_PARAM = "codedeploy.revision.s3.version";
  String CUSTOM_APPSPEC_YML_CONFIG_PARAM = "codedeploy.custom.appspec.yml";


  String EDIT_PARAMS_HTML = "editCodeDeployParams.html";
  String VIEW_PARAMS_HTML = "viewCodeDeployParams.html";
  String EDIT_PARAMS_JSP = "editCodeDeployParams.jsp";
  String VIEW_PARAMS_JSP = "viewCodeDeployParams.jsp";

  String ALL_REGIONS = "allRegions";
  String DEPLOYMENT_SCENARIOS = "deploymentScenarios";


  String TIMEOUT_BUILD_PROBLEM_TYPE = "CODEDEPLOY_TIMEOUT";
  String FAILURE_BUILD_PROBLEM_TYPE = "CODEDEPLOY_FAILURE";
  String EXCEPTION_BUILD_PROBLEM_TYPE = "CODEDEPLOY_EXCEPTION";
  String SERVICE_PROBLEM_TYPE = "CODEDEPLOY_SERVICE";
  String CLIENT_PROBLEM_TYPE = "CODEDEPLOY_CLIENT";



  String DEPLOYMENT_STEPS_PARAM = "codedeploy_deployment_steps";
  String DEPLOYMENT_STEPS_LABEL = "Deployment steps";

  String CREDENTIALS_TYPE_PARAM = "codedeploy_credentials_type";
  String CREDENTIALS_TYPE_LABEL = "Credentials type";
  String TEMP_CREDENTIALS_OPTION = "codedeploy_temp_credentials";
  String TEMP_CREDENTIALS_LABEL = "Temporary credentials";
  String ACCESS_KEYS_OPTION = "codedeploy_access_keys";
  String ACCESS_KEYS_LABEL = "Access keys";

  String USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM = "codedeploy_use_default_credential_provider_chain";
  String USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_LABEL = "Use default credential provider chain";

  String ACCESS_KEY_ID_PARAM = "codedeploy_access_key_id";
  String ACCESS_KEY_ID_LABEL = "Access key ID";
  String SECRET_ACCESS_KEY_PARAM = "codedeploy_secret_access_key";
  String SECRET_ACCESS_KEY_LABEL = "Secret access key";

  String IAM_ROLE_ARN_PARAM = "codedeploy_iam_role_arn";
  String IAM_ROLE_ARN_LABEL = "IAM role ARN";
  String EXTERNAL_ID_PARAM = "codedeploy_external_id";
  String EXTERNAL_ID_LABEL = "External ID";
  int TEMP_CREDENTIALS_DURATION_SEC_DEFAULT = 1800;

  String REVISION_PATHS_PARAM = "codedeploy_revision_paths";
  String READY_REVISION_PATH_LABEL = "Application revision";

  String S3_BUCKET_NAME_PARAM = "codedeploy_s3_bucket_name";
  String S3_BUCKET_NAME_LABEL = "S3 bucket";

  String S3_OBJECT_KEY_PARAM = "codedeploy_s3_object_key";
  String S3_OBJECT_KEY_LABEL = "S3 object key";

  String APP_NAME_PARAM = "codedeploy_application_name";
  String APP_NAME_LABEL = "Application name";
  String REGION_NAME_PARAM = "codedeploy_region_name";
  String REGION_NAME_LABEL = "AWS region";

  String DEPLOYMENT_GROUP_NAME_PARAM = "codedeploy_deployment_group_name";
  String DEPLOYMENT_GROUP_NAME_LABEL = "Deployment group";
  String DEPLOYMENT_CONFIG_NAME_PARAM = "codedeploy_deployment_config_name";
  String DEPLOYMENT_CONFIG_NAME_LABEL = "Deployment configuration";

  String WAIT_FLAG_PARAM = "codedeploy_wait";
  String WAIT_FLAG_LABEL = "Wait for deployment finish";
  String WAIT_TIMEOUT_SEC_PARAM = "codedeploy_wait_timeout_sec";
  String WAIT_TIMEOUT_SEC_LABEL = "Timeout (seconds)";
  String WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM = "codedeploy.wait.poll.interval.sec";
  String WAIT_POLL_INTERVAL_SEC_DEFAULT = "20";

  String UPLOAD_STEP = "s3uploadstep";
  String REGISTER_STEP = "registerstep";
  String DEPLOY_STEP = "deploystep";
  String STEP_SEPARATOR = "_";

  String UPLOAD_REGISTER_DEPLOY_STEPS = UPLOAD_STEP + STEP_SEPARATOR + REGISTER_STEP + STEP_SEPARATOR + DEPLOY_STEP;
  String REGISTER_DEPLOY_STEPS = REGISTER_STEP + STEP_SEPARATOR + DEPLOY_STEP;
  String UPLOAD_REGISTER_STEPS = UPLOAD_STEP + STEP_SEPARATOR + REGISTER_STEP;

  Map<String, String> STEP_LABELS = Collections.unmodifiableMap(CollectionsUtil.asMap(
    UPLOAD_REGISTER_DEPLOY_STEPS, "Upload, register and deploy",
    REGISTER_DEPLOY_STEPS, "Register and deploy",
    UPLOAD_REGISTER_STEPS, "Upload and register",
    UPLOAD_STEP, "Upload",
    REGISTER_STEP, "Register",
    DEPLOY_STEP, "Deploy"
  ));

  Map<String, String> DEFAULTS = Collections.unmodifiableMap(CollectionsUtil.asMap(
    CREDENTIALS_TYPE_PARAM, ACCESS_KEYS_OPTION,
    EXTERNAL_ID_PARAM, UUID.randomUUID().toString(), // see jetbrains.buildServer.runner.codedeploy.CodeDeployRunType#getDefaultRunnerProperties
    WAIT_FLAG_PARAM, "true",
    USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM, "false",
    DEPLOYMENT_STEPS_PARAM, UPLOAD_REGISTER_DEPLOY_STEPS
  ));

  String STATUS_IS_UNKNOWN = "status is unknown";

  String SPLIT_REGEX = " *[,\n\r] *";
  String APPSPEC_YML = "appspec.yml";
}

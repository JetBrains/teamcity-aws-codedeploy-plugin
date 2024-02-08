

package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.util.CollectionsUtil;

import java.util.Collections;
import java.util.Map;

/**
 * @author vbedrosova
 */
public interface CodeDeployConstants {
  String RUNNER_TYPE = "aws.codeDeploy";
  String RUNNER_DISPLAY_NAME = "AWS CodeDeploy";
  String RUNNER_DESCR = "Prepare, upload, register and deploy application revision using AWS CodeDeploy";

  String DEPLOYMENT_ID_BUILD_CONFIG_PARAM = "codedeploy.deployment.id";
  String S3_OBJECT_VERSION_CONFIG_PARAM = "codedeploy.revision.s3.version";
  String S3_OBJECT_ETAG_CONFIG_PARAM = "codedeploy.revision.s3.etag";
  String CUSTOM_APPSPEC_YML_CONFIG_PARAM = "codedeploy.custom.appspec.yml";


  String EDIT_PARAMS_HTML = "editCodeDeployParams.html";
  String VIEW_PARAMS_HTML = "viewCodeDeployParams.html";
  String EDIT_PARAMS_JSP = "editCodeDeployParams.jsp";
  String VIEW_PARAMS_JSP = "viewCodeDeployParams.jsp";

  String DEPLOYMENT_SCENARIOS = "deploymentScenarios";


  String TIMEOUT_BUILD_PROBLEM_TYPE = "CODEDEPLOY_TIMEOUT";
  String FAILURE_BUILD_PROBLEM_TYPE = "CODEDEPLOY_FAILURE";


  String DEPLOYMENT_STEPS_PARAM_OLD = "codedeploy_deployment_steps";
  String DEPLOYMENT_STEPS_PARAM = "codedeploy.deployment.steps";
  String DEPLOYMENT_STEPS_LABEL = "Deployment steps";

  String REVISION_PATHS_PARAM_OLD= "codedeploy_revision_paths";
  String REVISION_PATHS_PARAM = "codedeploy.revision.paths";
  String REVISION_PATHS_LABEL = "Application revision";

  String S3_BUCKET_NAME_PARAM_OLD = "codedeploy_s3_bucket_name";
  String S3_BUCKET_NAME_PARAM = "codedeploy.s3.bucket.name";
  String S3_BUCKET_NAME_LABEL = "S3 bucket";
  String REVISION_PATHS_NOTE = "Ant-style wildcards as well as target directories like out/**/*.zip => dist supported";

  String S3_OBJECT_KEY_PARAM_OLD = "codedeploy_s3_object_key";
  String S3_OBJECT_KEY_PARAM = "codedeploy.s3.object.key";
  String S3_OBJECT_KEY_LABEL = "S3 object key";

  String APP_NAME_PARAM_OLD = "codedeploy_application_name";
  String APP_NAME_PARAM = "codedeploy.application.name";
  String APP_NAME_LABEL = "Application name";

  String FILE_EXISTS_BEHAVIOR_PARAM = "codedeploy.file.exists.behavior";
  String FILE_EXISTS_BEHAVIOR_LABEL = "Behavior when file exists";

  String DEPLOYMENT_GROUP_NAME_PARAM_OLD = "codedeploy_deployment_group_name";
  String DEPLOYMENT_GROUP_NAME_PARAM = "codedeploy.deployment.group.name";
  String DEPLOYMENT_GROUP_NAME_LABEL = "Deployment group";
  String DEPLOYMENT_CONFIG_NAME_PARAM_OLD = "codedeploy_deployment_config_name";
  String DEPLOYMENT_CONFIG_NAME_PARAM = "codedeploy.deployment.config.name";
  String DEPLOYMENT_CONFIG_NAME_LABEL = "Deployment configuration";

  String ROLLBACK_ON_FAILURE_PARAM_OLD = "codedeploy_rollback_on_failure";
  String ROLLBACK_ON_FAILURE_PARAM = "codedeploy.rollback.on.failure";
  String ROLLBACK_ON_FAILURE_LABEL = "Roll back when a deployment fails";
  String ROLLBACK_ON_ALARM_THRESHOLD_PARAM_OLD = "codedeploy_rollback_on_alarm_threshold";
  String ROLLBACK_ON_ALARM_THRESHOLD_PARAM = "codedeploy.rollback.on.alarm.threshold";
  String ROLLBACK_ON_ALARM_THRESHOLD_LABEL = "Roll back when alarm thresholds are met";

  String GREEN_FLEET_PARAM_OLD = "codedeploy_green_fleet";
  String GREEN_FLEET_PARAM = "codedeploy.green.fleet";
  String GREEN_FLEET_LABEL = "Green fleet (replacement environment instances)";

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
    DEPLOYMENT_STEPS_PARAM, UPLOAD_REGISTER_DEPLOY_STEPS
  ));

  String STATUS_IS_UNKNOWN = "status is unknown";

  String MULTILINE_SPLIT_REGEX = " *[,\n\r] *";
  String PATH_SPLIT_REGEX = " *=> *";
  String APPSPEC_YML = "appspec.yml";
}
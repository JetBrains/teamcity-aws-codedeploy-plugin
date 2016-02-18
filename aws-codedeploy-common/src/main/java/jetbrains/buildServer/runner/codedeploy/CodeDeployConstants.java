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

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

/**
 * @author vbedrosova
 */
public interface CodeDeployConstants {
  String RUNNER_TYPE = "codedeploy";
  String RUNNER_DISPLAY_NAME = "AWS CodeDeploy";
  String RUNNER_DESCR = "Prepare, upload, register and deploy application revision to EC2 using CodeDeploy";



  String EDIT_PARAMS_JSP = "editCodeDeployParams.jsp";
  String VIEW_PARAMS_JSP = "viewCodeDeployParams.jsp";



  String ACCESS_KEY_ID_PARAM = "codedeploy.access.key.id";
  String ACCESS_KEY_ID_LABEL = "Access key ID";
  String SECRET_ACCESS_KEY_PARAM = "codedeploy.secret.access.key";
  String SECRET_ACCESS_KEY_LABEL = "Secret access key";

  String READY_REVISION_PATH_PARAM = "codedeploy.ready.revision.path";
  String READY_REVISION_PATH_LABEL = "Application revision";

  String S3_BUCKET_NAME_PARAM = "codedeploy.s3.bucket.name";
  String S3_BUCKET_NAME_LABEL = "S3 bucket";

  String APP_NAME_PARAM = "codedeploy.application.name";
  String APP_NAME_LABEL = "Application name";
  String REGION_NAME_PARAM = "codedeploy.region.name";
  String REGION_NAME_LABEL = "AWS region";

  String DEPLOYMENT_GROUP_NAME_PARAM = "codedeploy.deployment.group.name";
  String DEPLOYMENT_GROUP_NAME_LABEL = "Deployment group";
  String DEPLOYMENT_CONFIG_NAME_PARAM = "codedeploy.deployment.config.name";
  String DEPLOYMENT_CONFIG_NAME_LABEL = "Deployment configuration";

  String WAIT_FLAG_PARAM = "codedeploy.wait";
  String WAIT_TIMEOUT_SEC_PARAM = "codedeploy.wait.timeout.sec";
  String WAIT_INTERVAL_SEC_PARAM = "codedeploy.wait.interval.sec";

}

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

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author vbedrosova
 */
public class CodeDeployRunner implements AgentBuildRunner {

  public static final String DEPLOY_APPLICATION = "deploy application";
  public static final String REGISTER_REVISION = "register revision";
  public static final String UPLOAD_REVISION = "upload revision";

  @NotNull
  @Override
  public BuildProcess createBuildProcess(@NotNull final AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {
    return new SyncBuildProcessAdapter(context.getBuild().getBuildLogger()) {
      @Override
      protected void runProcess() throws RunBuildException {
        validateParams();

        final Map<String, String> runnerParameters = context.getRunnerParameters();

        final AWSClient awsClient = createAWSClient(runnerParameters, runningBuild);

        final File revision = FileUtil.resolvePath(runningBuild.getCheckoutDirectory(), runnerParameters.get(CodeDeployConstants.READY_REVISION_PATH_PARAM));

        final String s3BucketName = runnerParameters.get(CodeDeployConstants.S3_BUCKET_NAME_PARAM);
        final String applicationName = runnerParameters.get(CodeDeployConstants.APP_NAME_PARAM);
        final String deploymentGroupName = runnerParameters.get(CodeDeployConstants.DEPLOYMENT_GROUP_NAME_PARAM);
        final String deploymentConfigName = StringUtil.nullIfEmpty(runnerParameters.get(CodeDeployConstants.DEPLOYMENT_CONFIG_NAME_PARAM));

        if (Boolean.parseBoolean(runnerParameters.get(CodeDeployConstants.WAIT_FLAG_PARAM))) {
          awsClient.uploadRegisterDeployRevisionAndWait(
            revision,
            s3BucketName, applicationName,
            deploymentGroupName, deploymentConfigName,
            Integer.parseInt(runnerParameters.get(CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM)),
            Integer.getInteger(runnerParameters.get(CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_PARAM), CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_DEFAULT));
        } else {
          awsClient.uploadRegisterAndDeployRevision(revision, s3BucketName, applicationName, deploymentGroupName, deploymentConfigName);
        }
      }

      private void validateParams() throws RunBuildException {
        final Map<String, String> invalids = ParametersValidator.validateRuntime(context.getRunnerParameters(), runningBuild.getCheckoutDirectory());
        if (invalids.isEmpty()) return;
        throw new RunBuildException(invalids.values().iterator().next(), null, ErrorData.BUILD_RUNNER_ERROR_TYPE);
      }
    };
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new AgentBuildRunnerInfo() {
      @NotNull
      @Override
      public String getType() {
        return CodeDeployConstants.RUNNER_TYPE;
      }

      @Override
      public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
        return true;
      }
    };
  }

  @NotNull
  private AWSClient createAWSClient(final Map<String, String> runnerParameters, @NotNull final AgentRunningBuild runningBuild) {
    final BuildProgressLogger buildLogger = runningBuild.getBuildLogger();
    final String regionName = runnerParameters.get(CodeDeployConstants.REGION_NAME_PARAM);

    return (CodeDeployConstants.ACCESS_KEYS_OPTION.equals(runnerParameters.get(CodeDeployConstants.CREDENTIALS_TYPE_PARAM))
      ?
      new AWSClient(
        runnerParameters.get(CodeDeployConstants.ACCESS_KEY_ID_PARAM),
        runnerParameters.get(CodeDeployConstants.SECRET_ACCESS_KEY_PARAM),
        regionName
      ) :
      new AWSClient(
        runnerParameters.get(CodeDeployConstants.IAM_ROLE_ARN_PARAM),
        runnerParameters.get(CodeDeployConstants.EXTERNAL_ID_PARAM),
        runnerParameters.get(CodeDeployConstants.ACCESS_KEY_ID_PARAM),
        runnerParameters.get(CodeDeployConstants.SECRET_ACCESS_KEY_PARAM),
        runningBuild.getBuildTypeName(),
        2 * Integer.getInteger(runnerParameters.get(CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM), CodeDeployConstants.TEMP_CREDENTIALS_DURATION_SEC_DEFAULT),
        regionName
      ))
      .withDescription("TeamCity build " + runningBuild.getBuildTypeName() + " #" + runningBuild.getBuildNumber())
      .withListener(new AWSClient.Listener() {
        @Override
        void uploadRevisionStarted(@NotNull File revision, @NotNull String s3BucketName, @NotNull String key) {
          open(UPLOAD_REVISION);
          log(String.format("Uploading application revision %s to S3 bucket %s using key %s", revision.getPath(), s3BucketName, key));
        }

        @Override
        void uploadRevisionFinished(@NotNull File revision, @NotNull String s3BucketName, @NotNull String key, @NotNull String url) {
          log("Uploaded application revision S3 URL : " + url);
          close(UPLOAD_REVISION);
        }

        @Override
        void registerRevisionStarted(@NotNull String applicationName, @NotNull String s3BucketName, @NotNull String key, @NotNull String bundleType) {
          open(REGISTER_REVISION);
          log(String.format("Registering application %s revision from S3 bucket %s with key %s and bundle type %s", applicationName, s3BucketName, key, bundleType));
        }

        @Override
        void registerRevisionFinished(@NotNull String applicationName, @NotNull String s3BucketName, @NotNull String key, @NotNull String bundleType) {
          close(REGISTER_REVISION);
        }

        @Override
        void createDeploymentStarted(@NotNull String applicationName, @NotNull String deploymentGroupName, @Nullable String deploymentConfigName) {
          open(DEPLOY_APPLICATION);
          log(String.format("Creating application %s deployment to deployment group %s with %s deployment configuration", applicationName, deploymentGroupName, StringUtil.isEmptyOrSpaces(deploymentConfigName) ? "default" : deploymentConfigName));
        }

        @Override
        void createDeploymentFinished(@NotNull String applicationName, @NotNull String deploymentGroupName, @Nullable String deploymentConfigName, @NotNull String deploymentId) {
          parameter(CodeDeployConstants.DEPLOYMENT_ID_BUILD_ENV_VAR, deploymentId);
          log("Deployment " + deploymentId + " created");
        }

        @Override
        void deploymentWaitStarted(@NotNull String deploymentId) {
          log("Waiting for deployment finish");
        }


        @Override
        void deploymentInProgress(@NotNull String deploymentId, @Nullable InstancesStatus instancesStatus) {
          debug("Deployment " + deploymentDescription(instancesStatus, true));
        }

        @Override
        void deploymentFailed(@NotNull String deploymentId, @Nullable Integer timeoutSec, @Nullable ErrorInfo errorInfo, @Nullable InstancesStatus instancesStatus) {
          String msg = "Deployment" + (timeoutSec == null ? " " : " timeout " + timeoutSec + "sec exceeded, ");

          err(msg + deploymentDescription(instancesStatus, true));
          msg += deploymentDescription(instancesStatus, false);

          if (errorInfo != null) {
            if (StringUtil.isNotEmpty(errorInfo.message)) {
              err("Associated error: " + errorInfo.message);
              msg += ": " + errorInfo.message;
            }
            if (StringUtil.isNotEmpty(errorInfo.code)) {
              err("Error code: " + errorInfo.code);
            }
          }

          problem(getIdentity(timeoutSec, errorInfo, instancesStatus), timeoutSec == null? CodeDeployConstants.FAILURE_BUILD_PROBLEM_TYPE : CodeDeployConstants.TIMEOUT_BUILD_PROBLEM_TYPE, msg);

          close(DEPLOY_APPLICATION);
        }

        @Override
        void deploymentSucceeded(@NotNull String deploymentId, @Nullable InstancesStatus instancesStatus) {
          final String msg = "Deployment " + deploymentId + " ";

          log(msg + deploymentDescription(instancesStatus, true));
          statusText(msg + deploymentDescription(instancesStatus, false));

          close(DEPLOY_APPLICATION);
        }

        @Override
        void exception(@NotNull String message, @Nullable String details, @Nullable String type, @Nullable String identity) {
          err(message);
          if (StringUtil.isNotEmpty(details)) err(details);
          problem(getIdentity(message, identity), type == null ? CodeDeployConstants.EXCEPTION_BUILD_PROBLEM_TYPE : type, message);
          close(DEPLOY_APPLICATION);
        }

        private int getIdentity(@Nullable Integer timeoutSec, @Nullable ErrorInfo errorInfo, @Nullable InstancesStatus instancesStatus) {
          return getIdentity(
            timeoutSec == null ? null : timeoutSec.toString(),
            errorInfo == null ? null : errorInfo.code,
            instancesStatus == null ? null : instancesStatus.status
          );
        }

        @NotNull
        private String deploymentDescription(@Nullable InstancesStatus instancesStatus, boolean detailed) {
          final StringBuilder sb = new StringBuilder("status is");

          if (instancesStatus == null) sb.append(" unknown");
          else {
            sb.append(" ").append(StringUtil.isEmptyOrSpaces(instancesStatus.status) ? "unknown" : instancesStatus.status.toLowerCase());
            sb.append(", instances succeeded: ").append(instancesStatus.succeeded);
            if (instancesStatus.failed > 0 || detailed) sb.append(", failed: ").append(instancesStatus.failed);
            if (instancesStatus.pending > 0 || detailed) sb.append(", pending: ").append(instancesStatus.pending);
            if (instancesStatus.skipped > 0 || detailed) sb.append(", skipped: ").append(instancesStatus.skipped);
            if (instancesStatus.inProgress > 0 || detailed) sb.append(", in progress: ").append(instancesStatus.inProgress);
          }

          return sb.toString();
        }

        private int getIdentity(String... parts) {
          final String baseDir = runningBuild.getCheckoutDirectory().getAbsolutePath().replace("\\", "/");
          final StringBuilder sb = new StringBuilder();

          List<String> allParts = new ArrayList<String>(CollectionsUtil.join(Arrays.asList(parts), getIdentityFormingParameters()));
          allParts = CollectionsUtil.filterNulls(allParts);
          Collections.sort(allParts);

          for (String p : allParts) {
            if (StringUtil.isEmptyOrSpaces(p)) continue;

            p = p.replace("\\", "/");
            p = p.replace(baseDir, "");
            sb.append(p);
          }

          return sb.toString().replace(" ", "").toLowerCase().hashCode();
        }

        @NotNull
        private Collection<String> getIdentityFormingParameters() {
          final List<String> key = Arrays.asList(
            CodeDeployConstants.S3_BUCKET_NAME_PARAM,
            CodeDeployConstants.APP_NAME_PARAM,
            CodeDeployConstants.DEPLOYMENT_GROUP_NAME_PARAM,
            CodeDeployConstants.REGION_NAME_PARAM);
          return CollectionsUtil.filterMapByKeys(runnerParameters, new Filter<String>() {
            @Override
            public boolean accept(@NotNull String data) {
              return key.contains(data);
            }
          }).values();
        }

        private void log(@NotNull String message) {
          buildLogger.message(message);
        }

        private void open(@NotNull String block) {
          buildLogger.targetStarted(block);
        }

        private void close(@NotNull String block) {
          buildLogger.targetFinished(block);
        }

        private void err(@NotNull String message) {
          buildLogger.error(message);
        }

        private void debug(@NotNull String message) {
          buildLogger.message(String.format("##teamcity[message text='%s' tc:tags='tc:internal']", escape(message)));
        }

        private void problem(int identity, @NotNull String type, @NotNull String descr) {
          buildLogger.message(String.format("##teamcity[buildProblem identity='%d' type='%s' description='%s' tc:tags='tc:internal']", identity, type, escape(descr)));
        }

        private void parameter(@NotNull String name, @NotNull String value) {
          buildLogger.message(String.format("##teamcity[setParameter name='%s' value='%s' tc:tags='tc:internal']", name, value));
        }

        private void statusText(@NotNull String text) {
          buildLogger.message(String.format("##teamcity[buildStatus tc:tags='tc:internal' text='{build.status.text}; %s']", text));
        }

        @NotNull
        private String escape(@NotNull String s) {
          return s.
            replace("|", "||").
            replace("'", "|'").
            replace("\n", "|n").
            replace("\r", "|r").
            replace("\\uNNNN", "|0xNNNN").
            replace("[", "|[").replace("]", "|]");
        }
      });
  }
}

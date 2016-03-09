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
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author vbedrosova
 */
public class CodeDeployRunner implements AgentBuildRunner {
  @NotNull
  @Override
  public BuildProcess createBuildProcess(@NotNull final AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {
    return new SyncBuildProcessAdapter() {
      @NotNull
      @Override
      protected BuildFinishedStatus runImpl() throws RunBuildException {

        final Map<String, String> runnerParameters = validateParams();
        final AtomicBoolean problemOccurred = new AtomicBoolean();

        final AWSClient awsClient = createAWSClient(runnerParameters, runningBuild).withListener(
          new LoggingDeploymentListener(runnerParameters, runningBuild.getBuildLogger(), runningBuild.getCheckoutDirectory().getAbsolutePath()) {
            @Override
            protected void problem(int identity, @NotNull String type, @NotNull String descr) {
              super.problem(identity, type, descr);
              problemOccurred.set(true);
            }
          });

        final File revision = FileUtil.resolvePath(runningBuild.getCheckoutDirectory(), runnerParameters.get(CodeDeployConstants.READY_REVISION_PATH_PARAM));

        final String s3BucketName = runnerParameters.get(CodeDeployConstants.S3_BUCKET_NAME_PARAM);
        final String s3ObjectKey =
          runnerParameters.containsKey(CodeDeployConstants.S3_OBJECT_KEY_PARAM) ? runnerParameters.get(CodeDeployConstants.S3_OBJECT_KEY_PARAM) : revision.getName();

        final String applicationName = runnerParameters.get(CodeDeployConstants.APP_NAME_PARAM);
        final String deploymentGroupName = runnerParameters.get(CodeDeployConstants.DEPLOYMENT_GROUP_NAME_PARAM);
        final String deploymentConfigName = StringUtil.nullIfEmpty(runnerParameters.get(CodeDeployConstants.DEPLOYMENT_CONFIG_NAME_PARAM));

        final String waitParam = runnerParameters.get(CodeDeployConstants.WAIT_FLAG_PARAM);
        if (StringUtil.isEmptyOrSpaces(waitParam) || Boolean.parseBoolean(waitParam)) {
          awsClient.uploadRegisterDeployRevisionAndWait(
            revision,
            s3BucketName, s3ObjectKey,
            applicationName, deploymentGroupName, deploymentConfigName,
            Integer.parseInt(runnerParameters.get(CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM)),
            getIntegerOrDefault(context.getConfigParameters().get(CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM), CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_DEFAULT));
        } else {
          awsClient.uploadRegisterAndDeployRevision(revision, s3BucketName, s3ObjectKey, applicationName, deploymentGroupName, deploymentConfigName);
        }

        return problemOccurred.get() ? BuildFinishedStatus.FINISHED_WITH_PROBLEMS : BuildFinishedStatus.FINISHED_SUCCESS;
      }

      @NotNull
      private Map<String, String> validateParams() throws RunBuildException {
        final Map<String, String> runnerParameters = context.getRunnerParameters();
        final Map<String, String> invalids = ParametersValidator.validateRuntime(runnerParameters, context.getConfigParameters(), runningBuild.getCheckoutDirectory());
        if (invalids.isEmpty()) return runnerParameters;

        final RunBuildException runBuildException = new RunBuildException(invalids.values().iterator().next(), null, ErrorData.BUILD_RUNNER_ERROR_TYPE);
        runBuildException.setLogStacktrace(false);
        throw runBuildException;
      }

      @NotNull
      private Integer getIntegerOrDefault(@Nullable String parameter, @NotNull Integer defaultVal) {
        return StringUtil.isEmptyOrSpaces(parameter) ? defaultVal : Integer.parseInt(parameter);
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
    final String regionName = runnerParameters.get(CodeDeployConstants.REGION_NAME_PARAM);

    final boolean useDefaultCredProvChain = Boolean.parseBoolean(runnerParameters.get(CodeDeployConstants.USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM));
    final String accessKeyId = useDefaultCredProvChain ? null : runnerParameters.get(CodeDeployConstants.ACCESS_KEY_ID_PARAM);
    final String secretAccessKey = useDefaultCredProvChain ? null : runnerParameters.get(CodeDeployConstants.SECRET_ACCESS_KEY_PARAM);

    return (CodeDeployConstants.TEMP_CREDENTIALS_OPTION.equals(runnerParameters.get(CodeDeployConstants.CREDENTIALS_TYPE_PARAM)) ?
      new AWSClient(
        runnerParameters.get(CodeDeployConstants.IAM_ROLE_ARN_PARAM),
        runnerParameters.get(CodeDeployConstants.EXTERNAL_ID_PARAM),
        accessKeyId,
        secretAccessKey,
        runningBuild.getBuildTypeName(),
        2 * Integer.getInteger(runnerParameters.get(CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM), CodeDeployConstants.TEMP_CREDENTIALS_DURATION_SEC_DEFAULT),
        regionName
      ) :
      new AWSClient(accessKeyId, secretAccessKey, regionName))
      .withDescription("TeamCity build \"" + runningBuild.getBuildTypeName() + "\" #" + runningBuild.getBuildNumber());
  }
}

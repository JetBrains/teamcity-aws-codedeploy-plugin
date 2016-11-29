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
import jetbrains.buildServer.util.amazon.AWSUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.runner.codedeploy.CodeDeployConstants.*;
import static jetbrains.buildServer.runner.codedeploy.CodeDeployUtil.*;
import static jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces;
import static jetbrains.buildServer.util.StringUtil.nullIfEmpty;
import static jetbrains.buildServer.util.amazon.AWSCommonParams.*;

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
        final Map<String, String> configParameters = context.getConfigParameters();

        final Mutable m = new Mutable(configParameters);
        m.problemOccurred = false;
        m.s3ObjectVersion = nullIfEmpty(configParameters.get(S3_OBJECT_VERSION_CONFIG_PARAM));
        m.s3ObjectETag = nullIfEmpty(configParameters.get(S3_OBJECT_ETAG_CONFIG_PARAM));

        final AWSClient awsClient = createAWSClient(runnerParameters, runningBuild).withListener(
          new LoggingDeploymentListener(runnerParameters, runningBuild.getBuildLogger(), runningBuild.getCheckoutDirectory().getAbsolutePath()) {
            @Override
            protected void problem(int identity, @NotNull String type, @NotNull String descr) {
              super.problem(identity, type, descr);
              m.problemOccurred = true;
            }

            @Override
            void uploadRevisionFinished(@NotNull File revision, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag, @NotNull String url) {
              super.uploadRevisionFinished(revision, s3BucketName, s3ObjectKey, s3ObjectVersion, s3ObjectETag, url);
              m.s3ObjectVersion = s3ObjectVersion;
              m.s3ObjectETag = s3ObjectETag;
            }
          });

        final String s3BucketName = runnerParameters.get(S3_BUCKET_NAME_PARAM);
        String s3ObjectKey = runnerParameters.get(S3_OBJECT_KEY_PARAM);

        if (isUploadStepEnabled(runnerParameters) && !m.problemOccurred && !isInterrupted()) {
          final File readyRevision = new ApplicationRevision(
            isEmptyOrSpaces(s3ObjectKey) ? runningBuild.getBuildTypeExternalId() : s3ObjectKey,
            runnerParameters.get(REVISION_PATHS_PARAM),
            context.getWorkingDirectory(), runningBuild.getBuildTempDirectory(),
            configParameters.get(CUSTOM_APPSPEC_YML_CONFIG_PARAM),
            isRegisterStepEnabled(runnerParameters) || isDeployStepEnabled(runnerParameters)).withLogger(runningBuild.getBuildLogger()).getArchive();

          if (isEmptyOrSpaces(s3ObjectKey)) {
            s3ObjectKey = readyRevision.getName();
          }

          awsClient.uploadRevision(readyRevision, s3BucketName, s3ObjectKey);
        }

        final String applicationName = runnerParameters.get(APP_NAME_PARAM);
        final String bundleType = "" + AWSUtil.getBundleType(s3ObjectKey);

        if (CodeDeployUtil.isRegisterStepEnabled(runnerParameters) && !m.problemOccurred && !isInterrupted()) {
          awsClient.registerRevision(s3BucketName, s3ObjectKey, bundleType, m.s3ObjectVersion, m.s3ObjectETag, applicationName);
        }

        if (CodeDeployUtil.isDeployStepEnabled(runnerParameters) && !m.problemOccurred && !isInterrupted()) {
          final String deploymentGroupName = runnerParameters.get(DEPLOYMENT_GROUP_NAME_PARAM);
          final String deploymentConfigName = nullIfEmpty(runnerParameters.get(DEPLOYMENT_CONFIG_NAME_PARAM));

          if (CodeDeployUtil.isDeploymentWaitEnabled(runnerParameters)) {
            awsClient.deployRevisionAndWait(
              s3BucketName, s3ObjectKey, bundleType, m.s3ObjectVersion, m.s3ObjectETag,
              applicationName, deploymentGroupName, deploymentConfigName,
              Integer.parseInt(runnerParameters.get(WAIT_TIMEOUT_SEC_PARAM)),
              getIntegerOrDefault(configParameters.get(WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM), WAIT_POLL_INTERVAL_SEC_DEFAULT));
          } else {
            awsClient.deployRevision(
              s3BucketName, s3ObjectKey, bundleType, m.s3ObjectVersion, m.s3ObjectETag,
              applicationName, deploymentGroupName, deploymentConfigName);
          }
        }

        return m.problemOccurred ? BuildFinishedStatus.FINISHED_WITH_PROBLEMS : BuildFinishedStatus.FINISHED_SUCCESS;
      }

      @NotNull
      private Map<String, String> validateParams() throws RunBuildException {
        final Map<String, String> runnerParameters = context.getRunnerParameters();
        final Map<String, String> invalids = ParametersValidator.validateRuntime(runnerParameters, context.getConfigParameters(), runningBuild.getCheckoutDirectory());
        if (invalids.isEmpty()) return runnerParameters;
        throw new CodeDeployRunnerException(CodeDeployUtil.printStrings(invalids.values()), null);
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
        return RUNNER_TYPE;
      }

      @Override
      public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
        return true;
      }
    };
  }

  @NotNull
  private AWSClient createAWSClient(final Map<String, String> runnerParameters, @NotNull final AgentRunningBuild runningBuild) {
    final Map<String, String> params = new HashMap<String, String>(runnerParameters);
    params.put(TEMP_CREDENTIALS_SESSION_NAME_PARAM, runningBuild.getBuildTypeExternalId() + runningBuild.getBuildId());
    if (CodeDeployUtil.isDeploymentWaitEnabled(runnerParameters)) {
      params.put(TEMP_CREDENTIALS_DURATION_SEC_PARAM, String.valueOf(2 * Integer.parseInt(runnerParameters.get(WAIT_TIMEOUT_SEC_PARAM))));
    }

    return new AWSClient(createAWSClients(params, true)).withDescription("TeamCity build \"" + runningBuild.getBuildTypeName() + "\" #" + runningBuild.getBuildNumber());
  }

  static class CodeDeployRunnerException extends RunBuildException {
    public CodeDeployRunnerException(@NotNull String message, @Nullable Throwable cause) {
      super(message, cause, ErrorData.BUILD_RUNNER_ERROR_TYPE);
      this.setLogStacktrace(false);
    }
  }

  private class Mutable {
    public Mutable(@NotNull Map<String, String> configParameters) {
      problemOccurred = false;
      s3ObjectVersion = nullIfEmpty(configParameters.get(S3_OBJECT_VERSION_CONFIG_PARAM));
      s3ObjectETag = nullIfEmpty(configParameters.get(S3_OBJECT_ETAG_CONFIG_PARAM));
    }
    boolean problemOccurred;
    String s3ObjectVersion;
    String s3ObjectETag;
  }
}

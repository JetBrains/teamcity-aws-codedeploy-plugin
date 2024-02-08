

package jetbrains.buildServer.runner.codedeploy;

import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.s3.AmazonS3;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.util.amazon.AWSClients;
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

        final Map<String, String> runnerParameters = patchParams(validateParams(), runningBuild);
        final Map<String, String> configParameters = context.getConfigParameters();

        final Mutable m = new Mutable(configParameters);
        m.problemOccurred = false;
        m.s3ObjectVersion = nullIfEmpty(configParameters.get(S3_OBJECT_VERSION_CONFIG_PARAM));
        m.s3ObjectETag = nullIfEmpty(configParameters.get(S3_OBJECT_ETAG_CONFIG_PARAM));

        return withAWSClients(runnerParameters, new WithAWSClients<BuildFinishedStatus, CodeDeployRunnerException>() {
          @Nullable
          @Override
          public BuildFinishedStatus run(@NotNull AWSClients clients) throws CodeDeployRunnerException {
            final AWSClient awsClient = createAWSClient(clients.createS3Client(), clients.createCodeDeployClient(), runningBuild).withListener(
              new ServiceMessageLoggingDeploymentListener(runnerParameters, runningBuild.getCheckoutDirectory().getAbsolutePath()) {
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

                @Override
                protected void log(@NotNull String message) {
                  runningBuild.getBuildLogger().message(message);
                }
              });

            final String s3BucketName = getS3BucketName(runnerParameters);
            String s3ObjectKey = getS3ObjectKey(runnerParameters);

            if (isUploadStepEnabled(runnerParameters) && !m.problemOccurred && !isInterrupted()) {
              final File readyRevision = new ApplicationRevision(
                isEmptyOrSpaces(s3ObjectKey) ? runningBuild.getBuildTypeExternalId() : s3ObjectKey,
                getRevisionPaths(runnerParameters),
                context.getWorkingDirectory(), runningBuild.getBuildTempDirectory(),
                configParameters.get(CUSTOM_APPSPEC_YML_CONFIG_PARAM),
                isRegisterStepEnabled(runnerParameters) || isDeployStepEnabled(runnerParameters)).withLogger(runningBuild.getBuildLogger()).getArchive();

              if (isEmptyOrSpaces(s3ObjectKey)) {
                s3ObjectKey = readyRevision.getName();
              }

              awsClient.uploadRevision(readyRevision, s3BucketName, s3ObjectKey);
            }

            final String applicationName = getAppName(runnerParameters);
            final String bundleType = "" + getBundleType(s3ObjectKey);

            if (CodeDeployUtil.isRegisterStepEnabled(runnerParameters) && !m.problemOccurred && !isInterrupted()) {
              awsClient.registerRevision(s3BucketName, s3ObjectKey, bundleType, m.s3ObjectVersion, m.s3ObjectETag, applicationName);
            }

            if (CodeDeployUtil.isDeployStepEnabled(runnerParameters) && !m.problemOccurred && !isInterrupted()) {
              final String deploymentGroupName = getDeploymentGroupName(runnerParameters);
              final String deploymentConfigName = nullIfEmpty(getDeploymentConfigName(runnerParameters));

              awsClient.deployRevision(
                s3BucketName, s3ObjectKey, bundleType, m.s3ObjectVersion, m.s3ObjectETag,
                applicationName, deploymentGroupName,
                getEC2Tags(runnerParameters), getAutoScalingGroups(runnerParameters),
                deploymentConfigName,
                Boolean.parseBoolean(getRollbackOnFailure(runnerParameters)),
                Boolean.parseBoolean(getRollbackOnAlarmThreshold(runnerParameters)),
                getFileExistsBehavior(runnerParameters));
              return m.problemOccurred ? BuildFinishedStatus.FINISHED_WITH_PROBLEMS : BuildFinishedStatus.FINISHED_DETACHED;
            }
            return m.problemOccurred ? BuildFinishedStatus.FINISHED_WITH_PROBLEMS : BuildFinishedStatus.FINISHED_SUCCESS;
          }
        });
      }

      @NotNull
      private Map<String, String> validateParams() throws RunBuildException {
        final Map<String, String> runnerParameters = context.getRunnerParameters();
        final Map<String, String> invalids = ParametersValidator.validateRuntime(runnerParameters, context.getConfigParameters(), runningBuild.getCheckoutDirectory());
        if (invalids.isEmpty()) return runnerParameters;
        throw new CodeDeployRunnerException(CodeDeployUtil.printStrings(invalids.values()), null);
      }

      @NotNull
      private Map<String, String> patchParams(final Map<String, String> runnerParameters, @NotNull final AgentRunningBuild runningBuild) {
        final Map<String, String> params = new HashMap<String, String>(runnerParameters);
        params.put(TEMP_CREDENTIALS_SESSION_NAME_PARAM, runningBuild.getBuildTypeExternalId() + runningBuild.getBuildId());
        return params;
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
  private AWSClient createAWSClient(@NotNull final AmazonS3 s3Client, @NotNull final AmazonCodeDeployClient codeDeployClient, @NotNull final AgentRunningBuild runningBuild) {
    return new AWSClient(s3Client, codeDeployClient).withDescription("TeamCity build \"" + runningBuild.getBuildTypeName() + "\" #" + runningBuild.getBuildNumber());
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
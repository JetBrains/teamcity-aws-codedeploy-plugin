/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import jetbrains.buildServer.util.amazon.AWSException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author vbedrosova
 */
abstract class LoggingDeploymentListener extends AWSClient.Listener {
  @NotNull
  private static final Logger LOG = Logger.getInstance(LoggingDeploymentListener.class.getName());

  static final String DEPLOY_APPLICATION = "deploy application";
  static final String REGISTER_REVISION = "register revision";
  static final String UPLOAD_REVISION = "upload revision";

  @NotNull
  private final Map<String, String> myRunnerParameters;
  @Nullable
  private final String myCheckoutDir;

  LoggingDeploymentListener(@NotNull Map<String, String> runnerParameters, @Nullable String checkoutDir) {
    myRunnerParameters = runnerParameters;
    myCheckoutDir = checkoutDir;
  }

  @Override
  void uploadRevisionStarted(@NotNull File revision, @NotNull String s3BucketName, @NotNull String key) {
    open(UPLOAD_REVISION);
    log(String.format("Uploading application revision %s to S3 bucket %s using key %s", revision.getPath(), s3BucketName, key));
  }

  @Override
  void uploadRevisionFinished(@NotNull File revision, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag, @NotNull String url) {
    final boolean hasVersion = StringUtil.isNotEmpty(s3ObjectVersion);
    final boolean hasETag = StringUtil.isNotEmpty(s3ObjectETag);

    final String directUrl =
      url +
        (hasVersion || hasETag ? "?" : "") +
        (hasVersion ? "versionId=" + s3ObjectVersion : "") +
        (hasVersion && hasETag ? "&" : "") +
        (hasETag ? "etag=" + s3ObjectETag : "");

    log("Uploaded application revision " + directUrl);
    if (!CodeDeployUtil.isRegisterStepEnabled(myRunnerParameters)) {
      statusText("Uploaded " + directUrl);
    }
    if (hasVersion) parameter(CodeDeployConstants.S3_OBJECT_VERSION_CONFIG_PARAM, s3ObjectVersion);
    if (hasETag) parameter(CodeDeployConstants.S3_OBJECT_ETAG_CONFIG_PARAM, s3ObjectETag);
    close(UPLOAD_REVISION);
  }

  @Override
  void registerRevisionStarted(@NotNull String applicationName, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String s3BundleType, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag) {
    open(REGISTER_REVISION);
    log(String.format("Registering application %s revision from S3 bucket %s with key %s, bundle type %s, %s version and %s ETag", applicationName, s3BucketName, s3ObjectKey, s3BundleType, StringUtil.isEmptyOrSpaces(s3ObjectVersion) ? "latest" : s3ObjectVersion, StringUtil.isEmptyOrSpaces(s3ObjectETag) ? "no" : s3ObjectETag));
  }

  @Override
  void registerRevisionFinished(@NotNull String applicationName, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag) {
    if (!CodeDeployUtil.isDeployStepEnabled(myRunnerParameters)) {
      statusText("Registered revision");
    }
    close(REGISTER_REVISION);
  }

  @Override
  void createDeploymentStarted(@NotNull String applicationName, @NotNull String deploymentGroupName, @Nullable String deploymentConfigName) {
    open(DEPLOY_APPLICATION);
    log(String.format("Creating application %s deployment to deployment group %s with %s deployment configuration", applicationName, deploymentGroupName, StringUtil.isEmptyOrSpaces(deploymentConfigName) ? "default" : deploymentConfigName));
  }

  @Override
  void createDeploymentFinished(@NotNull String applicationName, @NotNull String deploymentGroupName, @Nullable String deploymentConfigName, @NotNull String deploymentId) {
    parameter(CodeDeployConstants.DEPLOYMENT_ID_BUILD_CONFIG_PARAM, deploymentId);
    log("Deployment " + deploymentId + " created");
    close(DEPLOY_APPLICATION);
  }

  @Override
  void deploymentWaitStarted(@NotNull String deploymentId) {
  }


  @Override
  void deploymentInProgress(@NotNull String deploymentId, @Nullable InstancesStatus instancesStatus) {
    progress(deploymentDescription(instancesStatus, deploymentId, false));
  }

  @Override
  void deploymentFailed(@NotNull String deploymentId, @Nullable Integer timeoutSec, @Nullable ErrorInfo errorInfo, @Nullable InstancesStatus instancesStatus) {
    String msg = (timeoutSec == null ? "" : "Timeout " + timeoutSec + " sec exceeded, ");

    err(msg + StringUtil.decapitalize(deploymentDescription(instancesStatus, deploymentId, true)));
    msg += StringUtil.decapitalize(deploymentDescription(instancesStatus, deploymentId, false));

    if (errorInfo != null) {
      if (StringUtil.isNotEmpty(errorInfo.message)) {
        err("Associated error: " + errorInfo.message);
        msg += ": " + errorInfo.message;
      }
      if (StringUtil.isNotEmpty(errorInfo.code)) {
        err("Error code: " + errorInfo.code);
      }
    }

    problem(getIdentity(timeoutSec, errorInfo, instancesStatus), timeoutSec == null ? CodeDeployConstants.FAILURE_BUILD_PROBLEM_TYPE : CodeDeployConstants.TIMEOUT_BUILD_PROBLEM_TYPE, msg);
  }

  @Override
  void deploymentSucceeded(@NotNull String deploymentId, @Nullable InstancesStatus instancesStatus) {
    log(deploymentDescription(instancesStatus, deploymentId, true));
    statusText(deploymentDescription(instancesStatus, deploymentId, false));
  }

  @Override
  void exception(@NotNull AWSException e) {
    LOG.error(e);

    final String message = e.getMessage();
    final String details = e.getDetails();

    err(message);
    if (StringUtil.isNotEmpty(details)) err(details);
    problem(getIdentity(e.getIdentity()), e.getType(), message);
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
  private String deploymentDescription(@Nullable InstancesStatus instancesStatus, @Nullable String deploymentId, boolean detailed) {
    final StringBuilder sb = new StringBuilder("Deployment ");

    if (StringUtil.isNotEmpty(deploymentId)) {
      sb.append(deploymentId).append(" ");
    }

    if (instancesStatus == null) sb.append(CodeDeployConstants.STATUS_IS_UNKNOWN);
    else {
      sb.append(StringUtil.isEmptyOrSpaces(instancesStatus.status) ? CodeDeployConstants.STATUS_IS_UNKNOWN : instancesStatus.status);
      sb.append(", ").append(instancesStatus.succeeded).append(" ").append(StringUtil.pluralize("instance", instancesStatus.succeeded)).append(" succeeded");
      if (instancesStatus.failed > 0 || detailed) sb.append(", ").append(instancesStatus.failed).append(" failed");
      if (instancesStatus.pending > 0 || detailed) sb.append(", ").append(instancesStatus.pending).append(" pending");
      if (instancesStatus.skipped > 0 || detailed) sb.append(", ").append(instancesStatus.skipped).append(" skipped");
      if (instancesStatus.inProgress > 0 || detailed) sb.append(", ").append(instancesStatus.inProgress).append(" in progress");
    }

    return sb.toString();
  }

  private int getIdentity(String... parts) {
    return AWSCommonParams.calculateIdentity(myCheckoutDir, myRunnerParameters, CollectionsUtil.join(getIdentityFormingParameters(), Arrays.asList(parts)));
  }

  @NotNull
  private Collection<String> getIdentityFormingParameters() {
    return Arrays.asList(
      CodeDeployUtil.getS3BucketName(myRunnerParameters),
      CodeDeployUtil.getAppName(myRunnerParameters),
      CodeDeployUtil.getDeploymentGroupName(myRunnerParameters));
  }

  protected abstract void log(@NotNull String message);

  protected abstract void err(@NotNull String message);

  protected abstract void open(@NotNull String block);

  protected abstract void close(@NotNull String block);

  protected abstract void progress(@NotNull String message);

  protected abstract void problem(int identity, @NotNull String type, @NotNull String descr);

  protected abstract void parameter(@NotNull String name, @NotNull String value);

  protected abstract void statusText(@NotNull String text);
}

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

import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.model.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSClients;
import jetbrains.buildServer.util.amazon.AWSException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author vbedrosova
 */
public class AWSClient {

  @NotNull private AmazonS3Client myS3Client;
  @NotNull private AmazonCodeDeployClient myCodeDeployClient;
  @Nullable private String myDescription;
  @NotNull private Listener myListener = new Listener();

  public AWSClient(@NotNull AWSClients clients) {
    myS3Client = clients.createS3Client();
    myCodeDeployClient = clients.createCodeDeployClient();
  }

  @NotNull
  public AWSClient withDescription(@NotNull String description) {
    myDescription = description;
    return this;
  }

  @NotNull
  public AWSClient withListener(@NotNull Listener listener) {
    myListener = listener;
    return this;
  }

  /**
   * Uploads application revision archive to S3 bucket named s3BucketName with the provided key and bundle type.
   * <p>
   * For performing this operation target AWSClient must have corresponding S3 permissions.
   *
   * @param revision     valid application revision containing appspec.yml
   * @param s3BucketName valid S3 bucket name
   * @param s3ObjectKey  valid S3 object key
   */
  public void uploadRevision(@NotNull File revision,
                             @NotNull String s3BucketName, @NotNull String s3ObjectKey) {
    try {
      doUploadRevision(revision, s3BucketName, s3ObjectKey);
    } catch (Throwable t) {
      processFailure(t);
    }
  }

  /**
   * Registers application revision from the specified location for the specified CodeDeploy application.
   * <p>
   * For performing this operation target AWSClient must have corresponding CodeDeploy permissions.
   *
   * @param s3BucketName    valid S3 bucket name
   * @param s3ObjectKey     valid S3 object key
   * @param bundleType      one of zip, tar or tar.gz
   * @param s3ObjectVersion S3 object version (for versioned buckets) or null to use the latest version
   * @param applicationName CodeDeploy application name
   */
  public void registerRevision(@NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion,
                               @NotNull String applicationName) {
    try {
      doRegisterRevision(getRevisionLocation(s3BucketName, s3ObjectKey, bundleType, s3ObjectVersion), applicationName);
    } catch (Throwable t) {
      processFailure(t);
    }
  }

  /**
   * Creates deployment of the application revision from the specified location for specified application (must be pre-configured) to
   * deploymentGroupName (must be pre-configured) EC2 instances group with
   * deploymentConfigName or default configuration name and waits for deployment finish.
   * <p>
   * For performing this operation target AWSClient must have corresponding CodeDeploy permissions.
   *
   * @param s3BucketName         valid S3 bucket name
   * @param s3ObjectKey          valid S3 object key
   * @param bundleType           one of zip, tar or tar.gz
   * @param s3ObjectVersion      S3 object version (for versioned buckets) or null to use the latest version
   * @param applicationName      CodeDeploy application name
   * @param deploymentGroupName  deployment group name
   * @param deploymentConfigName deployment configuration name or null for default deployment configuration
   * @param waitTimeoutSec       seconds to wait for the created deployment finish or fail
   * @param waitIntervalSec      seconds between polling CodeDeploy for the created deployment status
   */
  public void deployRevisionAndWait(@NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion,
                                    @NotNull String applicationName, @NotNull String deploymentGroupName, @Nullable String deploymentConfigName,
                                    int waitTimeoutSec, int waitIntervalSec) {
    doDeployAndWait(s3BucketName, s3ObjectKey, bundleType, s3ObjectVersion, applicationName, deploymentGroupName, deploymentConfigName, true, waitTimeoutSec, waitIntervalSec);
  }

  /**
   * The same as {@link #deployRevisionAndWait} but without waiting
   */
  public void deployRevision(@NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion,
                             @NotNull String applicationName, @NotNull String deploymentGroupName, @Nullable String deploymentConfigName) {
    doDeployAndWait(s3BucketName, s3ObjectKey, bundleType, s3ObjectVersion, applicationName, deploymentGroupName, deploymentConfigName, false, null, null);
  }

  @SuppressWarnings("ConstantConditions")
  private void doDeployAndWait(@NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion,
                               @NotNull String applicationName, @NotNull String deploymentGroupName, @Nullable String deploymentConfigName,
                               boolean wait, @Nullable Integer waitTimeoutSec, @Nullable Integer waitIntervalSec) {
    try {
        final String deploymentId = createDeployment(getRevisionLocation(s3BucketName, s3ObjectKey, bundleType, s3ObjectVersion), applicationName, deploymentGroupName, deploymentConfigName);

        if (wait) {
          waitForDeployment(deploymentId, waitTimeoutSec, waitIntervalSec);
        }
    } catch (Throwable t) {
      processFailure(t);
    }
  }

  private void waitForDeployment(@NotNull String deploymentId, int waitTimeoutSec, int waitIntervalSec) {
    myListener.deploymentWaitStarted(deploymentId);

    final GetDeploymentRequest dRequest = new GetDeploymentRequest().withDeploymentId(deploymentId);

    DeploymentInfo dInfo = myCodeDeployClient.getDeployment(dRequest).getDeploymentInfo();

    long startTime = (dInfo == null || dInfo.getStartTime() == null) ? System.currentTimeMillis() : dInfo.getStartTime().getTime();

    while (dInfo == null || dInfo.getCompleteTime() == null) {
      myListener.deploymentInProgress(deploymentId, getInstancesStatus(dInfo));

      if (System.currentTimeMillis() - startTime > waitTimeoutSec * 1000) {
        myListener.deploymentFailed(deploymentId, waitTimeoutSec, getErrorInfo(dInfo), getInstancesStatus(dInfo));
        return;
      }

      try {
        Thread.sleep(waitIntervalSec * 1000);
      } catch (InterruptedException e) {
        processFailure(e);
        return;
      }

      dInfo = myCodeDeployClient.getDeployment(dRequest).getDeploymentInfo();
    }

    if (isSuccess(dInfo)) {
      myListener.deploymentSucceeded(deploymentId, getInstancesStatus(dInfo));
    } else {
      myListener.deploymentFailed(deploymentId, null, getErrorInfo(dInfo), getInstancesStatus(dInfo));
    }
  }

  private void doUploadRevision(@NotNull File revision, @NotNull String s3BucketName, @NotNull String s3ObjectKey) {
    myListener.uploadRevisionStarted(revision, s3BucketName, s3ObjectKey);

    myS3Client.putObject(new PutObjectRequest(s3BucketName, s3ObjectKey, revision));

    myListener.uploadRevisionFinished(revision, s3BucketName, s3ObjectKey, myS3Client.getUrl(s3BucketName, s3ObjectKey).toString());
  }

  @NotNull
  private RevisionLocation getRevisionLocation(@NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion) {
    final S3Location loc = new S3Location().withBucket(s3BucketName).withKey(s3ObjectKey).withBundleType(bundleType);
    if (StringUtil.isNotEmpty(s3ObjectVersion)) loc.withVersion(s3ObjectVersion);
    return new RevisionLocation().withRevisionType(RevisionLocationType.S3).withS3Location(loc);
  }

  private void doRegisterRevision(@NotNull RevisionLocation revisionLocation, @NotNull String applicationName) {
    final S3Location s3Location = revisionLocation.getS3Location();
    myListener.registerRevisionStarted(applicationName, s3Location.getBucket(), s3Location.getKey(), s3Location.getBundleType(), s3Location.getVersion());

    myCodeDeployClient.registerApplicationRevision(
      new RegisterApplicationRevisionRequest()
        .withRevision(revisionLocation)
        .withApplicationName(applicationName)
        .withDescription(getDescription("Application revision registered by ", 100)));

    myListener.registerRevisionFinished(applicationName, s3Location.getBucket(), s3Location.getKey(), s3Location.getBundleType(), s3Location.getVersion());
  }

  @NotNull
  private String createDeployment(@NotNull RevisionLocation revisionLocation,
                                  @NotNull String applicationName,
                                  @NotNull String deploymentGroupName,
                                  @Nullable String deploymentConfigName) {
    myListener.createDeploymentStarted(applicationName, deploymentGroupName, deploymentConfigName);

    final CreateDeploymentRequest request =
      new CreateDeploymentRequest()
        .withRevision(revisionLocation)
        .withApplicationName(applicationName)
        .withDeploymentGroupName(deploymentGroupName)
        .withDescription(getDescription("Deployment created by ", 100));

    if (StringUtil.isNotEmpty(deploymentConfigName)) request.setDeploymentConfigName(deploymentConfigName);

    final String deploymentId = myCodeDeployClient.createDeployment(request).getDeploymentId();
    myListener.createDeploymentFinished(applicationName, deploymentGroupName, deploymentConfigName, deploymentId);
    return deploymentId;
  }

  private void processFailure(@NotNull Throwable t) {
    myListener.exception(new AWSException(t));
  }

  private boolean isSuccess(@NotNull DeploymentInfo dInfo) {
    return DeploymentStatus.Succeeded.toString().equals(dInfo.getStatus());
  }

  @NotNull
  private String getDescription(@NotNull String prefix, int threshold) {
    return prefix + CodeDeployUtil.truncateStringValueWithDotsAtCenter(StringUtil.isEmptyOrSpaces(myDescription) ? getClass().getName() : myDescription, threshold - prefix.length());
  }

  @Contract("null -> null")
  @Nullable
  private Listener.InstancesStatus getInstancesStatus(@Nullable DeploymentInfo dInfo) {
    if (dInfo == null) return null;
    if (dInfo.getStatus() == null || dInfo.getDeploymentOverview() == null) return null;

    final Listener.InstancesStatus instancesStatus = new Listener.InstancesStatus();
    instancesStatus.status = getHumanReadableStatus(dInfo.getStatus());

    final DeploymentOverview overview = dInfo.getDeploymentOverview();
    instancesStatus.succeeded = getInt(overview.getSucceeded());
    instancesStatus.failed = getInt(overview.getFailed());
    instancesStatus.inProgress = getInt(overview.getInProgress());
    instancesStatus.skipped = getInt(overview.getSkipped());
    instancesStatus.pending = getInt(overview.getPending());

    return instancesStatus;
  }

  private static int getInt(@Nullable Long l) {
    return l == null ? 0 : l.intValue();
  }

  @NotNull
  private String getHumanReadableStatus(@NotNull String status) {
    if (DeploymentStatus.Created.toString().equals(status)) return "created";
    if (DeploymentStatus.Queued.toString().equals(status)) return "queued";
    if (DeploymentStatus.InProgress.toString().equals(status)) return "in progress";
    if (DeploymentStatus.Succeeded.toString().equals(status)) return "succeeded";
    if (DeploymentStatus.Failed.toString().equals(status)) return "failed";
    if (DeploymentStatus.Stopped.toString().equals(status)) return "stopped";
    return CodeDeployConstants.STATUS_IS_UNKNOWN;
  }

  @Contract("null -> null")
  @Nullable
  private Listener.ErrorInfo getErrorInfo(@Nullable DeploymentInfo dInfo) {
    if (dInfo == null) return null;

    final ErrorInformation errorInformation = dInfo.getErrorInformation();
    if (errorInformation == null) return null;

    final Listener.ErrorInfo errorInfo = new Listener.ErrorInfo();
    errorInfo.message = removeTrailingDot(errorInformation.getMessage());
    errorInfo.code = errorInformation.getCode();
    return errorInfo;
  }

  @Contract("null -> null")
  @Nullable
  private String removeTrailingDot(@Nullable String msg) {
    return (msg != null && msg.endsWith(".")) ? msg.substring(0, msg.length() - 1) : msg;
  }

  public static class Listener {
    void uploadRevisionStarted(@NotNull File revision, @NotNull String s3BucketName, @NotNull String s3ObjectKey) {}
    void uploadRevisionFinished(@NotNull File revision, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String url) {}
    void registerRevisionStarted(@NotNull String applicationName, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion) {}
    void registerRevisionFinished(@NotNull String applicationName, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion) {}
    void createDeploymentStarted(@NotNull String applicationName, @NotNull String deploymentGroupName, @Nullable String deploymentConfigName) {}
    void createDeploymentFinished(@NotNull String applicationName, @NotNull String deploymentGroupName, @Nullable String deploymentConfigName, @NotNull String deploymentId) {}
    void deploymentWaitStarted(@NotNull String deploymentId) {}
    void deploymentInProgress(@NotNull String deploymentId, @Nullable InstancesStatus instancesStatus) {}
    void deploymentFailed(@NotNull String deploymentId, @Nullable Integer timeoutSec, @Nullable ErrorInfo errorInfo, @Nullable InstancesStatus instancesStatus) {}
    void deploymentSucceeded(@NotNull String deploymentId, @Nullable InstancesStatus instancesStatus) {}
    void exception(@NotNull AWSException exception) {}

    public static class InstancesStatus {
      int pending;
      int inProgress;
      int succeeded;
      int failed;
      int skipped;
      @Nullable
      String status;
    }

    public static class ErrorInfo {
      @Nullable
      String code;
      @Nullable
      String message;
    }
  }
}

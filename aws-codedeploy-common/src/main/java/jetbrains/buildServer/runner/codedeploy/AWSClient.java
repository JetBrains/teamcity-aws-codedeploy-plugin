

package jetbrains.buildServer.runner.codedeploy;

import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSException;
import jetbrains.buildServer.util.amazon.S3Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author vbedrosova
 */
@SuppressWarnings("JavaDoc")
public class AWSClient {

  @NotNull private final AmazonS3 myS3Client;
  @NotNull private final AmazonCodeDeployClient myCodeDeployClient;
  @Nullable private String myDescription;
  @NotNull private Listener myListener = new Listener();

  public AWSClient(@NotNull AmazonS3 s3Client,
                   @NotNull AmazonCodeDeployClient codeDeployClient) {
    myS3Client = s3Client;
    myCodeDeployClient = codeDeployClient;
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
   * @param s3ObjectETag    S3 object ETag (file checksum) for object validation or null if no validation should be performed
   * @param applicationName CodeDeploy application name
   */
  public void registerRevision(@NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag,
                               @NotNull String applicationName) {
    try {
      doRegisterRevision(getRevisionLocation(s3BucketName, s3ObjectKey, bundleType, s3ObjectVersion, s3ObjectETag), applicationName);
    } catch (Throwable t) {
      processFailure(t);
    }
  }

  /**
   * Creates deployment of the application revision from the specified location for specified application (must be pre-configured) to
   * deploymentGroupName (must be pre-configured) EC2 instances group with
   * deploymentConfigName or default configuration name.
   * <p>
   * For performing this operation target AWSClient must have corresponding CodeDeploy permissions.
   *
   * @param s3BucketName         valid S3 bucket name
   * @param s3ObjectKey          valid S3 object key
   * @param bundleType           one of zip, tar or tar.gz
   * @param s3ObjectVersion      S3 object version (for versioned buckets) or null to use the latest version
   * @param s3ObjectETag         S3 object ETag (file checksum) for object validation or null if no validation should be performed
   * @param applicationName      CodeDeploy application name
   * @param deploymentGroupName  deployment group name
   * @param deploymentConfigName deployment configuration name or null for default deployment configuration
   */
  public void deployRevision(@NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag,
                             @NotNull String applicationName, @NotNull String deploymentGroupName,
                             @NotNull Map<String, String> ec2Tags, @NotNull Collection<String> autoScalingGroups,
                             @Nullable String deploymentConfigName,
                             boolean rollbackOnFailure, boolean rollbackOnAlarmThreshold, @Nullable String fileExistsBehavior) {
    doDeploy(s3BucketName, s3ObjectKey, bundleType, s3ObjectVersion, s3ObjectETag, applicationName, deploymentGroupName, ec2Tags, autoScalingGroups, deploymentConfigName, rollbackOnFailure, rollbackOnAlarmThreshold, fileExistsBehavior);
  }

  private void doDeploy(@NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag,
                        @NotNull String applicationName, @NotNull String deploymentGroupName,
                        @NotNull Map<String, String> ec2Tags, @NotNull Collection<String> autoScalingGroups,
                        @Nullable String deploymentConfigName,
                        boolean rollbackOnFailure, boolean rollbackOnAlarmThreshold, @Nullable String fileExistsBehavior) {
    try {
      createDeployment(getRevisionLocation(s3BucketName, s3ObjectKey, bundleType, s3ObjectVersion, s3ObjectETag), applicationName, deploymentGroupName, ec2Tags, autoScalingGroups, deploymentConfigName, rollbackOnFailure, rollbackOnAlarmThreshold, fileExistsBehavior);
    } catch (Throwable t) {
      processFailure(t);
    }
  }

  /**
   *
   * @param deploymentId
   * @param knownDeploymentStartTime
   * @return deployment finis date or null if it's still in progress
   */
  @Nullable
  public Date checkDeploymentStatus(@NotNull String deploymentId, @Nullable Date knownDeploymentStartTime) {
    final DeploymentInfo dInfo = myCodeDeployClient.getDeployment(new GetDeploymentRequest().withDeploymentId(deploymentId)).getDeploymentInfo();

    if (dInfo == null || dInfo.getCompleteTime() == null) { // deployment in progress?
      myListener.deploymentInProgress(deploymentId, getInstancesStatus(dInfo));
      return null;
    }

    if (isSuccess(dInfo)) {
      myListener.deploymentSucceeded(deploymentId, getInstancesStatus(dInfo));
    } else {
      myListener.deploymentFailed(deploymentId, null, getErrorInfo(dInfo), getInstancesStatus(dInfo));
    }

    return dInfo.getCompleteTime();
  }

  private void doUploadRevision(@NotNull final File revision, @NotNull final String s3BucketName, @NotNull final String s3ObjectKey) throws Throwable {
    myListener.uploadRevisionStarted(revision, s3BucketName, s3ObjectKey);

    final UploadResult uploadResult = doUploadWithTransferManager(revision, s3BucketName, s3ObjectKey);

    myListener.uploadRevisionFinished(revision, s3BucketName, s3ObjectKey, uploadResult.getVersionId(), uploadResult.getETag(), myS3Client.getUrl(s3BucketName, s3ObjectKey).toString());
  }

  @NotNull
  private UploadResult doUploadWithTransferManager(@NotNull final File revision, @NotNull final String s3BucketName, @NotNull final String s3ObjectKey) throws Throwable {
    return S3Util.withTransferManager(myS3Client, new S3Util.WithTransferManager<Upload>() {
      @NotNull
      @Override
      public Collection<Upload> run(@NotNull TransferManager manager) throws Throwable {
        return Collections.singletonList(manager.upload(s3BucketName, s3ObjectKey, revision));
      }
    }).iterator().next().waitForUploadResult();
  }

  @NotNull
  private RevisionLocation getRevisionLocation(@NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag) {
    final S3Location loc = new S3Location().withBucket(s3BucketName).withKey(s3ObjectKey).withBundleType(bundleType);
    if (StringUtil.isNotEmpty(s3ObjectVersion)) loc.withVersion(s3ObjectVersion);
    if (StringUtil.isNotEmpty(s3ObjectETag)) loc.withETag(s3ObjectETag);
    return new RevisionLocation().withRevisionType(RevisionLocationType.S3).withS3Location(loc);
  }

  private void doRegisterRevision(@NotNull RevisionLocation revisionLocation, @NotNull String applicationName) {
    final S3Location s3Location = revisionLocation.getS3Location();
    myListener.registerRevisionStarted(applicationName, s3Location.getBucket(), s3Location.getKey(), s3Location.getBundleType(), s3Location.getVersion(), s3Location.getETag());

    myCodeDeployClient.registerApplicationRevision(
      new RegisterApplicationRevisionRequest()
        .withRevision(revisionLocation)
        .withApplicationName(applicationName)
        .withDescription(getDescription("Application revision registered by ", 100)));

    myListener.registerRevisionFinished(applicationName, s3Location.getBucket(), s3Location.getKey(), s3Location.getBundleType(), s3Location.getVersion(), s3Location.getETag());
  }

  @NotNull
  private String createDeployment(@NotNull RevisionLocation revisionLocation,
                                  @NotNull String applicationName,
                                  @NotNull String deploymentGroupName,
                                  @NotNull Map<String, String> ec2Tags,
                                  @NotNull Collection<String> autoScalingGroups,
                                  @Nullable String deploymentConfigName,
                                  boolean rollbackOnFailure,
                                  boolean rollbackOnAlarmThreshold,
                                  @Nullable String fileExistsBehavior) {
    myListener.createDeploymentStarted(applicationName, deploymentGroupName, deploymentConfigName);

    final CreateDeploymentRequest request =
      new CreateDeploymentRequest()
        .withRevision(revisionLocation)
        .withApplicationName(applicationName)
        .withDeploymentGroupName(deploymentGroupName)
        .withFileExistsBehavior(fileExistsBehavior)
        .withDescription(getDescription("Deployment created by ", 100));

    if (StringUtil.isNotEmpty(deploymentConfigName)) request.setDeploymentConfigName(deploymentConfigName);
    if (!ec2Tags.isEmpty() || !autoScalingGroups.isEmpty()) {
        request.withTargetInstances(new TargetInstances().withTagFilters(getTagFilters(ec2Tags)).withAutoScalingGroups(autoScalingGroups));
    }
    if (rollbackOnFailure || rollbackOnAlarmThreshold) {
      final AutoRollbackConfiguration rollbackConfiguration = new AutoRollbackConfiguration().withEnabled(true);
      if (rollbackOnFailure) {
        rollbackConfiguration.withEvents(AutoRollbackEvent.DEPLOYMENT_FAILURE);
      }
      if (rollbackOnAlarmThreshold) {
        rollbackConfiguration.withEvents(AutoRollbackEvent.DEPLOYMENT_STOP_ON_ALARM);
      }
      request.setAutoRollbackConfiguration(rollbackConfiguration);
    }

    final String deploymentId = myCodeDeployClient.createDeployment(request).getDeploymentId();
    myListener.createDeploymentFinished(applicationName, deploymentGroupName, deploymentConfigName, deploymentId);
    return deploymentId;
  }

  @NotNull
  private Collection<EC2TagFilter> getTagFilters(@NotNull Map<String, String> ec2Tags) {
    return CollectionsUtil.convertCollection(ec2Tags.entrySet(), new Converter<EC2TagFilter, Map.Entry<String, String>>() {
      @Override
      public EC2TagFilter createFrom(@NotNull Map.Entry<String, String> e) {
        return new EC2TagFilter().withKey(e.getKey()).withValue(e.getValue()).withType(EC2TagFilterType.KEY_AND_VALUE);
      }
    });
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
    if (DeploymentStatus.Ready.toString().equals(status)) return "ready";
    return StringUtil.decapitalize(status);
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
    void uploadRevisionFinished(@NotNull File revision, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag, @NotNull String url) {}
    void registerRevisionStarted(@NotNull String applicationName, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag) {}
    void registerRevisionFinished(@NotNull String applicationName, @NotNull String s3BucketName, @NotNull String s3ObjectKey, @NotNull String bundleType, @Nullable String s3ObjectVersion, @Nullable String s3ObjectETag) {}
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
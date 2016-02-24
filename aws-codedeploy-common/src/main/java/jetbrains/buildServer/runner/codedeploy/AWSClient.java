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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.model.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URL;

/**
 * @author vbedrosova
 */
public class AWSClient {
  public static final String UNSUPPORTED_SESSION_NAME_CHARS = "[^\\w+=,.@-]";

  @Nullable private final AWSCredentials myCredentials;
  @NotNull private final Region myRegion;

  @Nullable private String myBaseDir;
  @Nullable private String myDescription;

  @NotNull private Logger myLogger = new Logger();

  public AWSClient(@Nullable String accessKeyId, @Nullable String secretAccessKey, @NotNull String regionName) {
    this(getBasicCredentials(accessKeyId, secretAccessKey), getRegion(regionName));
  }

  public AWSClient(@NotNull String iamRoleARN, @Nullable String externalID,
                   @Nullable String accessKeyId, @Nullable String secretAccessKey,
                   @NotNull String sessionName, int sessionDuration,
                   @NotNull String regionName) {
    this(getTempCredentials(iamRoleARN, externalID, accessKeyId, secretAccessKey, patchSessionName(sessionName), sessionDuration), getRegion(regionName));
  }

  @NotNull
  private static String patchSessionName(@NotNull String sessionName) {
    return sessionName.replaceAll(UNSUPPORTED_SESSION_NAME_CHARS, "_");
  }

  @Nullable
  private static BasicAWSCredentials getBasicCredentials(@Nullable String accessKeyId, @Nullable String secretAccessKey) {
    if (accessKeyId == null) return null;
    if (secretAccessKey == null) throw new IllegalArgumentException("");
    return new BasicAWSCredentials(accessKeyId, secretAccessKey);
  }

  @NotNull
  private static AWSCredentials getTempCredentials(@NotNull String iamRoleARN, @Nullable String externalID,
                                                   @Nullable String accessKeyId, @Nullable String secretAccessKey,
                                                   @NotNull String sessionName, int sessionDuration) {
    final AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest().withRoleArn(iamRoleARN).withRoleSessionName(sessionName).withDurationSeconds(sessionDuration);
    if (StringUtil.isNotEmpty(externalID)) assumeRoleRequest.setExternalId(externalID);

    final Credentials credentials = new AWSSecurityTokenServiceClient(getBasicCredentials(accessKeyId, secretAccessKey)).assumeRole(assumeRoleRequest).getCredentials();
    return new BasicSessionCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey(), credentials.getSessionToken());
  }

  private AWSClient(@Nullable AWSCredentials credentials, @NotNull Region region) {
    myCredentials = credentials;
    myRegion = region;
  }

  @NotNull
  public AWSClient withBaseDir(@NotNull String baseDir) {
    myBaseDir = baseDir;
    return this;
  }

  @NotNull
  public AWSClient withDescription(@NotNull String description) {
    myDescription = description;
    return this;
  }

  @NotNull
  public AWSClient withLogger(@NotNull Logger logger) {
    myLogger = logger;
    return this;
  }

  @NotNull
  private AmazonS3Client createS3Client() {
    return (myCredentials == null ? new AmazonS3Client() : new AmazonS3Client(myCredentials)).withRegion(myRegion);
  }

  @NotNull
  private AmazonCodeDeployClient createCodeDeployClient() {
    return (myCredentials == null ? new AmazonCodeDeployClient() : new AmazonCodeDeployClient(myCredentials)).withRegion(myRegion);
  }

  /**
   * Uploads application revision archive to S3 bucket named s3BucketName,
   * registers it in CodeDeploy for the specified application,
   * creates deployment for specified application (must be pre-configured) to
   * deploymentGroupName (must be pre-configured) EC2 instances group with
   * deploymentConfigName or default configuration name and waits for deployment finish.
   *
   * For performing this operation target AWSClient must have corresponding S3 and CodeDeploy permissions.
   *
   * @param revision application revision
   * @param s3BucketName valid S3 bucket name
   * @param applicationName CodeDeploy application name
   * @param deploymentGroupName deployment group name
   * @param deploymentConfigName deployment configuration name or null for default deployment configuration
   * @param waitTimeoutSec seconds to wait for the created deployment finish or fail
   * @param waitIntervalSec seconds between polling CodeDeploy for the created deployment status
   */
  public void uploadRegisterDeployRevisionAndWait(@NotNull File revision, @NotNull String s3BucketName,
                                                  @NotNull String applicationName,
                                                  @NotNull String deploymentGroupName,
                                                  @Nullable String deploymentConfigName,
                                                  int waitTimeoutSec,
                                                  int waitIntervalSec) {
    uploadRegisterDeployWait(revision, s3BucketName, applicationName, true, deploymentGroupName, deploymentConfigName, true, waitTimeoutSec, waitIntervalSec);
  }


  /**
   * The same as uploadRegisterDeployRevisionAndWait but without waiting
   */
  public void uploadRegisterAndDeployRevision(@NotNull File revision, @NotNull String s3BucketName,
                                              @NotNull String applicationName,
                                              @NotNull String deploymentGroupName,
                                              @Nullable String deploymentConfigName) {
    uploadRegisterDeployWait(revision, s3BucketName, applicationName, true, deploymentGroupName, deploymentConfigName, false, null, null);
  }

  /**
   * The same as uploadRegisterAndDeployRevision but without creating deployment
   */
  public void uploadAndRegisterRevision(@NotNull File revision, @NotNull String s3BucketName, @NotNull String applicationName) {
    uploadRegisterDeployWait(revision, s3BucketName, applicationName, false, null, null, false, null, null);
  }

  @SuppressWarnings("ConstantConditions")
  private void uploadRegisterDeployWait(@NotNull File revision, @NotNull String s3BucketName,
                                        @NotNull String applicationName,
                                        boolean deploy,
                                        @Nullable String deploymentGroupName,
                                        @Nullable String deploymentConfigName,
                                        boolean wait,
                                        @Nullable Integer waitTimeoutSec,
                                        @Nullable Integer waitIntervalSec) {
    debug("AWS region is " + myRegion.getName());

    final String key = revision.getName();
    try {

      final RevisionLocation revisionLocation = uploadRevision(revision, s3BucketName, key);
      registerRevision(revisionLocation, applicationName);

      if (deploy) {
        final CreateDeploymentResult deployment = createDeployment(revisionLocation, applicationName, deploymentGroupName, deploymentConfigName);

        if (wait) {
          waitForDeployment(deployment, waitTimeoutSec, waitIntervalSec);
        }
      }
    } catch (Throwable t) {
      failBuild(t, s3BucketName.concat(key).concat(applicationName));
    }
  }

  private void waitForDeployment(@NotNull CreateDeploymentResult deploymentResult, int waitTimeoutSec, int waitIntervalSec) {
    log("Waiting for deployment " + deploymentResult.getDeploymentId() + " finish");

    final AmazonCodeDeployClient cdClient = createCodeDeployClient();

    final GetDeploymentRequest dRequest = new GetDeploymentRequest().withDeploymentId(deploymentResult.getDeploymentId());

    DeploymentInfo dInfo = cdClient.getDeployment(dRequest).getDeploymentInfo();

    long startTime = (dInfo == null || dInfo.getStartTime() == null) ? System.currentTimeMillis() : dInfo.getStartTime().getTime();

    while (dInfo == null || dInfo.getCompleteTime() == null) {
      dInfo = cdClient.getDeployment(dRequest).getDeploymentInfo();

      debug("Deployment " + deploymentInfoText(dInfo));

      if (System.currentTimeMillis() - startTime > waitTimeoutSec * 1000) {
        failBuild(waitTimeoutSec, dInfo);
        return;
      }

      try {
        Thread.sleep(waitIntervalSec * 1000);
      } catch (InterruptedException e) {
        failBuild(e, null);
        return;
      }
    }

    final String msg = "Deployment " + dInfo.getDeploymentId() + " finished in " + formatDuration(dInfo.getCompleteTime().getTime() - startTime) + " with " + deploymentInfoText(dInfo);
    if (isSuccess(dInfo)) {
      log(msg);
      updateBuildStatusText(dInfo);
      return;
    } else err(msg);

    failBuild(null, dInfo);
  }

  @NotNull
  private RevisionLocation uploadRevision(@NotNull File revision, @NotNull String s3BucketName, @NotNull String key) {
    log(String.format("Uploading application revision %s to S3 bucket %s using key %s", revision.getPath(), s3BucketName, key));

    final AmazonS3Client s3Client = createS3Client();
    s3Client.putObject(new PutObjectRequest(s3BucketName, key, revision));

    final URL url = s3Client.getUrl(s3BucketName, key);
    log("Uploaded application revision S3 link : " + url.toString());

    final S3Location loc = new S3Location().withBucket(s3BucketName).withKey(key).withBundleType(getBundleType(revision.getName()));
    return new RevisionLocation().withRevisionType(RevisionLocationType.S3).withS3Location(loc);
  }

  @NotNull
  public static BundleType getBundleType(@NotNull String revision) throws IllegalArgumentException {
    if (revision.endsWith(".zip")) return BundleType.Zip;
    if (revision.endsWith(".tar")) return BundleType.Tar;
    if (revision.endsWith(".tar.gz")) return BundleType.Tgz;

    throw new IllegalArgumentException("Supported application revision extensions are .zip, .tar and .tar.gz");
  }

  @NotNull
  public static Region getRegion(@NotNull String regionName) throws IllegalArgumentException {
    final Region region = Region.getRegion(Regions.fromName(regionName));
    if (region == null) throw new IllegalArgumentException("Unsupported region name " + regionName);
    return region;
  }

  private void registerRevision(@NotNull RevisionLocation revisionLocation, @NotNull String applicationName) {
    log(String.format("Registering application %s revision from S3 bucket %s", applicationName, revisionLocation.getS3Location().getBucket()));
    createCodeDeployClient().registerApplicationRevision(
      new RegisterApplicationRevisionRequest()
        .withRevision(revisionLocation)
        .withApplicationName(applicationName)
        .withDescription("Application revision registered by TeamCity build " + getDescription()));
  }

  @NotNull
  private CreateDeploymentResult createDeployment(@NotNull RevisionLocation revisionLocation,
                                                  @NotNull String applicationName,
                                                  @NotNull String deploymentGroupName,
                                                  @Nullable String deploymentConfigName) {
    log(String.format("Creating deployment to deployment group %s with %s deployment configuration", deploymentGroupName, StringUtil.isEmptyOrSpaces(deploymentConfigName) ? "default" : deploymentConfigName));

    final CreateDeploymentRequest request =
      new CreateDeploymentRequest()
        .withRevision(revisionLocation)
        .withApplicationName(applicationName)
        .withDeploymentGroupName(deploymentGroupName)
        .withDescription("Deployment created by TeamCity build " + getDescription());

    if (StringUtil.isNotEmpty(deploymentConfigName)) request.setDeploymentConfigName(deploymentConfigName);

    final CreateDeploymentResult d = createCodeDeployClient().createDeployment(request);
    log("Deployment " + d.getDeploymentId() + " created");
    return d;
  }

  private void failBuild(@Nullable Integer timeoutSec, @NotNull DeploymentInfo dInfo) {
    if (isSuccess(dInfo)) return;

    String msg;
    if (timeoutSec == null) msg = "Deployment failed";
    else {
      msg = "Timeout " + timeoutSec + "sec exceeded for deployment " + dInfo.getDeploymentId() + ", " + deploymentInfoText(dInfo);
      err(msg);
    }

    final ErrorInformation eInfo = dInfo.getErrorInformation();
    if (eInfo != null) {
      err("Associated error: " + eInfo.getMessage());
      err("Error code:       " + eInfo.getCode());

      msg += ": " + eInfo.getMessage();
    }

    problem(getIdentity(dInfo), timeoutSec == null? "CODE_DEPLOY_FAILURE" : "CODE_DEPLOY_TIMEOUT", msg);
  }

  private void failBuild(@NotNull Throwable t, @Nullable String footPrint) {
    String message;

    if (t instanceof AmazonServiceException) {
      AmazonServiceException ase = (AmazonServiceException) t;

      message = "AWS request failure: " + ase.getErrorMessage();

      err(message);
      err("Service   :       " + ase.getServiceName());
      err("HTTP Status Code: " + ase.getStatusCode());
      err("AWS Error Code:   " + ase.getErrorCode());
      err("Error Type:       " + ase.getErrorType());
      err("Request ID:       " + ase.getRequestId());

      debug("Response content: " + ase.getRawResponseContent());

      problem(getIdentity(ase, footPrint), "CODE_DEPLOY_SERVICE", message);

    } else if (t instanceof AmazonClientException) {
      message = "Internal error while trying to communicate with AWS: " + t.getMessage();
      err(message);
      problem(getIdentity(t.getMessage(), footPrint), "CODE_DEPLOY_CLIENT", message);
    } else {
      message = "Unexpected error during deployment: " + t.getMessage();
      err(message);
      problem(getIdentity(t.getMessage(), footPrint), "CODE_DEPLOY_EXCEPTION", message);
    }
  }

  private int getIdentity(@NotNull AmazonServiceException e, @Nullable String footPrint) {
    return getIdentity(e.getServiceName(), e.getErrorType().name(), String.valueOf(e.getStatusCode()), e.getErrorCode(), footPrint);
  }

  private int getIdentity(@NotNull DeploymentInfo dInfo) {
    final ErrorInformation eInfo = dInfo.getErrorInformation();
    final S3Location s3Location = dInfo.getRevision().getS3Location();
    return getIdentity(dInfo.getStatus(), eInfo == null ? "" : eInfo.getCode(), dInfo.getApplicationName(), s3Location.getKey(), s3Location.getBucket());
  }

  private int getIdentity(String... parts) {
    final StringBuilder sb = new StringBuilder();

    for (String p : parts) {
      if (p == null) continue;

      p = p.replace("\\", "/");
      if (StringUtil.isNotEmpty(myBaseDir)) p = p.replace(myBaseDir.replace("\\", "/"), "");
      sb.append(p);
    }
    sb.append(getClientFootPrint());

    return sb.toString().replace(" ", "").toLowerCase().hashCode();
  }

  @NotNull
  private String getClientFootPrint() {
    return myRegion.getName() + (myCredentials == null ? "" : myCredentials.getAWSAccessKeyId() + myCredentials.getAWSSecretKey());
  }

  private void updateBuildStatusText(@NotNull DeploymentInfo dInfo) {
    if (!isSuccess(dInfo)) return;
    log("##teamcity[buildStatus tc:tags='tc:internal' text='{build.status.text}; Deployment succeeded: instances " + instancesText(dInfo, false) + "']");
  }

  private String instancesText(DeploymentInfo dInfo, boolean detailed) {
    final DeploymentOverview o = dInfo.getDeploymentOverview();

    if (o == null) return "<unknown>";

    final StringBuilder sb = new StringBuilder("succeeded: ").append(o.getSucceeded());
    if (o.getFailed() > 0 || detailed) sb.append(", failed: ").append(o.getFailed());
    if (o.getSkipped() > 0 || detailed) sb.append(", skipped: ").append(o.getSkipped());
    if (o.getSkipped() > 0 || detailed) sb.append(", in progress: ").append(o.getInProgress());
    return sb.toString();
  }

  private String deploymentInfoText(DeploymentInfo dInfo) {
    return "status: " + (dInfo == null ? "<unknown>" : (dInfo.getStatus() + ", instances: " + instancesText(dInfo, true)));
  }

  private boolean isSuccess(@NotNull DeploymentInfo dInfo) {
    return DeploymentStatus.Succeeded.toString().equals(dInfo.getStatus());
  }

  private String formatDuration(long milliseconds) {
    return String.format("%.1f seconds", (milliseconds / 1000d)); //TODO: better formatting
  }

  void log(@NotNull String message) { myLogger.log(message); }

  void err(@NotNull String message) { myLogger.err(message); }

  void debug(@NotNull String message) { myLogger.debug(message); }

  void problem(int identity, @NotNull String type, @NotNull String descr) { myLogger.problem(identity, type, descr); }

  @NotNull
  private String getDescription() {
    return StringUtil.emptyIfNull(myDescription);
  }

  public static class Logger {
    void log(@NotNull String message) {}
    void err(@NotNull String message) {}
    void debug(@NotNull String message) {}
    void problem(int identity, @NotNull String type, @NotNull String descr) {}
  }

  //  @NotNull
//  private String ensureS3Bucket(@NotNull String s3BucketName) {
//    final AmazonS3Client s3Client = createS3Client();
//
//    if (!s3Client.doesBucketExist(s3BucketName)) {
//      log(String.format("Creating S3 bucket %s in region %s", s3BucketName, myRegion));
//      s3Client.createBucket(s3BucketName, myRegion.getName());
//
//      final String bucketLocation = s3Client.getBucketLocation(s3BucketName);
//      log("Created S3 bucket location: " + bucketLocation);
//    }
//
//    return s3BucketName;
//  }
}

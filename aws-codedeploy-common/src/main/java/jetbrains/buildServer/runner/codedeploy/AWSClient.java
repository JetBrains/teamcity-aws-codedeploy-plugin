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
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.model.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URL;

/**
 * @author vbedrosova
 */
public class AWSClient {
  @NotNull private final AWSCredentials myCredentials;
  @NotNull private final Region myRegion;
  @Nullable private String myBaseDir;

  public AWSClient(@NotNull String accessKeyId, @NotNull String secretAccessKey, @NotNull String regionName) {
    this(new BasicAWSCredentials(accessKeyId, secretAccessKey), getRegion(regionName));
  }

  private AWSClient(@NotNull AWSCredentials credentials, @NotNull Region region) {
    myCredentials = credentials;
    myRegion = region;
  }

  @NotNull
  public AWSClient withBaseDir(@Nullable String baseDir) {
    setBaseDir(baseDir);
    return this;
  }

  private void setBaseDir(@Nullable String baseDir) {
    myBaseDir = baseDir;
  }

  @NotNull
  private AmazonS3Client createS3Client() {
    return new AmazonS3Client(myCredentials).withRegion(myRegion);
  }

  @NotNull
  private AmazonCodeDeployClient createCodeDeployClient() {
    return new AmazonCodeDeployClient(myCredentials).withRegion(myRegion);
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

  /**
   * Uploads application revision archive to S3 bucket named s3BucketName.
   *
   * For performing this operation target AWSClient must have corresponding S3 permissions.
   *
   * @param revision application revision
   * @param s3BucketName valid S3 bucket name
   */
  public void uploadRevision(@NotNull File revision, @NotNull String s3BucketName) {
    final String key = revision.getName();
    try {
      uploadRevision(revision, s3BucketName, key);
    } catch (Throwable t) {
      failBuild(t, s3BucketName.concat(key));
    }
  }

  /**
   * Uploads application revision archive to S3 bucket named s3BucketName and
   * registers it in CodeDeploy for the specified application (must be pre-configured).
   *
   * For performing this operation target AWSClient must have corresponding S3 and CodeDeploy permissions.
   *
   * @param revision application revision
   * @param s3BucketName valid S3 bucket name
   * @param  applicationName CodeDeploy application name
   */
  public void uploadAndRegisterRevision(@NotNull File revision, @NotNull String s3BucketName, @NotNull String applicationName) {
    debug("AWS region is " + myRegion.getName());
    final String key = revision.getName();
    try {
      registerRevision(uploadRevision(revision, s3BucketName, key), applicationName);
    } catch (Throwable t) {
      failBuild(t, s3BucketName.concat(key));
    }
  }

  /**
   * Uploads application revision archive to S3 bucket named s3BucketName,
   * registers it in CodeDeploy for the specified application and
   * creates deployment for specified application (must be pre-configured) to
   * deploymentGroupName (must be pre-confugured) EC2 instances group with
   * deploymentConfigName or default configuration name.
   *
   * For performing this operation target AWSClient must have corresponding S3 and CodeDeploy permissions.
   *
   * @param revision application revision
   * @param s3BucketName valid S3 bucket name
   * @param applicationName CodeDeploy application name
   * @param deploymentGroupName deployment group name
   * @param deploymentConfigName deployment configuration name or null for default deployment configuration
   */
  public void uploadAndRegisterAndDeployRevision(@NotNull File revision, @NotNull String s3BucketName,
                                      @NotNull String applicationName,
                                      @NotNull String deploymentGroupName,
                                      @Nullable String deploymentConfigName) {
    debug("AWS region is " + myRegion.getName());
    final String key = revision.getName();
    try {
      createDeployment(uploadRevision(revision, s3BucketName, key), applicationName, deploymentGroupName, deploymentConfigName);
    } catch (Throwable t) {
      failBuild(t, s3BucketName.concat(key));
    }
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

  private void failBuild(@NotNull Throwable t, String footPrint) {
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

  private int getIdentity(@NotNull AmazonServiceException e, String footPrint) {
    return getIdentity(e.getServiceName(), e.getErrorType().name(), String.valueOf(e.getStatusCode()), e.getErrorCode(), footPrint);
  }

  private int getIdentity(String... parts) {
    final StringBuilder sb = new StringBuilder();

    for (String p : parts) {
      p = p.replace("\\", "/");
      if (StringUtil.isNotEmpty(myBaseDir)) p = p.replace(myBaseDir.replace("\\", "/"), "");
      sb.append(p);
    }
    sb.append(getClientFootPrint());

    return sb.toString().replace(" ", "").toLowerCase().hashCode();
  }

  @NotNull
  private String getClientFootPrint() {
    return myRegion.getName() + myCredentials.getAWSAccessKeyId() + myCredentials.getAWSSecretKey();
  }

  protected void log(@NotNull String message) {
    System.out.println(message);
  }

  protected void err(@NotNull String message) {
    System.err.println(message);
  }

  protected void debug(@NotNull String message) {
    System.out.println(message);
  }

  protected void problem(int identity, @NotNull String type, @NotNull String descr) {
    System.out.println("Build Problem:");
    System.out.println("identity:" + identity);
    System.out.println("type:" + type);
    System.out.println("description:" + descr);
  }

  @NotNull
  protected String getDescription() {
    return getClass().getName();
  }
}

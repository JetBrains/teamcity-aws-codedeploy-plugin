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

import jetbrains.buildServer.agent.NullBuildProgressLogger;
import jetbrains.buildServer.util.amazon.AWSException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;

/**
 * @author vbedrosova
 */
public class LoggingDeploymentListenerTest extends LoggingTestCase {

  private static final String FAKE_ID = "ID-123XYZ";

  @BeforeMethod(alwaysRun = true)
  public void mySetUp() throws Exception {
    super.mySetUp();
  }

  @AfterMethod(alwaysRun = true)
  public void myTearDown() throws Exception {
    super.myTearDown();
  }

  @Test
  public void common_events() throws Exception {
    final LoggingDeploymentListener listener = create();

    final String revisionName = "revision.zip";
    final File revision = writeFile(revisionName);
    final String bucketName = "bucketName";
    final String key = "path/key.zip";
    final String eTag = "12345";

    listener.uploadRevisionStarted(revision, bucketName, key);

    final String url = "https://s3-eu-west-1.amazonaws.com/bucketName/path/key.zip";

    listener.uploadRevisionFinished(revision, bucketName, key, null, eTag, url);

    final String applicationName = "App Name";
    final String bundleType = ".zip";

    listener.registerRevisionStarted(applicationName, bucketName, key, bundleType, null, eTag);
    listener.registerRevisionFinished(applicationName, bucketName, key, bundleType, null, eTag);

    final String groupName = "Deployment Fleet";

    listener.createDeploymentStarted(applicationName, groupName, null);

    listener.createDeploymentFinished(applicationName, groupName, null, FAKE_ID);

    listener.deploymentWaitStarted(FAKE_ID);

    assertLog(
      "OPEN " + LoggingDeploymentListener.UPLOAD_REVISION,
      "LOG Uploading application revision ##BASE_DIR##/" + revisionName + " to S3 bucket " + bucketName + " using key " + key,
      "LOG Uploaded application revision " + url + "?etag=" + eTag,
      "STATUS_TEXT Uploaded " + url + "?etag=" + eTag,
      "PARAM " + CodeDeployConstants.S3_OBJECT_ETAG_CONFIG_PARAM + " -> " + eTag,
      "CLOSE " + LoggingDeploymentListener.UPLOAD_REVISION,
      "OPEN " + LoggingDeploymentListener.REGISTER_REVISION,
      "LOG Registering application " + applicationName + " revision from S3 bucket " + bucketName + " with key " + key + ", bundle type " + bundleType + ", latest version and " + eTag + " ETag",
      "STATUS_TEXT Registered revision",
      "CLOSE " + LoggingDeploymentListener.REGISTER_REVISION,
      "OPEN " + LoggingDeploymentListener.DEPLOY_APPLICATION,
      "LOG Creating application " + applicationName + " deployment to deployment group " + groupName + " with default deployment configuration",
      "PARAM " + CodeDeployConstants.DEPLOYMENT_ID_BUILD_CONFIG_PARAM + " -> " + FAKE_ID,
      "LOG Deployment " + FAKE_ID + " created",
      "LOG Waiting for deployment finish");
  }

  @Test
  public void deployment_progress_unknown() throws Exception {
    create().deploymentInProgress(FAKE_ID, createStatus());
    assertLog("PROGRESS Deployment status is unknown, 0 instances succeeded");
  }

  @Test
  public void deployment_progress() throws Exception {
    create().deploymentInProgress(FAKE_ID, createStatus("in progress", 1, 1, 1, 1, 1));
    assertLog("PROGRESS Deployment in progress, 1 instance succeeded, 1 failed, 1 pending, 1 skipped, 1 in progress");
  }

  @Test
  public void deployment_progress_finished_five_succeeded() throws Exception {
    create().deploymentInProgress(FAKE_ID, createStatus("finished", 0, 0, 5, 0, 0));
    assertLog("PROGRESS Deployment finished, 5 instances succeeded");
  }

  @Test
  public void deployment_succeeded_short() throws Exception {
    create().deploymentSucceeded(FAKE_ID, createStatus("finished", 0, 0, 5, 0, 0));
    assertLog(
      "LOG Deployment " + FAKE_ID + " finished, 5 instances succeeded, 0 failed, 0 pending, 0 skipped, 0 in progress",
      "STATUS_TEXT Deployment " + FAKE_ID + " finished, 5 instances succeeded",
      "CLOSE " + LoggingDeploymentListener.DEPLOY_APPLICATION);
  }

  @Test
  public void deployment_succeeded_long() throws Exception {
    create().deploymentSucceeded(FAKE_ID, createStatus("finished", 2, 2, 1, 2, 2));
    assertLog(
      "LOG Deployment " + FAKE_ID + " finished, 1 instance succeeded, 2 failed, 2 pending, 2 skipped, 2 in progress",
      "STATUS_TEXT Deployment " + FAKE_ID + " finished, 1 instance succeeded, 2 failed, 2 pending, 2 skipped, 2 in progress",
      "CLOSE " + LoggingDeploymentListener.DEPLOY_APPLICATION);
  }

  @Test
  public void deployment_failed_timeout() throws Exception {
    create().deploymentFailed(FAKE_ID, 2400, null, createStatus("in progress", 1, 1, 0, 0, 0));
    assertLog(
      "ERR Timeout 2400 sec exceeded, deployment " + FAKE_ID + " in progress, 0 instances succeeded, 0 failed, 1 pending, 0 skipped, 1 in progress",
      "PROBLEM identity: -1745153132 type: CODEDEPLOY_TIMEOUT descr: Timeout 2400 sec exceeded, deployment " + FAKE_ID + " in progress, 0 instances succeeded, 1 pending, 1 in progress",
      "CLOSE deploy application");
  }

  @Test
  public void deployment_failed_timeout_with_error() throws Exception {
    create().deploymentFailed(FAKE_ID, 2400, createError("abc", "Some error message"), createStatus("failed", 1, 1, 0, 1, 0));
    assertLog(
      "ERR Timeout 2400 sec exceeded, deployment " + FAKE_ID + " failed, 0 instances succeeded, 1 failed, 1 pending, 0 skipped, 1 in progress",
      "ERR Associated error: Some error message",
      "ERR Error code: abc",
      "PROBLEM identity: -116108899 type: CODEDEPLOY_TIMEOUT descr: Timeout 2400 sec exceeded, deployment " + FAKE_ID + " failed, 0 instances succeeded, 1 failed, 1 pending, 1 in progress: Some error message",
      "CLOSE deploy application");
  }

  @Test
  public void deployment_failed() throws Exception {
    create().deploymentFailed(FAKE_ID, null, createError("abc", "Some error message"), createStatus("failed", 0, 0, 0, 2, 0));
    assertLog(
      "ERR deployment " + FAKE_ID + " failed, 0 instances succeeded, 2 failed, 0 pending, 0 skipped, 0 in progress",
      "ERR Associated error: Some error message",
      "ERR Error code: abc",
      "PROBLEM identity: 448838431 type: CODEDEPLOY_FAILURE descr: deployment " + FAKE_ID + " failed, 0 instances succeeded, 2 failed: Some error message",
      "CLOSE deploy application");
  }

  @Test
  public void deployment_exception_type() throws Exception {
    create().exception(new AWSException("Some exception message", null, AWSException.EXCEPTION_BUILD_PROBLEM_TYPE, null));
    assertLog(
      "ERR Some exception message",
      "PROBLEM identity: 2086901196 type: AWS_EXCEPTION descr: Some exception message",
      "CLOSE deploy application");
  }

  @Test
  public void deployment_exception_description_type() throws Exception {
    create().exception(new AWSException("Some exception message", null, AWSException.CLIENT_PROBLEM_TYPE, "Some exception details"));
    assertLog(
      "ERR Some exception message",
      "ERR Some exception details",
      "PROBLEM identity: 2086901196 type: AWS_CLIENT descr: Some exception message",
      "CLOSE deploy application");
  }

  @Test
  public void deployment_exception_description_type_identity() throws Exception {
    create().exception(new AWSException("Some exception message", "ABC123", AWSException.CLIENT_PROBLEM_TYPE, "Some exception details"));
    assertLog(
      "ERR Some exception message",
      "ERR Some exception details",
      "PROBLEM identity: -1424436592 type: AWS_CLIENT descr: Some exception message",
      "CLOSE deploy application");
  }

  @Override
  protected void performAfterTestVerification() {
    // override parent behaviour
  }

  @NotNull
  private AWSClient.Listener.InstancesStatus createStatus(@Nullable String status, int pending, int inProgress, int succeeded, int failed, int skipped) {
    final AWSClient.Listener.InstancesStatus instancesStatus = new AWSClient.Listener.InstancesStatus();
    instancesStatus.pending = pending;
    instancesStatus.inProgress = inProgress;
    instancesStatus.succeeded = succeeded;
    instancesStatus.failed = failed;
    instancesStatus.skipped = skipped;
    if (status != null) instancesStatus.status = status;
    return instancesStatus;
  }

  @NotNull
  private AWSClient.Listener.InstancesStatus createStatus() {
    return new AWSClient.Listener.InstancesStatus();
  }

  @NotNull
  private AWSClient.Listener.ErrorInfo createError(@Nullable String code, @Nullable String message) {
    final AWSClient.Listener.ErrorInfo errorInfo = new AWSClient.Listener.ErrorInfo();
    errorInfo.code = code;
    errorInfo.message = message;
    return errorInfo;
  }

  @NotNull
  private LoggingDeploymentListener create() {
    return new LoggingDeploymentListener(Collections.<String, String>emptyMap(),
      new NullBuildProgressLogger(),
      "fake_checkout_dir") {
      @Override
      protected void debug(@NotNull String message) {
        logMessage("DEBUG " + message);
      }

      @Override
      protected void log(@NotNull String message) {
        logMessage("LOG " + message);
      }

      @Override
      protected void err(@NotNull String message) {
        logMessage("ERR " + message);
      }

      @Override
      protected void open(@NotNull String block) {
        logMessage("OPEN " + block);
      }

      @Override
      protected void close(@NotNull String block) {
        logMessage("CLOSE " + block);
      }

      @Override
      protected void parameter(@NotNull String name, @NotNull String value) {
        logMessage("PARAM " + name + " -> " + value);
      }

      @Override
      protected void problem(int identity, @NotNull String type, @NotNull String descr) {
        logMessage("PROBLEM identity: " + identity + " type: " + type + " descr: " + descr);
      }

      @Override
      protected void progress(@NotNull String message) {
        logMessage("PROGRESS " + message);
      }

      @Override
      protected void statusText(@NotNull String text) {
        logMessage("STATUS_TEXT " + text);
      }
    };
  }
}

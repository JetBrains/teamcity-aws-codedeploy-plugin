package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author vbedrosova
 */
public class LoggingDeploymentListener extends AWSClient.Listener {
  public static final String DEPLOY_APPLICATION = "deploy application";
  public static final String REGISTER_REVISION = "register revision";
  public static final String UPLOAD_REVISION = "upload revision";

  @NotNull
  private final Map<String, String> myRunnerParameters;
  @NotNull
  private final BuildProgressLogger myBuildLogger;
  @NotNull
  private final String myCheckoutDir;

  public LoggingDeploymentListener(@NotNull Map<String, String> runnerParameters, @NotNull BuildProgressLogger buildLogger, @NotNull String checkoutDir) {
    myRunnerParameters = runnerParameters;
    myBuildLogger = buildLogger;
    myCheckoutDir = checkoutDir;
  }

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
    parameter(CodeDeployConstants.DEPLOYMENT_ID_BUILD_CONFIG_PARAM, deploymentId);
    log("Deployment " + deploymentId + " created");
  }

  @Override
  void deploymentWaitStarted(@NotNull String deploymentId) {
    log("Waiting for deployment finish");
  }


  @Override
  void deploymentInProgress(@NotNull String deploymentId, @Nullable InstancesStatus instancesStatus) {
    progress(deploymentDescription(instancesStatus, false));
  }

  @Override
  void deploymentFailed(@NotNull String deploymentId, @Nullable Integer timeoutSec, @Nullable ErrorInfo errorInfo, @Nullable InstancesStatus instancesStatus) {
    String msg = (timeoutSec == null ? "" : "Timeout " + timeoutSec + "sec exceeded, ");

    err(msg + deploymentDescription(instancesStatus, true).toLowerCase());
    msg += deploymentDescription(instancesStatus, false).toLowerCase();

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

    close(DEPLOY_APPLICATION);
  }

  @Override
  void deploymentSucceeded(@NotNull String deploymentId, @Nullable InstancesStatus instancesStatus) {
    log(deploymentDescription(instancesStatus, true));
    statusText(deploymentDescription(instancesStatus, false));

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
    final StringBuilder sb = new StringBuilder("Deployment ");

    if (instancesStatus == null) sb.append(CodeDeployConstants.STATUS_IS_UNKNOWN);
    else {
      sb.append(StringUtil.isEmptyOrSpaces(instancesStatus.status) ? CodeDeployConstants.STATUS_IS_UNKNOWN : instancesStatus.status);
      sb.append(", instances succeeded: ").append(instancesStatus.succeeded);
      if (instancesStatus.failed > 0 || detailed) sb.append(", failed: ").append(instancesStatus.failed);
      if (instancesStatus.pending > 0 || detailed) sb.append(", pending: ").append(instancesStatus.pending);
      if (instancesStatus.skipped > 0 || detailed) sb.append(", skipped: ").append(instancesStatus.skipped);
      if (instancesStatus.inProgress > 0 || detailed) sb.append(", in progress: ").append(instancesStatus.inProgress);
    }

    return sb.toString();
  }

  private int getIdentity(String... parts) {
    final String baseDir = myCheckoutDir.replace("\\", "/");
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
    return CollectionsUtil.filterMapByKeys(myRunnerParameters, new Filter<String>() {
      @Override
      public boolean accept(@NotNull String data) {
        return key.contains(data);
      }
    }).values();
  }

  protected void debug(@NotNull String message) {
    myBuildLogger.message(String.format("##teamcity[message text='%s' tc:tags='tc:internal']", escape(message)));
  }

  protected void log(@NotNull String message) {
    myBuildLogger.message(message);
  }

  protected void err(@NotNull String message) {
    myBuildLogger.error(message);
  }

  protected void open(@NotNull String block) {
    myBuildLogger.targetStarted(block);
  }

  protected void close(@NotNull String block) {
    myBuildLogger.targetFinished(block);
  }

  protected void progress(@NotNull String message) {
    myBuildLogger.message(String.format("##teamcity[progressMessage '%s']", escape(message)));
  }

  protected void problem(int identity, @NotNull String type, @NotNull String descr) {
    myBuildLogger.message(String.format("##teamcity[buildProblem identity='%d' type='%s' description='%s' tc:tags='tc:internal']", identity, type, escape(descr)));
  }

  protected void parameter(@NotNull String name, @NotNull String value) {
    myBuildLogger.message(String.format("##teamcity[setParameter name='%s' value='%s' tc:tags='tc:internal']", name, value));
  }

  protected void statusText(@NotNull String text) {
    myBuildLogger.message(String.format("##teamcity[buildStatus tc:tags='tc:internal' text='{build.status.text}; %s']", text));
  }

  @NotNull
  protected String escape(@NotNull String s) {
    return s.
      replace("|", "||").
      replace("'", "|'").
      replace("\n", "|n").
      replace("\r", "|r").
      replace("\\uNNNN", "|0xNNNN").
      replace("[", "|[").replace("]", "|]");
  }
}

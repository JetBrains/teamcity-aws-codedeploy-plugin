

package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.runner.codedeploy.CodeDeployConstants.*;
import static jetbrains.buildServer.runner.codedeploy.CodeDeployUtil.*;

/**
 * @author vbedrosova
 */
final class ParametersValidator {
  /**
   * Must be used for parameters validation during the build
   * Returns map from parameter name to invalidity reason
   */
  @NotNull
  static Map<String, String> validateRuntime(@NotNull Map<String, String> runnerParams, @NotNull Map<String, String> configParams, @NotNull File checkoutDir) {
    final Map<String, String> invalids = new HashMap<String, String>(validate(runnerParams, true));

    if (!invalids.containsKey(REVISION_PATHS_PARAM) && isUploadStepEnabled(runnerParams)) {
      final String revisionPath = getReadyRevision(runnerParams.get(REVISION_PATHS_PARAM));
      if (revisionPath != null && !FileUtil.resolvePath(checkoutDir, revisionPath).exists()) {
        invalids.put(REVISION_PATHS_PARAM, REVISION_PATHS_LABEL + " " + revisionPath + " doesn't exist");
      }
    }
    return Collections.unmodifiableMap(invalids);
  }

  /**
   * Returns map from parameter name to invalidity reason
   */
  @NotNull
  static Map<String, String> validateSettings(@NotNull Map<String, String> params) {
    return validate(params, false);
  }

  private static Map<String, String> validate(@NotNull Map<String, String> runnerParams, boolean runtime) {
    final Map<String, String> invalids = new HashMap<String, String>();

    invalids.putAll(AWSCommonParams.validate(runnerParams, !runtime));

    boolean uploadStepEnabled = false;
    boolean registerStepEnabled = false;
    boolean deployStepEnabled = false;

    final String deploymentSteps = getDeploymentSteps(runnerParams);
    if (StringUtil.isEmptyOrSpaces(deploymentSteps)) {
      invalids.put(DEPLOYMENT_STEPS_PARAM, DEPLOYMENT_STEPS_LABEL + " must not be empty");
    } else {
      uploadStepEnabled = isUploadStepEnabled(runnerParams);
      registerStepEnabled = isRegisterStepEnabled(runnerParams);
      deployStepEnabled = isDeployStepEnabled(runnerParams);

      if (!uploadStepEnabled && !registerStepEnabled && !deployStepEnabled) {
        invalids.put(DEPLOYMENT_STEPS_PARAM, DEPLOYMENT_STEPS_LABEL + " has unexpected value " + deploymentSteps);
      }
    }

    if (uploadStepEnabled) {
      final String revisionPaths = getRevisionPaths(runnerParams);
      if (StringUtil.isEmptyOrSpaces(revisionPaths)) {
        invalids.put(REVISION_PATHS_PARAM, REVISION_PATHS_LABEL + " must not be empty");
      } else if (!isReference(revisionPaths, runtime)) {
        final String readyRevision = getReadyRevision(revisionPaths);
        if (readyRevision == null) {
          if (getRevisionPathMappings(revisionPaths).isEmpty()) {
            invalids.put(REVISION_PATHS_PARAM, REVISION_PATHS_LABEL + " has unexpected value, " + REVISION_PATHS_NOTE);
          }
        }
      }
    }

    if (uploadStepEnabled || registerStepEnabled || deployStepEnabled) {
      final String s3BucketName = getS3BucketName(runnerParams);
      if (StringUtil.isEmptyOrSpaces(s3BucketName)) {
        invalids.put(S3_BUCKET_NAME_PARAM, S3_BUCKET_NAME_LABEL + " must not be empty");
      } else if (s3BucketName.contains("/")) {
        invalids.put(S3_BUCKET_NAME_PARAM, S3_BUCKET_NAME_LABEL + " must not contain / characters. For addressing folders use " + S3_OBJECT_KEY_LABEL + " parameter");
      }

      final String s3ObjectKey = getS3ObjectKey(runnerParams);
      if (StringUtil.isEmptyOrSpaces(s3ObjectKey)) {
        if (!uploadStepEnabled) {
          invalids.put(S3_OBJECT_KEY_PARAM, S3_OBJECT_KEY_LABEL + " must not be empty");
        }
      } else {
        validateS3Key(invalids, s3ObjectKey, S3_OBJECT_KEY_PARAM, S3_OBJECT_KEY_LABEL, runtime);
        if (registerStepEnabled || deployStepEnabled) {
          validateBundleType(invalids, s3ObjectKey, S3_OBJECT_KEY_PARAM, S3_OBJECT_KEY_LABEL, runtime);
        }
      }
    }

    if (registerStepEnabled || deployStepEnabled) {
      if (StringUtil.isEmptyOrSpaces(getAppName(runnerParams))) {
        invalids.put(APP_NAME_PARAM, APP_NAME_LABEL + " must not be empty");
      }
    }

    if (deployStepEnabled) {
      if (StringUtil.isEmptyOrSpaces(getDeploymentGroupName(runnerParams))) {
        invalids.put(DEPLOYMENT_GROUP_NAME_PARAM, DEPLOYMENT_GROUP_NAME_LABEL + " must not be empty");
      }

      final String fileExistsParam = getFileExistsBehavior(runnerParams);
      if (StringUtil.isNotEmpty(fileExistsParam)) {
        validateFileExistsBehavior(invalids, fileExistsParam, FILE_EXISTS_BEHAVIOR_PARAM, FILE_EXISTS_BEHAVIOR_PARAM, runtime);
      }
    }
    return invalids;
  }

  private static void validatePositiveInteger(@NotNull Map<String, String> invalids, @NotNull String param, @NotNull String key, @NotNull String name, boolean runtime) {
    if (!isReference(param, runtime)) {
      try {
        final int i = Integer.parseInt(param);
        if (i <= 0) {
          invalids.put(key, name + " must be a positive integer value");
        }
      } catch (NumberFormatException e) {
        invalids.put(key, name + " must be a positive integer value");
      }
    }
  }

  private static void validateS3Key(@NotNull Map<String, String> invalids, @NotNull String param, @NotNull String key, @NotNull String name, boolean runtime) {
    if (!isReference(param, runtime)) {
      if (!param.matches("[a-zA-Z_0-9!\\-\\.*'()/]*")) {
        invalids.put(key, name + " must contain only safe characters");
      }
    }
  }

  private static void validateFileExistsBehavior(@NotNull Map<String, String> invalids, @NotNull String param, @NotNull String key, @NotNull String name, boolean runtime) {
    if (!isReference(param, runtime)) {
      String[] allowedValues = {"DISALLOW", "OVERWRITE", "RETAIN"};
      if (!Arrays.asList(allowedValues).contains(param)) {
        invalids.put(key, name + " must contain either DISALLOW, OVERWRITE, or RETAIN");
      }
    }
  }

  private static void validateBundleType(@NotNull Map<String, String> invalids, @NotNull String param, @NotNull String key, @NotNull String name, boolean runtime) {
    if (!isReference(param, runtime)) {
      if (null == getBundleType(param)) {
        invalids.put(key, name + " provides invalid bundle type, supported bundle types are .zip, .tar and .tar.gz");
      }
    }
  }

  private static boolean isReference(@NotNull String param, boolean runtime) {
    return ReferencesResolverUtil.containsReference(param, new String[]{}, true) && !runtime;
  }
}
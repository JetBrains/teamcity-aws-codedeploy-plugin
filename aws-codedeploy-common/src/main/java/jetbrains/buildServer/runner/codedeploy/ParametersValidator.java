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

import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vbedrosova
 */
public final class ParametersValidator {
  /**
   * Must be used for parameters validation during the build
   * Returns map from parameter name to invalidity reason
   */
  @NotNull
  public static Map<String, String> validateRuntime(@NotNull Map<String, String> runnerParams, @NotNull Map<String, String> configParams, @NotNull File checkoutDir) {
    final Map<String, String> invalids = new HashMap<String, String>(validate(runnerParams, true));

    if (!invalids.containsKey(CodeDeployConstants.READY_REVISION_PATH_PARAM)) {
      final String revisionPath = runnerParams.get(CodeDeployConstants.READY_REVISION_PATH_PARAM);
      if (!FileUtil.resolvePath(checkoutDir, revisionPath).exists()) {
        invalids.put(CodeDeployConstants.READY_REVISION_PATH_PARAM, "Application revision " + revisionPath + " doesn't exist");
      }
    }

    final String waitIntervalSec = configParams.get(CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM);
    if (StringUtil.isNotEmpty(waitIntervalSec)) {
      validatePositiveInteger(invalids, waitIntervalSec, CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM, CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM, true);
    }

    return invalids;
  }

  /**
   * Returns map from parameter name to invalidity reason
   */
  @NotNull
  public static Map<String, String> validateSettings(@NotNull Map<String, String> params) {
    return validate(params, false);
  }

  private static Map<String, String> validate(@NotNull Map<String, String> runnerParams, boolean runtime) {
    final Map<String, String> invalids = new HashMap<String, String>();

    final String regionName = runnerParams.get(CodeDeployConstants.REGION_NAME_PARAM);
    if (StringUtil.isEmptyOrSpaces(regionName)) {
      invalids.put(CodeDeployConstants.REGION_NAME_PARAM, CodeDeployConstants.REGION_NAME_LABEL + " mustn't be empty");
    } else {
      if (!isReference(regionName, runtime)) {
        try {
          AWSClient.getRegion(regionName);
        } catch (IllegalArgumentException e) {
          invalids.put(CodeDeployConstants.REGION_NAME_PARAM, e.getMessage());
        }
      }
    }

    if (!Boolean.parseBoolean(runnerParams.get(CodeDeployConstants.USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM))) {
      if (StringUtil.isEmptyOrSpaces(runnerParams.get(CodeDeployConstants.ACCESS_KEY_ID_PARAM))) {
        invalids.put(CodeDeployConstants.ACCESS_KEY_ID_PARAM, CodeDeployConstants.ACCESS_KEY_ID_PARAM + " mustn't be empty");
      }
      if (StringUtil.isEmptyOrSpaces(runnerParams.get(CodeDeployConstants.SECRET_ACCESS_KEY_PARAM))) {
        invalids.put(CodeDeployConstants.SECRET_ACCESS_KEY_PARAM, CodeDeployConstants.SECRET_ACCESS_KEY_LABEL + " mustn't be empty");
      }
    }

    final String credentialsType = runnerParams.get(CodeDeployConstants.CREDENTIALS_TYPE_PARAM);
    if (CodeDeployConstants.TEMP_CREDENTIALS_OPTION.equals(credentialsType)) {
      if (StringUtil.isEmptyOrSpaces(runnerParams.get(CodeDeployConstants.IAM_ROLE_ARN_PARAM))) {
        invalids.put(CodeDeployConstants.IAM_ROLE_ARN_PARAM, CodeDeployConstants.IAM_ROLE_ARN_LABEL + " mustn't be empty");
      }
    } else if (StringUtil.isEmptyOrSpaces(credentialsType)) {
      invalids.put(CodeDeployConstants.CREDENTIALS_TYPE_PARAM, CodeDeployConstants.CREDENTIALS_TYPE_LABEL + " mustn't be empty");
    } else if (!CodeDeployConstants.ACCESS_KEYS_OPTION.equals(credentialsType)) {
      invalids.put(CodeDeployConstants.CREDENTIALS_TYPE_PARAM, "Unexpected " + CodeDeployConstants.CREDENTIALS_TYPE_LABEL + " " + credentialsType);
    }

    final String revisionPath = runnerParams.get(CodeDeployConstants.READY_REVISION_PATH_PARAM);
    if (StringUtil.isEmptyOrSpaces(revisionPath)) {
      invalids.put(CodeDeployConstants.READY_REVISION_PATH_PARAM, CodeDeployConstants.READY_REVISION_PATH_LABEL + " mustn't be empty");
    } else {
      if (!isReference(revisionPath, runtime)) {
        try {
          AWSClient.getBundleType(revisionPath);
        } catch (IllegalArgumentException e) {
          invalids.put(CodeDeployConstants.READY_REVISION_PATH_PARAM, e.getMessage());
        }
      }
    }

    final String s3BucketName = runnerParams.get(CodeDeployConstants.S3_BUCKET_NAME_PARAM);
    if (StringUtil.isEmptyOrSpaces(s3BucketName)) {
      invalids.put(CodeDeployConstants.S3_BUCKET_NAME_PARAM, CodeDeployConstants.S3_BUCKET_NAME_LABEL + " mustn't be empty");
    } else if (s3BucketName.contains("/")) {
      invalids.put(CodeDeployConstants.S3_BUCKET_NAME_PARAM, CodeDeployConstants.S3_BUCKET_NAME_LABEL + " mustn't contain / characters");
    }

    final String s3ObjectKey = runnerParams.get(CodeDeployConstants.S3_OBJECT_KEY_PARAM);
    if (StringUtil.isNotEmpty(s3ObjectKey)) {
      validateS3Key(invalids, s3ObjectKey, CodeDeployConstants.S3_OBJECT_KEY_PARAM, CodeDeployConstants.S3_OBJECT_KEY_LABEL, runtime);
    }

    if (StringUtil.isEmptyOrSpaces(runnerParams.get(CodeDeployConstants.APP_NAME_PARAM))) {
      invalids.put(CodeDeployConstants.APP_NAME_PARAM, CodeDeployConstants.APP_NAME_LABEL + " mustn't be empty");
    }

    if (StringUtil.isEmptyOrSpaces(runnerParams.get(CodeDeployConstants.DEPLOYMENT_GROUP_NAME_PARAM))) {
      invalids.put(CodeDeployConstants.DEPLOYMENT_GROUP_NAME_PARAM, CodeDeployConstants.DEPLOYMENT_GROUP_NAME_LABEL + " mustn't be empty");
    }

    final String waitParam = runnerParams.get(CodeDeployConstants.WAIT_FLAG_PARAM);
    if (StringUtil.isEmptyOrSpaces(waitParam) || Boolean.parseBoolean(waitParam)) {
      final String waitTimeoutSec = runnerParams.get(CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM);
      if (StringUtil.isEmptyOrSpaces(waitTimeoutSec)) {
        invalids.put(CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM, CodeDeployConstants.WAIT_TIMEOUT_SEC_LABEL + " mustn't be empty");
      } else {
        validatePositiveInteger(invalids, waitTimeoutSec, CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM, CodeDeployConstants.WAIT_TIMEOUT_SEC_LABEL, runtime);
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

  private static boolean isReference(@NotNull String param, boolean runtime) {
    return ReferencesResolverUtil.containsReference(param) && !runtime;
  }
}

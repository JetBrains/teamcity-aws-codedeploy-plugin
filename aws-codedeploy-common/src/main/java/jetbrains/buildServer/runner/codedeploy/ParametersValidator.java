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
  public static Map<String, String> validateRuntime(@NotNull Map<String, String> params, @NotNull File checkoutDir) {
    final Map<String, String> invalids = new HashMap<String, String>(validate(params, true));

    if (!invalids.containsKey(CodeDeployConstants.READY_REVISION_PATH_PARAM)) {
      final String revisionPath = params.get(CodeDeployConstants.READY_REVISION_PATH_PARAM);
      if (!FileUtil.resolvePath(checkoutDir, revisionPath).exists()) {
        invalids.put(CodeDeployConstants.READY_REVISION_PATH_PARAM, "Application revision " + revisionPath + " not found");
      }
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

  private static Map<String, String> validate(@NotNull Map<String, String> params, boolean runtime) {
    final Map<String, String> invalids = new HashMap<String, String>();

    final String revisionPath = params.get(CodeDeployConstants.READY_REVISION_PATH_PARAM);
    if (StringUtil.isEmptyOrSpaces(revisionPath)) {
      invalids.put(CodeDeployConstants.READY_REVISION_PATH_PARAM, CodeDeployConstants.READY_REVISION_PATH_LABEL + " mustn't be empty");
    } else {
      try {
        AWSClient.getBundleType(revisionPath);
      } catch (IllegalArgumentException e) {
        if (!isReference(revisionPath, runtime)) {
          invalids.put(CodeDeployConstants.READY_REVISION_PATH_PARAM, e.getMessage());
        }
      }
    }

    final String accessKeyId = params.get(CodeDeployConstants.ACCESS_KEY_ID_PARAM);
    if (StringUtil.isEmptyOrSpaces(accessKeyId)) {
      invalids.put(CodeDeployConstants.ACCESS_KEY_ID_PARAM, CodeDeployConstants.ACCESS_KEY_ID_LABEL + " mustn't be empty");
    }

    final String secretAccessKey = params.get(CodeDeployConstants.SECRET_ACCESS_KEY_PARAM);
    if (StringUtil.isEmptyOrSpaces(secretAccessKey)) {
      invalids.put(CodeDeployConstants.SECRET_ACCESS_KEY_PARAM, CodeDeployConstants.SECRET_ACCESS_KEY_LABEL + " mustn't be empty");
    }

    final String regionName = params.get(CodeDeployConstants.REGION_NAME_PARAM);
    if (StringUtil.isEmptyOrSpaces(regionName)) {
      invalids.put(CodeDeployConstants.REGION_NAME_PARAM, CodeDeployConstants.REGION_NAME_LABEL + " mustn't be empty");
    } else {
      try {
        AWSClient.getRegion(regionName);
      } catch (IllegalArgumentException e) {
        invalids.put(CodeDeployConstants.REGION_NAME_PARAM, e.getMessage());
      }
    }

    final String s3BucketName = params.get(CodeDeployConstants.S3_BUCKET_NAME_PARAM);
    if (StringUtil.isEmptyOrSpaces(s3BucketName)) {
      invalids.put(CodeDeployConstants.S3_BUCKET_NAME_PARAM, CodeDeployConstants.S3_BUCKET_NAME_LABEL + " mustn't be empty");
    } else if (s3BucketName.contains("/")) {
      invalids.put(CodeDeployConstants.S3_BUCKET_NAME_PARAM, CodeDeployConstants.S3_BUCKET_NAME_LABEL + " mustn't contain / characters");
    }

    final String applicationName = params.get(CodeDeployConstants.APP_NAME_PARAM);
    if (StringUtil.isEmptyOrSpaces(applicationName)) {
      invalids.put(CodeDeployConstants.APP_NAME_PARAM, CodeDeployConstants.APP_NAME_LABEL + " mustn't be empty");
    }

    final String deploymentGroupName = params.get(CodeDeployConstants.DEPLOYMENT_GROUP_NAME_PARAM);
    if (StringUtil.isEmptyOrSpaces(deploymentGroupName)) {
      invalids.put(CodeDeployConstants.DEPLOYMENT_GROUP_NAME_PARAM, CodeDeployConstants.DEPLOYMENT_GROUP_NAME_LABEL + " mustn't be empty");
    }

    final boolean wait = Boolean.parseBoolean(params.get(CodeDeployConstants.WAIT_FLAG_PARAM));
    if (wait) {
      final String waitTimeoutSec = params.get(CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM);
      if (StringUtil.isEmptyOrSpaces(waitTimeoutSec)) {
        invalids.put(CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM, CodeDeployConstants.WAIT_TIMEOUT_SEC_LABEL + " mustn't be empty");
      } else {
        validatePositiveInteger(invalids, waitTimeoutSec, CodeDeployConstants.WAIT_TIMEOUT_SEC_PARAM, CodeDeployConstants.WAIT_TIMEOUT_SEC_LABEL, runtime);
      }

      final String waitIntervalSec = params.get(CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_PARAM);
      if (StringUtil.isNotEmpty(waitIntervalSec)) {
        validatePositiveInteger(invalids, waitIntervalSec, CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_PARAM, CodeDeployConstants.WAIT_POLL_INTERVAL_SEC_LABEL, runtime);
      }
    }
    return invalids;
  }

  private static void validatePositiveInteger(@NotNull Map<String, String> invalids, @NotNull String param, @NotNull String key, @NotNull String name, boolean runtime) {
    try {
      final int i = Integer.parseInt(param);
      if (i <= 0) {
        invalids.put(key, name + " must be a positive integer value");
      }
    } catch (NumberFormatException e) {
      if (!isReference(param, runtime)) {
        invalids.put(key, name + " must be a positive integer value");
      }
    }
  }

  private static boolean isReference(@NotNull String param, boolean runtime) {
    return param.contains("%") && !runtime;
  }
}

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

import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.runner.codedeploy.CodeDeployConstants.*;

/**
 * @author vbedrosova
 */
public abstract class CodeDeployUtil {
  public static boolean isUploadStepEnabled(@NotNull Map<String, String> params) {
    return isStepEnabled(UPLOAD_STEP, params);
  }

  public static boolean isRegisterStepEnabled(@NotNull Map<String, String> params) {
    return isStepEnabled(REGISTER_STEP, params);
  }

  public static boolean isDeployStepEnabled(@NotNull Map<String, String> params) {
    return isStepEnabled(DEPLOY_STEP, params);
  }

  public static boolean isDeploymentWaitEnabled(@NotNull Map<String, String> params) {
    if (isDeployStepEnabled(params)) {
      final String waitParam = params.get(WAIT_FLAG_PARAM);
      return StringUtil.isEmptyOrSpaces(waitParam) || Boolean.parseBoolean(waitParam);
    }
    return false;
  }

  public static boolean isStepEnabled(@NotNull String step, @NotNull Map<String, String> params) {
    final String steps = params.get(DEPLOYMENT_STEPS_PARAM);
    return steps != null && steps.contains(step);
  }

  @Nullable
  public static String getReadyRevision(@NotNull String revisionPathsParam) {
    final String[] split = revisionPathsParam.split(SPLIT_REGEX);
    if (split.length == 1) {
      final String revisionPath = split[0];
      if (revisionPath.contains("*")) return null;
      if (null == AWSClient.getBundleType(revisionPath)) return null;
      return revisionPath;
    }
    return null;
  }

  @Nullable
  public static List<String> getRevisionPaths(@NotNull String revisionPathsParam) {
    final String readyRevision = getReadyRevision(revisionPathsParam);
    if (readyRevision == null) {
      return Arrays.asList(revisionPathsParam.split(SPLIT_REGEX));
    }
    return null;
  }
}

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

import com.amazonaws.services.codedeploy.model.BundleType;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.PathMappings;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jetbrains.buildServer.runner.codedeploy.CodeDeployConstants.*;

/**
 * @author vbedrosova
 */
final class CodeDeployUtil {
  static boolean isUploadStepEnabled(@NotNull Map<String, String> params) {
    return isStepEnabled(UPLOAD_STEP, params);
  }

  static boolean isRegisterStepEnabled(@NotNull Map<String, String> params) {
    return isStepEnabled(REGISTER_STEP, params);
  }

  static boolean isDeployStepEnabled(@NotNull Map<String, String> params) {
    return isStepEnabled(DEPLOY_STEP, params);
  }

  static boolean isDeploymentWaitEnabled(@NotNull Map<String, String> params) {
    return isDeployStepEnabled(params) && Boolean.parseBoolean(params.get(WAIT_FLAG_PARAM));
  }

  private static boolean isStepEnabled(@NotNull String step, @NotNull Map<String, String> params) {
    final String steps = params.get(DEPLOYMENT_STEPS_PARAM);
    return steps != null && steps.contains(step);
  }

  @Nullable
  static String getReadyRevision(@NotNull String revisionPathsParam) {
    final String[] split = revisionPathsParam.trim().split(MULTILINE_SPLIT_REGEX);
    if (split.length == 1) {
      final String revisionPath = split[0];
      if (PathMappings.isWildcard(revisionPath)) return null;
      if (null == getBundleType(revisionPath)) return null;
      return revisionPath;
    }
    return null;
  }

  @NotNull
  static Map<String, String> getRevisionPathMappings(@NotNull String revisionPathsParam) {
    final String readyRevision = getReadyRevision(revisionPathsParam);
    if (readyRevision == null) {
      final Map<String, String> dest = new LinkedHashMap<String, String>();
      for (String path : revisionPathsParam.trim().split(MULTILINE_SPLIT_REGEX)) {
        final String[] parts = path.split(PATH_SPLIT_REGEX);
        if (parts.length > 0) {
          dest.put(
            normalize(parts[0], true),
            parts.length == 1 ? StringUtil.EMPTY : normalize(parts[1], false));
        }
      }
      return Collections.unmodifiableMap(dest);
    }
    return Collections.<String, String>emptyMap();
  }

  @NotNull
  private static String normalize(@NotNull String path, boolean isFromPart) {
    path = StringUtil.removeLeadingSlash(FileUtil.toSystemIndependentName(path));
    final String suffix = isFromPart && path.endsWith("/") ? "/" : StringUtil.EMPTY;
    path = FileUtil.normalizeRelativePath(path);
    return StringUtil.isEmpty(path) && isFromPart ? "**" : path + suffix;
  }

  @NotNull
  static String printStrings(@NotNull Collection<String> strings) {
    if (strings.isEmpty()) return StringUtil.EMPTY;
    final StringBuilder sb = new StringBuilder();
    for (String s : strings) sb.append(s).append("\n");
    return sb.toString();
  }

  /* Borrowed from jetbrains.buildServer.util.StringUtil.truncateStringValueWithDotsAtCenter
  * TODO: use the original util method */
  @Contract("null, _ -> null")
  public static String truncateStringValueWithDotsAtCenter(@Nullable final String str, final int maxLength) {
    if (str == null) return null;
    if (str.length() > maxLength) {
      String start = str.substring(0, maxLength / 2);
      String dots = "...";
      String end = str.substring(str.length() - maxLength + start.length() + dots.length(), str.length());
      return start + dots + end;
    } else {
      return str;
    }
  }

  @NotNull
  public static Collection<String> getAutoScalingGroups(@NotNull Map<String, String> params) {
    final String deploymentInstances = params.get(GREEN_FLEET_PARAM);
    if (StringUtil.isEmptyOrSpaces(deploymentInstances)) return Collections.emptyList();

    final List<String> ec2Tags = new ArrayList<String>();
    for (String s : deploymentInstances.trim().split(MULTILINE_SPLIT_REGEX)) {
      if (s.contains(" ")) continue;
      ec2Tags.add(s);
    }
    return ec2Tags;
  }

  @NotNull
  public static Map<String, String> getEC2Tags(@NotNull Map<String, String> params) {
    final String deploymentInstances = params.get(GREEN_FLEET_PARAM);
    if (StringUtil.isEmptyOrSpaces(deploymentInstances)) return Collections.emptyMap();

    final Map<String, String> autoScalingGroups = new HashMap<String, String>();
    for (String s : deploymentInstances.trim().split(MULTILINE_SPLIT_REGEX)) {
      if (s.contains(" ")) {
        final List<String> res = StringUtil.split(deploymentInstances, " ");
        if (res.size() < 2) continue;
        if (res.size() > 2) {
          // report somehow
        }
        autoScalingGroups.put(res.get(0).trim(), res.get(1).trim());
      }
    }
    return autoScalingGroups;
  }

  @Nullable
  public static String getBundleType(@NotNull String revision) {
    if (revision.endsWith(".zip")) return BundleType.Zip.name();
    if (revision.endsWith(".tar")) return BundleType.Tar.name();
    if (revision.endsWith(".tar.gz") || revision.endsWith(".tgz")) return BundleType.Tgz.name();
    return null;
  }
}

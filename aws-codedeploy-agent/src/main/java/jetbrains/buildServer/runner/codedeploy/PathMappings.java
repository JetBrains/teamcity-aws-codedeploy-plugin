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

import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.pathMatcher.AntPatternFileCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author vbedrosova
 */
class PathMappings {
  @NotNull
  private final File myBaseDir;
  @NotNull
  private final Map<String, String> myPathMappings;
  @NotNull
  private final List<String> myPaths;

  PathMappings(@NotNull File baseDir, @NotNull Map<String, String> pathMappings) {
    myBaseDir = baseDir;
    myPathMappings = filterEmptyMappingsAndRemoveWildCards(pathMappings);
    myPaths = new ArrayList<>(pathMappings.keySet());
  }

  @NotNull
  List<File> collectFiles() {
    return AntPatternFileCollector.scanDir(myBaseDir, CollectionsUtil.toStringArray(myPaths), null);
  }

  @Nullable
  String mapPath(@NotNull File f) {
    final String relativePath = FileUtil.getRelativePath(myBaseDir, f);

    if (relativePath == null) return null;

    for (Map.Entry<String, String> mapping : myPathMappings.entrySet()) {
      if (StringUtil.isEmpty(mapping.getKey())) return relativePath;
      if (relativePath.equals(mapping.getKey())) return doMap(f.getName(), mapping.getValue());

      final String commonPrefix = StringUtil.commonPrefix(relativePath, mapping.getKey());
      if (StringUtil.isEmpty(commonPrefix)) continue;
      return doMap(relativePath.substring(commonPrefix.length()), mapping.getValue());
    }
    return relativePath;
  }

  @NotNull
  private String doMap(@NotNull String path, @NotNull String dest) {
    return (".".equals(dest) ? StringUtil.EMPTY : dest + "/") + path;
  }

  @NotNull
  private Map<String, String> filterEmptyMappingsAndRemoveWildCards(@NotNull Map<String, String> pathMappings) {
    final Map<String, String> res = new LinkedHashMap<>();
    for (Map.Entry<String, String> mapping : pathMappings.entrySet()) {
      if (StringUtil.isEmpty(mapping.getValue())) continue;
      res.put(removeWildCards(mapping.getKey()), mapping.getValue());
    }
    return res;
  }

  @NotNull
  private String removeWildCards(@NotNull String path) {
    final int firstStar = path.indexOf('*');
    final int firstQuest = path.indexOf('?');

    if (firstStar < 0 && firstQuest < 0) return path;

    int mark = firstStar < 0 ? firstQuest : (firstStar < firstQuest || firstQuest < 0 ? firstStar : firstQuest);
    final int lastSlash = path.lastIndexOf('/', mark);
    return lastSlash > 0 ? path.substring(0, lastSlash + 1) : StringUtil.EMPTY;
  }
}

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

  PathMappings(@NotNull File baseDir, @NotNull Map<String, String> pathMappings) {
    myBaseDir = baseDir;
    myPathMappings = pathMappings;
  }

  @NotNull
  List<File> collectFiles() {
    return doCollectFiles(myPathMappings.keySet());
  }

  @NotNull
  private List<File> doCollectFiles(@NotNull Set<String> paths) {
    return AntPatternFileCollector.scanDir(myBaseDir, CollectionsUtil.toStringArray(paths), new AntPatternFileCollector.ScanOption[]{AntPatternFileCollector.ScanOption.NOT_FOLLOW_SYMLINK_DIRS, AntPatternFileCollector.ScanOption.INCLUDE_ALL_IF_NO_RULES});
  }

  private boolean matches(@NotNull String rule, @NotNull File file) {
    return doCollectFiles(Collections.<String>singleton(rule)).contains(file);
  }

  @Nullable
  String mapPath(@NotNull File f) {
    String relativePath = FileUtil.getRelativePath(myBaseDir, f);

    if (relativePath == null) return null;

    relativePath = FileUtil.toSystemIndependentName(relativePath);

    String result = null;
    for (Map.Entry<String, String> m : myPathMappings.entrySet()) {
      if (relativePath.equals(m.getKey())) return doMap(f.getName(), m.getValue());

      if (relativePath.startsWith(m.getKey())) {
        result = doMap(relativePath.substring(StringUtil.commonPrefix(relativePath, m.getKey()).length()), m.getValue());
        continue;
      }

      if (CodeDeployUtil.isWildcard(m.getKey()) && matches(m.getKey(), f)) {
        final String withoutWildcards = removeWildcards(m.getKey());
        result = doMap(
          StringUtil.isEmpty(withoutWildcards) ?
            relativePath :
            relativePath.substring(relativePath.lastIndexOf(withoutWildcards) + withoutWildcards.length()),
          m.getValue());
      }
    }
    return result == null ? relativePath : result;
  }

  @NotNull
  private String doMap(@NotNull String path, @NotNull String dest) {
    return (StringUtil.isEmpty(dest) ? StringUtil.EMPTY : dest + "/") + path;
  }

  @NotNull
  private String removeWildcards(@NotNull String path) {
    final int lastMark = Math.max(path.lastIndexOf('*'), path.lastIndexOf('?'));
    if (lastMark < 0) return path;

    int slash = path.indexOf('/', lastMark);
    if (slash < 0 || path.length() - slash < 2) {
      slash = path.lastIndexOf('/', lastMark);
      return slash > 0 ? removeWildcards(path.substring(0, slash + 1)) : StringUtil.EMPTY;
    }
    return path.substring(slash);
  }
}

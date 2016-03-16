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

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.filters.Filter;
import jetbrains.buildServer.util.pathMatcher.AntPatternFileCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author vbedrosova
 */
public class ApplicationRevision {
  @NotNull
  private final String myName;
  @NotNull
  private final String myPaths;
  @NotNull
  private final File myBaseDir;
  @NotNull
  private final File myTempDir;
  @Nullable
  private final String myCustomAppSpecContent;
  @Nullable
  private BuildProgressLogger myLogger;

  public ApplicationRevision(@NotNull String name, @NotNull String paths, @NotNull File baseDir, @NotNull File tempDir, @Nullable String customAppSpecContent) {
    myName = name;
    myPaths = paths;
    myBaseDir = baseDir;
    myTempDir = tempDir;
    myCustomAppSpecContent = customAppSpecContent;
  }

  @NotNull
  public File getArchive() throws CodeDeployRunner.CodeDeployRunnerException {
    final String readyRevisionPath = CodeDeployConstants.getReadyRevision(myPaths);
    return readyRevisionPath == null ? packZip() : FileUtil.resolvePath(myBaseDir, readyRevisionPath);
  }

  @NotNull
  private File packZip() throws CodeDeployRunner.CodeDeployRunnerException {
    @SuppressWarnings("ConstantConditions") final List<File> files =
      new ArrayList<File>(AntPatternFileCollector.scanDir(myBaseDir, CollectionsUtil.toStringArray(CodeDeployConstants.getRevisionPaths(myPaths)), null));

    if (files.isEmpty()) {
      throw new CodeDeployRunner.CodeDeployRunnerException("No " + CodeDeployConstants.REVISION_PATHS_PARAM + " files found", null);
    }
    return zipFiles(patchAppspecYml(files), new File(myTempDir, myName.endsWith(".zip") ? myName : myName + ".zip"));
  }

  @NotNull
  private List<File> patchAppspecYml(@NotNull List<File> files) throws CodeDeployRunner.CodeDeployRunnerException {
    final File appSpecYml = CollectionsUtil.<File>findFirst(files, new Filter<File>() {
      @Override
      public boolean accept(@NotNull File data) {
        return CodeDeployConstants.APPSPEC_YML.equals(FileUtil.getRelativePath(myBaseDir, data));
      }
    });

    if (StringUtil.isNotEmpty(myCustomAppSpecContent)) {
      final File customAppSpecYml = customAppSpecYmlFile();

      try {
        FileUtil.writeFile(customAppSpecYml, myCustomAppSpecContent, "UTF-8");
      } catch (IOException e) {
        throw new CodeDeployRunner.CodeDeployRunnerException("Failed to write custom " + CodeDeployConstants.APPSPEC_YML, e);
      }

      if (null == appSpecYml) {
        log("Will use custom " + CodeDeployConstants.APPSPEC_YML + " " + customAppSpecYml);
      } else {
        log("Will replace existing " + CodeDeployConstants.APPSPEC_YML + " " + appSpecYml + " with custom " + customAppSpecYml);
        files.remove(appSpecYml);
      }
      files.add(customAppSpecYml);

    } else if (null == appSpecYml) {
      throw new CodeDeployRunner.CodeDeployRunnerException("No " + CodeDeployConstants.APPSPEC_YML + " file found among " + CodeDeployConstants.REVISION_PATHS_PARAM + " files", null);
    }
    return files;
  }

  @NotNull
  private File customAppSpecYmlFile() {
    return new File(myTempDir, CodeDeployConstants.APPSPEC_YML);
  }

  @NotNull
  private File zipFiles(@NotNull List<File> files, @NotNull File destZip) throws CodeDeployRunner.CodeDeployRunnerException {
    log("Adding " + files.size() + " files to application revision " + destZip);

    ZipOutputStream zipOutput = null;
    try {
      zipOutput = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
      byte[] buffer = new byte[64 * 1024];

      for (File f : files) {

        final ZipEntry zipEntry = new ZipEntry(getZipPath(f));
        zipEntry.setTime(f.lastModified());
        zipOutput.putNextEntry(zipEntry);

        final InputStream input = new BufferedInputStream(new FileInputStream(f));

        try {
          int read;
          do {
            read = input.read(buffer);
            zipOutput.write(buffer, 0, Math.max(read, 0));
          } while (read == buffer.length);
        } catch (IOException e) {
          throw new CodeDeployRunner.CodeDeployRunnerException("Failed to add file " + f + " to application revision " + destZip, e);
        } finally {
          FileUtil.close(input);
          zipOutput.closeEntry();
        }
      }
    } catch (Throwable t) {
      if (t instanceof CodeDeployRunner.CodeDeployRunnerException) {
        throw (CodeDeployRunner.CodeDeployRunnerException) t;
      }
      throw new CodeDeployRunner.CodeDeployRunnerException("Failed to package files to application revision " + destZip, t);
    } finally {
      FileUtil.close(zipOutput);
    }
    return destZip;
  }

  @NotNull
  private String getZipPath(@NotNull File f) throws CodeDeployRunner.CodeDeployRunnerException {
    if (customAppSpecYmlFile().equals(f)) return CodeDeployConstants.APPSPEC_YML;

    final String relativePath = FileUtil.getRelativePath(myBaseDir, f);
    if(relativePath  == null) throw new CodeDeployRunner.CodeDeployRunnerException("Unexpected application revision file " + f, null);
    return relativePath;
  }

  @NotNull
  public ApplicationRevision withLogger(@Nullable BuildProgressLogger logger) {
    myLogger = logger;
    return this;
  }

  private void log(@NotNull String m) {
    if (myLogger == null) return;
    myLogger.message(m);
  }
}

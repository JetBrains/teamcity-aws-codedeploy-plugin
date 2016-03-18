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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
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
  private final String myCustomAppSpec;
  @NotNull
  private final PathMappings myPathMappings;
  @Nullable
  private BuildProgressLogger myLogger;

  ApplicationRevision(@NotNull String name, @NotNull String paths, @NotNull File baseDir, @NotNull File tempDir, @Nullable String customAppSpecContent) {
    myName = name;
    myPaths = paths;
    myBaseDir = baseDir;
    myTempDir = tempDir;
    myCustomAppSpec = customAppSpecContent;

    myPathMappings = new PathMappings(myBaseDir, CodeDeployUtil.getRevisionPathMappings(myPaths));
  }

  @NotNull
  File getArchive() throws CodeDeployRunner.CodeDeployRunnerException {
    final String readyRevisionPath = CodeDeployUtil.getReadyRevision(myPaths);
    return readyRevisionPath == null ? packZip() : FileUtil.resolvePath(myBaseDir, readyRevisionPath);
  }

  @NotNull
  private File packZip() throws CodeDeployRunner.CodeDeployRunnerException {
    final List<File> files = new ArrayList<File>(myPathMappings.collectFiles());

    if (files.isEmpty()) {
      throw new CodeDeployRunner.CodeDeployRunnerException("No " + CodeDeployConstants.REVISION_PATHS_LABEL.toLowerCase() + " files found", null);
    }
    return zipFiles(patchAppSpecYml(files), new File(myTempDir, myName.endsWith(".zip") ? myName : myName + ".zip"));
  }

  @NotNull
  private List<File> patchAppSpecYml(@NotNull List<File> files) throws CodeDeployRunner.CodeDeployRunnerException {
    final File appSpecYml = CollectionsUtil.<File>findFirst(files, new Filter<File>() {
      @Override
      public boolean accept(@NotNull File data) {
        return CodeDeployConstants.APPSPEC_YML.equals(myPathMappings.mapPath(data));
      }
    });

    final File customAppSpecYml = getCustomAppSpecYmlFile();
    if (customAppSpecYml != null) {
      if (null == appSpecYml) {
        log("Will use custom AppSpec file " + customAppSpecYml);
      } else {
        log("Will replace existing AppSpec file " + appSpecYml + " with custom " + customAppSpecYml);
        files.remove(appSpecYml);
      }
      files.add(customAppSpecYml);

    } else if (null == appSpecYml) {
      throw new CodeDeployRunner.CodeDeployRunnerException("No " + CodeDeployConstants.APPSPEC_YML + " file found among " + CodeDeployConstants.REVISION_PATHS_LABEL.toLowerCase() + " files and no custom AppSpec file provided", null);
    }
    return files;
  }

  @Nullable
  private File getCustomAppSpecYmlFile() throws CodeDeployRunner.CodeDeployRunnerException {
    if (StringUtil.isEmptyOrSpaces(myCustomAppSpec)) return null;

    if (myCustomAppSpec.endsWith(CodeDeployConstants.APPSPEC_YML)) {
      return FileUtil.resolvePath(myBaseDir, myCustomAppSpec);
    }

    final File customAppSpecYml = new File(myTempDir, CodeDeployConstants.APPSPEC_YML);
    if (!customAppSpecYml.isFile()) {
      try {
        FileUtil.writeFile(customAppSpecYml, "" + myCustomAppSpec, "UTF-8");
      } catch (IOException e) {
        throw new CodeDeployRunner.CodeDeployRunnerException("Failed to write custom " + CodeDeployConstants.APPSPEC_YML, e);
      }
    }
    return customAppSpecYml;
  }

  @NotNull
  private File zipFiles(@NotNull List<File> files, @NotNull File destZip) throws CodeDeployRunner.CodeDeployRunnerException {
    log("Packaging " + files.size() + " files to application revision " + destZip);

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
          throw new CodeDeployRunner.CodeDeployRunnerException("Failed to package file " + f + " to application revision " + destZip, e);
        } finally {
          FileUtil.close(input);
          zipOutput.closeEntry();
        }
      }
    } catch (Throwable t) {
      if (t instanceof CodeDeployRunner.CodeDeployRunnerException) {
        throw (CodeDeployRunner.CodeDeployRunnerException) t;
      }
      throw new CodeDeployRunner.CodeDeployRunnerException("Failed to package application revision " + destZip, t);
    } finally {
      FileUtil.close(zipOutput);
    }
    return destZip;
  }

  @NotNull
  private String getZipPath(@NotNull File f) throws CodeDeployRunner.CodeDeployRunnerException {
    if (f.equals(getCustomAppSpecYmlFile())) return CodeDeployConstants.APPSPEC_YML;

    final String zipPath = myPathMappings.mapPath(f);
    if(zipPath  == null) throw new CodeDeployRunner.CodeDeployRunnerException("Unexpected application revision file " + f, null);
    return zipPath;
  }

  @NotNull
  ApplicationRevision withLogger(@Nullable BuildProgressLogger logger) {
    myLogger = logger;
    return this;
  }

  private void log(@NotNull String m) {
    if (myLogger == null) return;
    myLogger.message(m);
  }
}

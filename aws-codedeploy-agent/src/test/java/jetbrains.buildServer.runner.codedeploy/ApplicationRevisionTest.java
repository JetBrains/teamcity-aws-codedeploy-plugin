

package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.agent.NullBuildProgressLogger;
import jetbrains.buildServer.util.ArchiveUtil;
import jetbrains.buildServer.util.FileUtil;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import static jetbrains.buildServer.runner.codedeploy.CodeDeployRunner.CodeDeployRunnerException;
import static org.assertj.core.api.BDDAssertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author vbedrosova
 */
public class ApplicationRevisionTest extends LoggingTestCase {

  @BeforeMethod(alwaysRun = true)
  public void mySetUp() throws Exception {
    super.mySetUp();
  }

  @AfterMethod(alwaysRun = true)
  public void myTearDown() throws Exception {
    super.myTearDown();
  }

  @Test
  public void ready_revision_zip() throws Exception {
    ready_revision_arch(".zip");
  }

  @Test
  public void ready_revision_tar() throws Exception {
    ready_revision_arch(".tar");
  }

  @Test
  public void ready_revision_tar_gz() throws Exception {
    ready_revision_arch(".tar.gz");
  }

  private void ready_revision_arch(@NotNull String ext) throws Exception {
    final String path = "some/path/readyRevision" + ext;
    final File readyRevision = writeFile(path);

    then(create(path).getArchive()).as("Unexpected revision").isEqualTo(readyRevision);
  }

  @Test
  public void ready_revision_absolute_path() throws Exception {
    final File readyRevision = writeTempFile("some/path/readyRevision.zip");
    then(create(readyRevision.getAbsolutePath()).getArchive()).as("Unexpected revision").isEqualTo(readyRevision);
  }

  @Test
  public void no_files_found() throws Exception {
    try {
      create(REVISION_PATHS).getArchive();
      failBecauseExceptionWasNotThrown(CodeDeployRunnerException.class);
    } catch (CodeDeployRunnerException e) {
      Assertions.assertThat(e).hasMessage("No " + CodeDeployConstants.REVISION_PATHS_LABEL.toLowerCase() + " files found");
    }
  }

  @Test
  public void no_appspec_yml_found() throws Exception {
    try {
      fillBaseDir(false);

      create(REVISION_PATHS).getArchive();
      failBecauseExceptionWasNotThrown(CodeDeployRunnerException.class);
    } catch (CodeDeployRunnerException e) {
      Assertions.assertThat(e).hasMessage("No appspec.yml file found among application revision files and no custom AppSpec file provided");
    }
  }

  @Test
  public void no_appspec_yml_found_custom_provided() throws Exception {
    fillBaseDir(false);

    assertRevision(create(REVISION_PATHS, CAC).getArchive(), RESULT_PATHS, CAC);

    assertLog(
      "Will use custom AppSpec file ##TEMP_DIR##/appspec.yml",
      "Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void no_appspec_yml_found_custom_path_provided() throws Exception {
    fillBaseDir(false);
    writeFile("another/path/appspec.yml", CAC);

    assertRevision(create(REVISION_PATHS, "another/path/appspec.yml").getArchive(), RESULT_PATHS, CAC);

    assertLog(
      "Will use custom AppSpec file ##BASE_DIR##/another/path/appspec.yml",
      "Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void no_appspec_yml_found_custom_absolute_path_provided() throws Exception {
    fillBaseDir(false);

    assertRevision(create(REVISION_PATHS, writeTempFile("some/path/appspec.yml", CAC).getAbsolutePath()).getArchive(), RESULT_PATHS, CAC);

    assertLog(
      "Will use custom AppSpec file ##TEMP_DIR##/some/path/appspec.yml",
      "Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void with_appspec_yml_custom_provided() throws Exception {
    fillBaseDir(true);

    assertRevision(create(REVISION_PATHS, CAC).getArchive(), RESULT_PATHS, CAC);

    assertLog(
      "Will replace existing AppSpec file ##BASE_DIR##/appspec.yml with custom ##TEMP_DIR##/appspec.yml",
      "Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void with_appspec_yml_custom_path_provided() throws Exception {
    fillBaseDir(true);
    writeFile("another/path/appspec.yml", CAC);

    assertRevision(create(REVISION_PATHS, "another/path/appspec.yml").getArchive(), RESULT_PATHS, CAC);

    assertLog(
      "Will replace existing AppSpec file ##BASE_DIR##/appspec.yml with custom ##BASE_DIR##/another/path/appspec.yml",
      "Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void with_appspec_yml_custom_absolute_path_provided() throws Exception {
    fillBaseDir(true);

    assertRevision(create(REVISION_PATHS, writeTempFile("some/path/appspec.yml", CAC).getAbsolutePath()).getArchive(), RESULT_PATHS, CAC);

    assertLog(
      "Will replace existing AppSpec file ##BASE_DIR##/appspec.yml with custom ##TEMP_DIR##/some/path/appspec.yml",
      "Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void with_appspec_yml() throws Exception {
    fillBaseDir(true);

    final File revision = create(REVISION_PATHS).getArchive();
    then(revision).as("Unexpected revision").isEqualTo(getCustomRevision("test_revision.zip"));
    assertRevision(revision, RESULT_PATHS, AC);

    assertLog("Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void wildcard_two_stars() throws Exception {
    fillBaseDir(true);

    assertRevision(create("some/path/**,appspec.yml").getArchive(), RESULT_PATHS, AC);

    assertLog("Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void wildcard_include_all() throws Exception {
    fillBaseDir(true);

    assertRevision(create("**").getArchive(), arr("some/path/index.html", "some/path/inner/path/error.html", "some/path/inner/path/test/test.html", "another/path/index.html", "another/path/inner/path/error.html", "another/path/inner/path/test/test.html", "appspec.yml"), AC);

    assertLog("Packaging 7 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void simple_paths() throws Exception {
    fillBaseDir(true);

    assertRevision(create("some/path/index.html,some/path/inner/path/error.html,some/path/inner/path/test/test.html,appspec.yml").getArchive(), arr("index.html", "error.html", "test.html", "appspec.yml"), AC);

    assertLog("Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void simple_dir() throws Exception {
    fillBaseDir(true);

    assertRevision(create("some/path/inner/path/,appspec.yml").getArchive(), arr("error.html", "test/test.html", "appspec.yml"), AC);

    assertLog("Packaging 3 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void simple_paths_with_mapping() throws Exception {
    fillBaseDir(false);
    writeFile("another/path/appspec.yml", AC);

    assertRevision(create("some/path/index.html=>pages/dist,some/path/inner/path/error.html=>pages/dist/error,some/path/inner/path/test/test.html=>pages,another/path/appspec.yml => .").getArchive(), arr("pages/dist/index.html", "pages/dist/error/error.html", "pages/test.html", "appspec.yml"), AC);

    assertLog("Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void simple_paths_with_mapping_with_slashes() throws Exception {
    fillBaseDir(false);
    writeFile("another/path/appspec.yml", AC);

    assertRevision(create("some/path/index.html=>/pages/dist,some/path/inner/path/error.html=>pages/dist/error/,some/path/inner/path/test/test.html=>/pages/,another/path/appspec.yml => .").getArchive(), arr("pages/dist/index.html", "pages/dist/error/error.html", "pages/test.html", "appspec.yml"), AC);

    assertLog("Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void simple_dir_with_mapping() throws Exception {
    fillBaseDir(true);

    assertRevision(create("some/path/=>pages/dist,some/path/inner/path/=>pages/dist/error,some/path/inner/path/test/=>pages,appspec.yml").getArchive(), arr("pages/dist/index.html", "pages/dist/error/error.html", "pages/test.html", "appspec.yml"), AC);

    assertLog("Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void wildcard_two_stars_with_mapping() throws Exception {
    fillBaseDir(true);

    assertRevision(create("some/path/**=>pages/dist,some/path/inner/path/test/**=>pages,appspec.yml => .").getArchive(), arr("pages/dist/index.html", "pages/dist/inner/path/error.html", "pages/test.html", "appspec.yml"), AC);

    assertLog("Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

//  TW-45267
  @Test
  public void wildcard_two_stars_with_mapping_with_plus_prefix() throws Exception {
    fillBaseDir(true);

    assertRevision(create("+:some/path/**=>pages/dist,+:some/path/inner/path/test/**=>pages,+:appspec.yml").getArchive(), arr("pages/dist/index.html", "pages/dist/inner/path/error.html", "pages/test.html", "appspec.yml"), AC);

    assertLog("Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void wildcard_three_stars_with_mapping() throws Exception {
    fillBaseDir(true);

    assertRevision(create("some/path/**/*.html=>pages/dist,some/path/inner/path/test/**/*.html=>pages,appspec.yml => .").getArchive(), arr("pages/dist/index.html", "pages/dist/inner/path/error.html", "pages/test.html", "appspec.yml"), AC);

    assertLog("Packaging 4 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void wildcard_include_all_with_mapping() throws Exception {
    fillBaseDir(true);

    assertRevision(create("** => pages/dist,some/path/**/test/** => pages,another/path/**/test/** => .,appspec.yml => .").getArchive(), arr("pages/dist/some/path/index.html", "pages/dist/some/path/inner/path/error.html", "pages/test.html", "pages/dist/another/path/index.html", "pages/dist/another/path/inner/path/error.html", "test.html", "appspec.yml"), AC);

    assertLog("Packaging 7 files to application revision ##TEMP_DIR##/test_revision.zip");
  }


  @Test
  public void only_dot() throws Exception {
    fillBaseDir(true);

    assertRevision(create(".").getArchive(), arr("some/path/index.html", "some/path/inner/path/error.html", "some/path/inner/path/test/test.html", "another/path/index.html", "another/path/inner/path/error.html", "another/path/inner/path/test/test.html", "appspec.yml"), AC);

    assertLog("Packaging 7 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void empty_from() throws Exception {
    fillBaseDir(true);

    assertRevision(create("=>dist,appspec.yml").getArchive(), arr("dist/some/path/index.html", "dist/some/path/inner/path/error.html", "dist/some/path/inner/path/test/test.html", "dist/another/path/index.html", "dist/another/path/inner/path/error.html", "dist/another/path/inner/path/test/test.html", "appspec.yml"), AC);

    assertLog("Packaging 7 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  @Test
  public void no_false_matches() throws Exception {
    fillBaseDir(true);
    writeFile("another_file");

    assertRevision(create("another/path/**=>dist , appspec.yml, another_file ").getArchive(), arr("dist/index.html", "dist/inner/path/error.html", "dist/inner/path/test/test.html", "appspec.yml", "another_file"), AC);

    assertLog("Packaging 5 files to application revision ##TEMP_DIR##/test_revision.zip");
  }

  private void fillBaseDir(boolean withAppSpecFile) throws IOException {
    writeFile("some/path/index.html");
    writeFile("some/path/inner/path/error.html");
    writeFile("some/path/inner/path/test/test.html");
    writeFile("another/path/index.html");
    writeFile("another/path/inner/path/error.html");
    writeFile("another/path/inner/path/test/test.html");
    if (withAppSpecFile) writeFile("appspec.yml", AC);
  }

  private void assertRevision(@NotNull File revision, @NotNull String[] paths, @NotNull String expectedAppSpecYmlContent) throws IOException {
    final File res = unpackZip(revision);
    final LinkedList<String> resPaths = new LinkedList<String>();
    collectPaths(res, res, resPaths);
    then(resPaths).as("Unexpected number of files in zip").hasSize(paths.length);
    for (String p : paths) {
      then(resPaths).as("zip must contain " + p).contains(p);
    }
    then(FileUtil.readText(new File(res, new File("appspec.yml").getName()))).as("Unexpected appspec.yml content").isEqualTo(expectedAppSpecYmlContent);
  }

  @NotNull
  private File unpackZip(@NotNull File zip) throws IOException {
    final File tempDir = createTempDir();
    ArchiveUtil.unpackZip(zip, tempDir);
    return tempDir;
  }

  private void collectPaths(@NotNull File f, @NotNull File baseDir, @NotNull LinkedList<String> paths) throws IOException {
    if (f.isFile()) paths.add(FileUtil.toSystemIndependentName(FileUtil.getRelativePath(baseDir, f)));
    if (f.isDirectory()) {
      final File[] files = f.listFiles();
      if (files == null ||  files.length == 0) return;
      for (File child : files) {
        collectPaths(child, baseDir, paths);
      }
    }
  }

  @NotNull
  private File getCustomRevision(@NotNull String name) {
    return new File(getTempDir(), name);
  }

  @NotNull
  private ApplicationRevision create(@NotNull String paths) {
    return create(paths, null);
  }

  @NotNull
  private ApplicationRevision create(@NotNull String paths, @Nullable String customAppSpec) {
    return new ApplicationRevision("test_revision", paths, getBaseDir(), getTempDir(), customAppSpec, true).withLogger(new NullBuildProgressLogger() {
      @Override
      public void message(String message) {
        ApplicationRevisionTest.this.logMessage(message);
      }
    });
  }

  @NotNull
  private static String[] arr(String... strings) {
    return strings;
  }

  private static final String CAC = "CUSTOM_APPSPEC_CONTENT";
  private static final String AC = "APPSPEC_CONTENT";
  private static final String REVISION_PATHS = "some/path/**/*.html\nappspec.yml";
  private static final String[] RESULT_PATHS = arr("index.html", "inner/path/error.html", "inner/path/test/test.html", "appspec.yml");
}
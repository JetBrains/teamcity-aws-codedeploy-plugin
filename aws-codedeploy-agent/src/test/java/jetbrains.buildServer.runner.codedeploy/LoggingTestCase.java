

package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import static org.assertj.core.api.BDDAssertions.*;

/**
 * @author vbedrosova
 */
abstract class LoggingTestCase extends BaseTestCase {
  @NotNull
  private final LinkedList<String> myLog = new LinkedList<String>();
  private File myTempDir;
  private File myBaseDir;

  @BeforeMethod(alwaysRun = true)
  public void mySetUp() throws Exception {
    myTempDir = createTempDir();
    myBaseDir = createTempDir();
  }

  @AfterMethod(alwaysRun = true)
  public void myTearDown() throws Exception {
    myLog.clear();
  }

  protected File getTempDir() {
    return myTempDir;
  }

  protected File getBaseDir() {
    return myBaseDir;
  }

  protected void logMessage(@NotNull String msg) {
    myLog.addLast(msg.replace(myBaseDir.getAbsolutePath(), "##BASE_DIR##").replace(myTempDir.getAbsolutePath(), "##TEMP_DIR##").replace("\\", "/"));
  }

  /*
   * these and only these messages in this order
   */
  protected void assertLog(@NotNull String... messages) {
    then(myLog).containsExactly(messages);
  }

  /*
   * these but mey be not only these messages in this order
   */
  protected void assertLogContains(@NotNull String... messages) {
    then(myLog).containsSequence(messages);
  }

  @NotNull
  protected File writeFile(@NotNull String path) throws IOException {
    return writeFile(path, null);
  }

  @NotNull
  protected File writeFile(@NotNull String path, @Nullable String content) throws IOException {
    return writeFile(getBaseDir(), path, content);
  }

  @NotNull
  protected File writeTempFile(@NotNull String path) throws IOException {
    return writeTempFile(path, null);
  }

  @NotNull
  protected File writeTempFile(@NotNull String path, @Nullable String content) throws IOException {
    return writeFile(getTempDir(), path, content);
  }

  @NotNull
  private File writeFile(@NotNull File baseDir, @NotNull String path, @Nullable String content) throws IOException {
    return writeFile(new File(baseDir, path), content);
  }

  @NotNull
  private File writeFile(@NotNull File file, @Nullable String content) throws IOException {
    FileUtil.createParentDirs(file);
    FileUtil.writeFile(file, content == null ? "just some bytes" : content, "UTF-8");
    return file;
  }
}
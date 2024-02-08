

package jetbrains.buildServer.runner.codedeploy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class ServiceMessageLoggingDeploymentListener extends LoggingDeploymentListener {
  ServiceMessageLoggingDeploymentListener(@NotNull Map<String, String> runnerParameters, @Nullable String checkoutDir) {
    super(runnerParameters, checkoutDir);
  }

  protected void err(@NotNull String message) {
    log(String.format("##teamcity[message text='%s' tc:tags='tc:internal' status='error']", escape(message)));
  }

  protected void open(@NotNull String block) {
    log(String.format("##teamcity[blockOpened name='%s' tc:tags='tc:internal']", escape(block)));
  }

  protected  void close(@NotNull String block) {
    log(String.format("##teamcity[blockClosed name='%s' tc:tags='tc:internal']", escape(block)));
  }

  protected void progress(@NotNull String message) {
    log(String.format("##teamcity[progressMessage '%s' tc:tags='tc:internal']", escape(message)));
  }

  protected void problem(int identity, @NotNull String type, @NotNull String descr) {
    log(String.format("##teamcity[buildProblem identity='%d' type='%s' description='%s' tc:tags='tc:internal']", identity, type, escape(descr)));
  }

  protected void parameter(@NotNull String name, @NotNull String value) {
    log(String.format("##teamcity[setParameter name='%s' value='%s' tc:tags='tc:internal']", name, value));
  }

  protected void statusText(@NotNull String text) {
    log(String.format("##teamcity[buildStatus tc:tags='tc:internal' text='{build.status.text}; %s']", text));
  }

  @NotNull
  protected String escape(@NotNull String s) {
    return s.
      replace("|", "||").
      replace("'", "|'").
      replace("\n", "|n").
      replace("\r", "|r").
      replace("\\uNNNN", "|0xNNNN").
      replace("[", "|[").replace("]", "|]");
  }
}
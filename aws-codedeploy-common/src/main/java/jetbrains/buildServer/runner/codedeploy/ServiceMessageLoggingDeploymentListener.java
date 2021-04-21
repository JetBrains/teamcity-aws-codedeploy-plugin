/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

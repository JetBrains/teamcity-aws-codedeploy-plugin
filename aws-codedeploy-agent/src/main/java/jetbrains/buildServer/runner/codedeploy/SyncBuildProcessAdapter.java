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

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

/**
 * @author vbedrosova
 * a copy of jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter in teamcity-deployer-plugin
 */
public abstract class SyncBuildProcessAdapter extends BuildProcessAdapter {
  protected final BuildProgressLogger myLogger;
  private volatile boolean hasFinished;
  private volatile boolean hasFailed;
  private volatile boolean isInterrupted;


  public SyncBuildProcessAdapter(@NotNull final BuildProgressLogger logger) {
    myLogger = logger;
    hasFinished = false;
    hasFailed = false;
  }


  @Override
  public void interrupt() {
    isInterrupted = true;
  }

  @Override
  public boolean isInterrupted() {
    return isInterrupted;
  }

  @Override
  public boolean isFinished() {
    return hasFinished;
  }

  @NotNull
  @Override
  public BuildFinishedStatus waitFor() throws RunBuildException {
    while (!isInterrupted() && !hasFinished) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RunBuildException(e);
      }
    }
    return hasFinished ?
      hasFailed ? BuildFinishedStatus.FINISHED_FAILED :
        BuildFinishedStatus.FINISHED_SUCCESS :
      BuildFinishedStatus.INTERRUPTED;
  }

  @Override
  public void start() throws RunBuildException {
    try {
      runProcess();
    } catch (RunBuildException e) {
      myLogger.buildFailureDescription(e.getMessage());
      Loggers.AGENT.error(e);
      hasFailed = true;
    } finally {
      hasFinished = true;
    }
  }

  protected abstract void runProcess() throws RunBuildException;

  protected void checkIsInterrupted() throws UploadInterruptedException {
    if (isInterrupted()) throw new UploadInterruptedException();
  }
}

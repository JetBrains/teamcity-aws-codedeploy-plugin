

package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author vbedrosova
 */
public abstract class SyncBuildProcessAdapter implements BuildProcess {
  @NotNull
  private final AtomicBoolean myIsInterrupted = new AtomicBoolean();
  @NotNull
  private final AtomicBoolean myIsFinished = new AtomicBoolean();

  public final boolean isInterrupted() {
    return myIsInterrupted.get();
  }

  public final boolean isFinished() {
    return myIsFinished.get();
  }

  public final void interrupt() {
    myIsInterrupted.set(true);
    interruptImpl();
  }

  public void start() throws RunBuildException { }

  @NotNull
  public final BuildFinishedStatus waitFor() throws RunBuildException {
    try {
      if (isInterrupted()) return BuildFinishedStatus.INTERRUPTED;
      final BuildFinishedStatus status = runImpl();
      if (isInterrupted()) return BuildFinishedStatus.INTERRUPTED;
      return status;
    } finally {
      myIsFinished.set(true);
    }
  }

  @NotNull
  protected abstract BuildFinishedStatus runImpl() throws RunBuildException;
  protected void interruptImpl() { }
}
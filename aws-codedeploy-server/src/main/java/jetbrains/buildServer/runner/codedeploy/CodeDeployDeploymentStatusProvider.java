

package jetbrains.buildServer.runner.codedeploy;

import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.s3.AmazonS3;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.agentless.DetachedBuildStatusProvider;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static jetbrains.buildServer.serverSide.buildLog.MessageAttrs.DEFAULT_FLOW_ID;
import static jetbrains.buildServer.serverSide.buildLog.MessageAttrs.attrs;

public class CodeDeployDeploymentStatusProvider implements DetachedBuildStatusProvider {
  @NotNull
  @Override
  public String getDescription() {
    return CodeDeployConstants.RUNNER_DISPLAY_NAME;
  }

  @Override
  public boolean accepts(@NotNull SRunningBuild runningBuild, @Nullable String trackingInfo) {
    return getDeploymentInfo(runningBuild) != null;
  }

  @Nullable
  private String getDeploymentInfo(@NotNull SRunningBuild sRunningBuild) {
    return sRunningBuild.getParametersProvider().get(CodeDeployConstants.DEPLOYMENT_ID_BUILD_CONFIG_PARAM);
  }

  @Override
  public void updateBuild(@NotNull SRunningBuild runningBuild, @Nullable String trackingInfo) {
    final String deploymentId = getDeploymentInfo(runningBuild);
    assert deploymentId != null;

    final Map<String, String> runnerParameters = getParameters(runningBuild);
    final Date finishDate = AWSCommonParams.withAWSClients(runnerParameters, clients -> {
      final AWSClient awsClient = createAWSClient(clients.createS3Client(), clients.createCodeDeployClient(), runningBuild).withListener(new LoggingDeploymentListener(runnerParameters, StringUtil.EMPTY) {
        private void log(@NotNull String message, @NotNull Status status) {
          runningBuild.getBuildLog().message(message, status, attrs());
        }

        @Override
        protected void log(@NotNull String message) {
          log(message, Status.NORMAL);
        }

        @Override
        protected void err(@NotNull String message) {
          log(message, Status.ERROR);
        }

        @Override
        protected void open(@NotNull String block) {
          runningBuild.getBuildLog().openBlock(block, "", attrs());
        }

        @Override
        protected void close(@NotNull String block) {
          runningBuild.getBuildLog().closeBlock(block, "", new Date(), DEFAULT_FLOW_ID);
        }

        @Override
        protected void progress(@NotNull String message) {
          runningBuild.getBuildLog().progressMessage(message, null, DEFAULT_FLOW_ID);
        }

        @Override
        protected void problem(int identity, @NotNull String type, @NotNull String descr) {
          runningBuild.addBuildProblem(BuildProblemData.createBuildProblem(String.valueOf(identity), type, descr));
        }

        @Override
        protected void parameter(@NotNull String name, @NotNull String value) {
          ((RunningBuildEx) runningBuild).getBuildPromotion().setCustomParameters(Collections.singletonMap(name, value));
        }

        @Override
        protected void statusText(@NotNull String text) {
          ((RunningBuildEx) runningBuild).setCustomStatusText(text);
        }
      });

      try {
        return IOGuard.allowNetworkCall(() -> awsClient.checkDeploymentStatus(deploymentId, runningBuild.getFinishOnAgentDate()));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    if (finishDate == null) return;
    runningBuild.finish(finishDate);
  }

  @NotNull
  private Map<String, String> getParameters(@NotNull SRunningBuild build) {
    final BuildPromotionEx buildPromotion = (BuildPromotionEx) build.getBuildPromotion();
    final String step = (String) buildPromotion.getAttribute(BuildAttributes.BUILD_DETACHED_FROM_AGENT_AT_STEP);
    assert step != null;
    final SBuildStepDescriptor runner = buildPromotion.getBuildSettings().getBuildRunners().stream().filter(r -> step.equals(r.getId())).findFirst().orElse(null);
    assert runner != null;
    return runner.getParameters();
  }

  @NotNull
  private AWSClient createAWSClient(@NotNull final AmazonS3 s3Client, @NotNull final AmazonCodeDeployClient codeDeployClient, @NotNull final SRunningBuild runningBuild) {
    return new AWSClient(s3Client, codeDeployClient).withDescription("TeamCity build \"" + runningBuild.getBuildTypeName() + "\" #" + runningBuild.getBuildNumber());
  }
}
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
  public String getType() {
    return CodeDeployConstants.RUNNER_TYPE;
  }

  @NotNull
  @Override
  public String getDescription() {
    return CodeDeployConstants.RUNNER_DISPLAY_NAME;
  }

  @Override
  public boolean accepts(@NotNull SRunningBuild sRunningBuild, @NotNull String s) {
    return getDeploymentId(sRunningBuild) != null;
  }

  @Nullable
  private String getDeploymentId(@NotNull SRunningBuild sRunningBuild) {
    return sRunningBuild.getParametersProvider().get(CodeDeployConstants.DEPLOYMENT_ID_BUILD_CONFIG_PARAM);
  }

  @Override
  public boolean processBuild(@NotNull SRunningBuild runningBuild, @Nullable String trackingId) {
    final String deploymentId = getDeploymentId(runningBuild);
    assert deploymentId != null;

    final Map<String, String> runnerParameters = getParameters(runningBuild);
    return AWSCommonParams.withAWSClients(runnerParameters, clients -> {
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
          ((RunningBuildEx)runningBuild).getBuildPromotion().setCustomParameters(Collections.singletonMap(name, value));
        }

        @Override
        protected void statusText(@NotNull String text) {
          ((RunningBuildEx)runningBuild).setCustomStatusText(text);
        }
      });
      return awsClient.checkDeploymentStatus(deploymentId, runningBuild.getFinishOnAgentDate());
    });
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

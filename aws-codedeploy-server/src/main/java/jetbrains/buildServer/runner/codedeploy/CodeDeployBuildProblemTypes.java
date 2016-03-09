package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.serverSide.problems.BaseBuildProblemTypeDetailsProvider;
import jetbrains.buildServer.serverSide.problems.BuildProblemTypeDetailsProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author vbedrosova
 */
public class CodeDeployBuildProblemTypes {

  public CodeDeployBuildProblemTypes(@NotNull ExtensionHolder extensionHolder) {
    register(CodeDeployConstants.TIMEOUT_BUILD_PROBLEM_TYPE, "CodeDeploy timeout", extensionHolder);
    register(CodeDeployConstants.FAILURE_BUILD_PROBLEM_TYPE, "CodeDeploy failure", extensionHolder);
    register(CodeDeployConstants.EXCEPTION_BUILD_PROBLEM_TYPE, "AWS unexpected exception", extensionHolder);
    register(CodeDeployConstants.SERVICE_PROBLEM_TYPE, "AWS service exception", extensionHolder);
    register(CodeDeployConstants.CLIENT_PROBLEM_TYPE, "AWS client exception", extensionHolder);
  }

  private static void register(@NotNull final String type, @NotNull final String descr, @NotNull final ExtensionHolder extensionHolder) {
    extensionHolder.registerExtension(BuildProblemTypeDetailsProvider.class,
      type,
      new BaseBuildProblemTypeDetailsProvider() {
        @NotNull
        public String getType() {
          return type;
        }

        @NotNull
        @Override
        public String getTypeDescription() {
          return descr;
        }
      });
  }

}

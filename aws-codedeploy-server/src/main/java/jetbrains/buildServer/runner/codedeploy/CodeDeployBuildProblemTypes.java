

package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.serverSide.problems.BaseBuildProblemTypeDetailsProvider;
import jetbrains.buildServer.serverSide.problems.BuildProblemTypeDetailsProvider;
import jetbrains.buildServer.util.amazon.AWSException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author vbedrosova
 */
public class CodeDeployBuildProblemTypes {

  public CodeDeployBuildProblemTypes(@NotNull ExtensionHolder extensionHolder) {
    register(CodeDeployConstants.TIMEOUT_BUILD_PROBLEM_TYPE, "CodeDeploy timeout", extensionHolder);
    register(CodeDeployConstants.FAILURE_BUILD_PROBLEM_TYPE, "CodeDeploy failure", extensionHolder);

    for (Map.Entry<String, String> e : AWSException.PROBLEM_TYPES.entrySet())
      register(e.getKey(), e.getValue(), extensionHolder);
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


package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.runner.codedeploy.CodeDeployConstants.*;

/**
 * @author vbedrosova
 */
public class CodeDeployRunType extends RunType {
  @NotNull
  private final String myEditParamsPath;
  @NotNull
  private final String myViewParamsPath;
  @NotNull
  private final ServerSettings myServerSettings;

  public CodeDeployRunType(@NotNull RunTypeRegistry registry,
                           @NotNull PluginDescriptor descriptor,
                           @NotNull WebControllerManager controllerManager,
                           @NotNull ServerSettings serverSettings) {
    registry.registerRunType(this);

    myServerSettings = serverSettings;

    myEditParamsPath = registerController(descriptor, controllerManager, EDIT_PARAMS_JSP, EDIT_PARAMS_HTML);
    myViewParamsPath = registerController(descriptor, controllerManager, VIEW_PARAMS_JSP, VIEW_PARAMS_HTML);
  }

  @NotNull
  private static String registerController(@NotNull final PluginDescriptor descriptor,
                                           @NotNull WebControllerManager controllerManager,
                                           @NotNull final String jspPath,
                                           @NotNull String htmlPath) {
    final String resolvedHtmlPath = descriptor.getPluginResourcesPath(htmlPath);
    controllerManager.registerController(resolvedHtmlPath, new BaseController() {
      @NotNull
      @Override
      protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        final ModelAndView mv = new ModelAndView(descriptor.getPluginResourcesPath(jspPath));
        mv.getModel().put(DEPLOYMENT_SCENARIOS, STEP_LABELS);
        return mv;
      }
    });
    return resolvedHtmlPath;
  }

  @NotNull
  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return properties -> CollectionsUtil.convertCollection(ParametersValidator.validateSettings(properties).entrySet(), source -> new InvalidProperty(source.getKey(), source.getValue()));
  }

  @NotNull
  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    final Map<String, String> defaults = new HashMap<String, String>(DEFAULTS);
    defaults.putAll(AWSCommonParams.getDefaults(myServerSettings.getServerUUID()));
    return defaults;
  }

  @NotNull
  @Override
  public String getType() {
    return RUNNER_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return RUNNER_DISPLAY_NAME;
  }

  @NotNull
  @Override
  public String getDescription() {
    return RUNNER_DESCR;
  }

  @NotNull
  @Override
  public String getEditRunnerParamsJspFilePath() {
    return myEditParamsPath;
  }

  @NotNull
  @Override
  public String getViewRunnerParamsJspFilePath() {
    return myViewParamsPath;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> parameters) {
    final Map<String, String> invalids = ParametersValidator.validateSettings(parameters);
    return
      invalids.isEmpty() ?
      STEP_LABELS.get(CodeDeployUtil.getDeploymentSteps(parameters)) + " application revision" :
      CodeDeployUtil.printStrings(invalids.values());
  }
}
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

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    myEditParamsPath = registerController(descriptor, controllerManager, CodeDeployConstants.EDIT_PARAMS_JSP, CodeDeployConstants.EDIT_PARAMS_HTML);
    myViewParamsPath = registerController(descriptor, controllerManager, CodeDeployConstants.VIEW_PARAMS_JSP, CodeDeployConstants.VIEW_PARAMS_HTML);
  }

  @NotNull
  private static String registerController(@NotNull final PluginDescriptor descriptor,
                                           @NotNull WebControllerManager controllerManager,
                                           @NotNull final String jspPath,
                                           @NotNull String htmlPath) {
    final String resolvedHtmlPath = descriptor.getPluginResourcesPath(htmlPath);
    controllerManager.registerController(resolvedHtmlPath, new BaseController() {
      @Nullable
      @Override
      protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        final ModelAndView mv = new ModelAndView(descriptor.getPluginResourcesPath(jspPath));
        mv.getModel().put(CodeDeployConstants.ALL_REGIONS, AWSUtil.getAllRegions());
        mv.getModel().put(CodeDeployConstants.DEPLOYMENT_SCENARIOS, CodeDeployConstants.STEP_LABELS);
        return mv;
      }
    });
    return resolvedHtmlPath;
  }

  @Nullable
  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new PropertiesProcessor() {
      @Override
      public Collection<InvalidProperty> process(Map<String, String> properties) {
        return CollectionsUtil.convertCollection(ParametersValidator.validateSettings(properties).entrySet(), new Converter<InvalidProperty, Map.Entry<String, String>>() {
          @Override
          public InvalidProperty createFrom(@NotNull Map.Entry<String, String> source) {
            return new InvalidProperty(source.getKey(), source.getValue());
          }
        });
      }
    };
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    final Map<String, String> defaults = new HashMap<String, String>(CodeDeployConstants.DEFAULTS);
    final String serverUUID = myServerSettings.getServerUUID();
    if (StringUtil.isNotEmpty(serverUUID)) {
      defaults.put(CodeDeployConstants.EXTERNAL_ID_PARAM, "TeamCity-server-" + serverUUID);
    }
    return defaults;
  }

  @NotNull
  @Override
  public String getType() {
    return CodeDeployConstants.RUNNER_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return CodeDeployConstants.RUNNER_DISPLAY_NAME;
  }

  @NotNull
  @Override
  public String getDescription() {
    return CodeDeployConstants.RUNNER_DESCR;
  }

  @Nullable
  @Override
  public String getEditRunnerParamsJspFilePath() {
    return myEditParamsPath;
  }

  @Nullable
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
      CodeDeployConstants.STEP_LABELS.get(parameters.get(CodeDeployConstants.DEPLOYMENT_STEPS_PARAM)) + " application revision" :
      CodeDeployUtil.printStrings(invalids.values());
  }
}

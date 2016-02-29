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

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vbedrosova
 */
public class CodeDeployRunType extends RunType {
  @NotNull
  private final PluginDescriptor myDescriptor;
  @NotNull
  private final ServerSettings myServerSettings;

  public CodeDeployRunType(@NotNull RunTypeRegistry registry, @NotNull PluginDescriptor descriptor, @NotNull ServerSettings serverSettings) {
    registry.registerRunType(this);
    myDescriptor = descriptor;
    myServerSettings = serverSettings;
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
      defaults.put(CodeDeployConstants.EXTERNAL_ID_PARAM, serverUUID);
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
    return myDescriptor.getPluginResourcesPath(CodeDeployConstants.EDIT_PARAMS_JSP);
  }

  @Nullable
  @Override
  public String getViewRunnerParamsJspFilePath() {
    return myDescriptor.getPluginResourcesPath(CodeDeployConstants.VIEW_PARAMS_JSP);
  }

  @SuppressWarnings("StringBufferReplaceableByString")
  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> parameters) {
    final StringBuilder descr = new StringBuilder();
    descr
      .append("Deploy ")
      .append(parameters.get(CodeDeployConstants.READY_REVISION_PATH_PARAM))
      .append(" to deployment group ")
      .append(parameters.get(CodeDeployConstants.DEPLOYMENT_GROUP_NAME_PARAM))
      .append(" in scope of ")
      .append(parameters.get(CodeDeployConstants.APP_NAME_PARAM))
      .append(" application");
    return descr.toString();
  }
}

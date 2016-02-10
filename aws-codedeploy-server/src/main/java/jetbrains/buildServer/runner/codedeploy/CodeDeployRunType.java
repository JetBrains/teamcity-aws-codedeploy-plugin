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

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author vbedrosova
 */
public class CodeDeployRunType extends RunType {
  public CodeDeployRunType(@NotNull RunTypeRegistry registry) {
    registry.registerRunType(this);
  }

  @Nullable
  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return null;
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    return null;
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
    return CodeDeployConstants.EDIT_PARAMS_JSP;
  }

  @Nullable
  @Override
  public String getViewRunnerParamsJspFilePath() {
    return CodeDeployConstants.VIEW_PARAMS_JSP;
  }
}

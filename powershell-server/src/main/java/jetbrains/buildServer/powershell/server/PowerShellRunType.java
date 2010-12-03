/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package jetbrains.buildServer.powershell.server;

import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.requirements.RequirementType;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 18:27
 */
public class PowerShellRunType extends RunType {
  private final PluginDescriptor myDescriptor;

  public PowerShellRunType(@NotNull RunTypeRegistry reg,
                           @NotNull final PluginDescriptor descriptor) {
    myDescriptor = descriptor;
    reg.registerRunType(this);
  }

  @NotNull
  @Override
  public String getType() {
    return PowerShellConstants.RUN_TYPE;
  }

  @Override
  public String getDisplayName() {
    return "Powershell";
  }

  @Override
  public String getDescription() {
    return "Powershell runner";
  }

  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new PropertiesProcessor() {
      public Collection<InvalidProperty> process(final Map<String, String> properties) {
        Collection<InvalidProperty> col = new ArrayList<InvalidProperty>();

        final PowerShellBitness bit = getBitness(properties);
        if (bit == null) {
          col.add(new InvalidProperty(PowerShellConstants.RUNNER_BITNESS, "Bitness is not defined"));
        }

        final PowerShellScriptMode mod = getScriptMode(properties);
        if (mod == null) {
          col.add(new InvalidProperty(PowerShellConstants.RUNNER_SCRIPT_MODE, "Script mode is not defined"));
        } else {
          switch (mod) {
            case FILE:
              if (StringUtil.isEmptyOrSpaces(properties.get(PowerShellConstants.RUNNER_SCRIPT_FILE))) {
                col.add(new InvalidProperty(PowerShellConstants.RUNNER_SCRIPT_FILE, "Script file is not defined"));
              }
            case CODE:
              if (StringUtil.isEmptyOrSpaces(properties.get(PowerShellConstants.RUNNER_SCRIPT_CODE))) {
                col.add(new InvalidProperty(PowerShellConstants.RUNNER_SCRIPT_CODE, "Code should not be empty"));
              }
          }
        }
        return col;
      }
    };
  }

  @Override
  public String getEditRunnerParamsJspFilePath() {
    return myDescriptor.getPluginResourcesPath("editPowerShell.jsp");
  }

  @Override
  public String getViewRunnerParamsJspFilePath() {
    return myDescriptor.getPluginResourcesPath("viewPowerShell.jsp");
  }

  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    Map<String, String> map = new HashMap<String, String>();
    map.put(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.toString());
    return map;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull final Map<String, String> parameters) {
    StringBuilder sb = new StringBuilder("Powershell ");

    final PowerShellBitness bit = getBitness(parameters);
    if (bit != null) {
      sb.append(bit).append(" ");
    }

    final PowerShellScriptMode mode = getScriptMode(parameters);
    if (mode != null) {
      switch (mode) {
        case FILE:
          sb.append(parameters.get(PowerShellConstants.RUNNER_SCRIPT_FILE));
        case CODE:
          sb.append("<script>");
      }
    }
    return sb.toString();
  }

  @Nullable
  private PowerShellScriptMode getScriptMode(@NotNull final Map<String, String> parameters) {
    return PowerShellScriptMode.fromString(parameters.get(PowerShellConstants.RUNNER_SCRIPT_MODE));
  }

  @Nullable
  private PowerShellBitness getBitness(@NotNull final Map<String, String> parameters) {
    return PowerShellBitness.fromString(parameters.get(PowerShellConstants.RUNNER_BITNESS));
  }

  @Override
  public List<Requirement> getRunnerSpecificRequirements(@NotNull final Map<String, String> runParameters) {
    final PowerShellBitness bit = getBitness(runParameters);
    if (bit != null) {
      return Collections.singletonList(new Requirement(bit.getVersionKey(), null, RequirementType.EXISTS));
    }
    return Collections.emptyList();
  }
}

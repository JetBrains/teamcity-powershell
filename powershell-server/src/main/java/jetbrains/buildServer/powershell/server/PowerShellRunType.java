/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.powershell.common.*;
import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.requirements.RequirementQualifier;
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

import static jetbrains.buildServer.powershell.common.PowerShellConstants.*;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 18:27
 */
public class PowerShellRunType extends RunType {
  private final PluginDescriptor myDescriptor;

  @SuppressWarnings("WeakerAccess")
  public PowerShellRunType(@NotNull final RunTypeRegistry reg,
                           @NotNull final PluginDescriptor descriptor) {
    myDescriptor = descriptor;
    reg.registerRunType(this);
  }

  @NotNull
  @Override
  public String getType() {
    return PowerShellConstants.RUN_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "PowerShell";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "PowerShell runner";
  }

  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return properties -> {
      Collection<InvalidProperty> col = new ArrayList<>();
      final PowerShellExecutionMode exe = PowerShellExecutionMode.fromString(properties.get(RUNNER_EXECUTION_MODE));
      if (exe == null) {
        col.add(new InvalidProperty(RUNNER_EXECUTION_MODE, "Execution mode must be specified"));
      }

      final PowerShellScriptMode mod = getScriptMode(properties);
      if (mod == null) {
        col.add(new InvalidProperty(RUNNER_SCRIPT_MODE, "Script mode is not defined"));
      } else {
        switch (mod) {
          case FILE:
            final String script = properties.get(RUNNER_SCRIPT_FILE);
            if (StringUtil.isEmptyOrSpaces(script)) {
              col.add(new InvalidProperty(RUNNER_SCRIPT_FILE, "Script file is not defined"));
            } else if (!ReferencesResolverUtil.containsReference(script) && !script.toLowerCase().endsWith(".ps1")) {
              col.add(new InvalidProperty(RUNNER_SCRIPT_FILE, "PowerShell requires script files to have .ps1 extension"));
            }
            break;
          case CODE:
            if (StringUtil.isEmptyOrSpaces(properties.get(RUNNER_SCRIPT_CODE))) {
              col.add(new InvalidProperty(RUNNER_SCRIPT_CODE, "Code should not be empty"));
            }
            break;
        }
      }
      return col;
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
    Map<String, String> map = new HashMap<>();
    map.put(RUNNER_NO_PROFILE, "true");
    map.put(RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.toString());
    return map;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull final Map<String, String> parameters) {
    final StringBuilder sb = new StringBuilder("PowerShell ");

    final String minVersion = getMinimalVersion(parameters);
    if (minVersion != null) {
      sb.append(minVersion).append(" ");
    }

    final PowerShellEdition edition = getEdition(parameters);
    if (edition != null) {
      sb.append(edition.getDisplayName()).append(" ");
    }

    final PowerShellBitness bit = getBitness(parameters);
    if (bit != null) {
      sb.append(bit).append(" ");
    } else {
      sb.append(" <Any Bitness>");
    }

    final PowerShellScriptMode mode = getScriptMode(parameters);
    if (mode != null) {
      switch (mode) {
        case FILE:
          sb.append(" File: ").append(parameters.get(RUNNER_SCRIPT_FILE));
          break;
        case CODE:
          sb.append(" <script>");
          break;
      }
    }
    return sb.toString();
  }

  @Nullable
  private PowerShellScriptMode getScriptMode(@NotNull final Map<String, String> parameters) {
    return PowerShellScriptMode.fromString(parameters.get(RUNNER_SCRIPT_MODE));
  }

  @Nullable
  private PowerShellBitness getBitness(@NotNull final Map<String, String> parameters) {
    return PowerShellBitness.fromString(parameters.get(RUNNER_BITNESS));
  }

  @Nullable
  private String getMinimalVersion(@NotNull final Map<String, String> parameters) {
    return parameters.get(RUNNER_MIN_VERSION);
  }

  @Nullable
  private PowerShellEdition getEdition(@NotNull final Map<String, String> parameters) {
    return PowerShellEdition.fromString(parameters.get(RUNNER_EDITION));
  }

  @NotNull
  @Override
  public List<Requirement> getRunnerSpecificRequirements(@NotNull final Map<String, String> runParameters) {
    final List<Requirement> result = new ArrayList<>();

    final String minVersion = getMinimalVersion(runParameters);
    final PowerShellBitness bit = getBitness(runParameters);
    final PowerShellEdition edition = getEdition(runParameters);

    // filter editions
    final List<PowerShellEdition> editions = new ArrayList<>();
    if (edition == null) { // for all editions
      editions.addAll(Arrays.asList(PowerShellEdition.values()));
    } else {
      editions.add(edition);
    }

    final List<PowerShellBitness> bits = new ArrayList<>();
    if (bit == null) {
      bits.addAll(Arrays.asList(PowerShellBitness.values()));
    } else {
      bits.add(bit);
    }

    List<String> keys = new ArrayList<>();
    for (PowerShellEdition e: editions) {
      for (PowerShellBitness b: bits) {
        keys.add(generateGeneralKey(e, b));
      }
    }

    if (minVersion == null) { // EXISTS requirement type, as we have no specific version set
      result.add(new Requirement(getRequirementPropertyName(keys), null, RequirementType.EXISTS));
    } else { // VER_NO_LESS_THAN requirement type, as minimal version is set
      result.add(new Requirement(getRequirementPropertyName(keys), minVersion, RequirementType.VER_NO_LESS_THAN));
    }
    return result;
  }

  private String getRequirementPropertyName(@NotNull final List<String> keys) {
    if (keys.size() == 1) {
      return keys.get(0);
    } else {
      return RequirementQualifier.EXISTS_QUALIFIER + "(" + String.join("|", keys) + ")";
    }
  }
}

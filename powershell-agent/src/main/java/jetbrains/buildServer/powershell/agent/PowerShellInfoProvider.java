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

package jetbrains.buildServer.powershell.agent;

import com.intellij.openapi.diagnostic.Logger;
import java.util.*;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.config.AgentParametersSupplier;
import jetbrains.buildServer.powershell.agent.detect.DetectionContext;
import jetbrains.buildServer.powershell.agent.detect.DetectionContextImpl;
import jetbrains.buildServer.powershell.agent.detect.PowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 16:24
 */
public class PowerShellInfoProvider {

  @NotNull
  private static final Logger LOG = Loggers.DETECTION_LOGGER;

  @NotNull
  private final ShellInfoHolder myHolder;

  public PowerShellInfoProvider(@NotNull final BuildAgentConfiguration config,
                                @NotNull final ExtensionHolder extensionHolder,
                                @NotNull final List<PowerShellDetector> detectors,
                                @NotNull final ShellInfoHolder holder) {
    myHolder = holder;
    extensionHolder.registerExtension(AgentParametersSupplier.class, getClass().getName(), new AgentParametersSupplier(){
      @Override
      public Map<String, String> getParameters() {
        final Map<String, String> parameters = new HashMap<>();

        registerDetectedPowerShells(detectors, new DetectionContextImpl(config), parameters);
        return parameters;
      }
    });
  }

  private void registerDetectedPowerShells(@NotNull final List<PowerShellDetector> detectors,
                                           @NotNull final DetectionContext detectionContext,
                                           Map<String, String> parameters) {
    Map<String, PowerShellInfo> shells = new HashMap<>();
    for (PowerShellDetector detector: detectors) {
      LOG.debug("Processing detected PowerShells from " + detector.getClass().getName());
      for (Map.Entry<String, PowerShellInfo> entry: detector.findShells(detectionContext).entrySet()) {
        LOG.debug("Processing detected PowerShell [" + entry.getKey() + "][" + entry.getValue() + "]");
        if (!shells.containsKey(entry.getKey())) {
          shells.put(entry.getKey(), entry.getValue());
          entry.getValue().saveInfo(parameters);
          myHolder.addShellInfo(entry.getValue());
        }
      }
    }
    // provide parameters for agent compatibility filters
    if (!myHolder.getShells().isEmpty()) {
      provideMaxVersions(parameters);
      provideCompatibilityParams(parameters);
    } else {
      LOG.info("No PowerShell detected. If it is installed in non-standard location, " +
              "please provide install locations in teamcity.powershell.detector.search.paths " +
              "agent property (with ';' as a separator)");
    }
  }

  /**
   * Provides max version of all {@code edition x bitness} combinations.
   * Helps with agent requirements
   */
  private void provideMaxVersions(Map<String, String> parameters) {
    for (PowerShellBitness bitness: PowerShellBitness.values()) {
      for (PowerShellEdition edition: PowerShellEdition.values()) {
        PowerShellInfo info = selectTool(bitness, null, edition);
        if (info != null) {
          parameters.put(PowerShellConstants.generateGeneralKey(edition, bitness), info.getVersion());
        }
      }
    }
  }

  private void provideCompatibilityParams(Map<String, String> parameters) {
    for (PowerShellBitness bitness: PowerShellBitness.values()) {
      // select shell info of max version of each bitness and provide legacy parameters
      PowerShellInfo info = selectTool(bitness, null, null);
      if (info != null) {
        LegacyKeys.fillLegacyKeys(parameters, bitness, info);
      }
    }
  }

  boolean anyPowerShellDetected() {
    return !myHolder.getShells().isEmpty();
  }

  @Nullable
  public PowerShellInfo selectTool(@Nullable final PowerShellBitness bit,
                                   @Nullable final String version,
                                   @Nullable final PowerShellEdition edition) {
    // filter by edition
    List<PowerShellInfo> availableShells = myHolder.getShells();
    if (edition != null) {
      availableShells = CollectionsUtil.filterCollection(availableShells, data -> edition.equals(data.getEdition()));
    }
    // filter by version
    if (version != null) {
      availableShells = CollectionsUtil.filterCollection(availableShells, data -> VersionComparatorUtil.compare(data.getVersion(), version) >= 0);
    }
    // filter by bitness
    if (bit != null) {
      availableShells = CollectionsUtil.filterCollection(availableShells, data -> data.getBitness().equals(bit));
    }
    if (availableShells.isEmpty()) {
      return null;
    }
    if (availableShells.size() == 1) {
      return availableShells.get(0);
    }
    // prefer desktop over core
    if (edition == null) {
      Map<PowerShellEdition, List<PowerShellInfo>> byEdition = new HashMap<>();
      for (PowerShellInfo info: availableShells) {
        if (!byEdition.containsKey(info.getEdition())) {
          byEdition.put(info.getEdition(), new ArrayList<>());
        }
        byEdition.get(info.getEdition()).add(info);
      }
      if (byEdition.get(PowerShellEdition.DESKTOP) != null) {
        availableShells = byEdition.get(PowerShellEdition.DESKTOP);
      } else {
        availableShells = byEdition.get(PowerShellEdition.CORE);
      }
    }
    if (availableShells.size() == 1) {
      return availableShells.get(0);
    }
    // prefer 64bit over 32bit
    if (bit == null) {
      Map<PowerShellBitness, List<PowerShellInfo>> byBits = new HashMap<>();
      for (PowerShellInfo info : availableShells) {
        if (!byBits.containsKey(info.getBitness())) {
          byBits.put(info.getBitness(), new ArrayList<>());
        }
        byBits.get(info.getBitness()).add(info);
      }
      if (byBits.containsKey(PowerShellBitness.x64)) {
        availableShells = byBits.get(PowerShellBitness.x64);
      } else {
        availableShells = byBits.get(PowerShellBitness.x86);
      }
    }
    if (availableShells.isEmpty()) {
      return null;
    }
    if (availableShells.size() == 1) {
      return availableShells.get(0);
    }
    return Collections.max(availableShells, (info1, info2) -> VersionComparatorUtil.compare(info1.getVersion(), info2.getVersion()));
  }
}

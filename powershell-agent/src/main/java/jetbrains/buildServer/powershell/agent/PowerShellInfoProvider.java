/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.detect.DetectionContext;
import jetbrains.buildServer.powershell.agent.detect.PowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.VersionComparatorUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 16:24
 */
public class PowerShellInfoProvider {

  private final BuildAgentConfiguration myConfig;

  public PowerShellInfoProvider(@NotNull final BuildAgentConfiguration config,
                                @NotNull final EventDispatcher<AgentLifeCycleListener> events,
                                @NotNull final List<PowerShellDetector> detectors) {
    myConfig = config;
    events.addListener(new AgentLifeCycleAdapter(){

      @Override
      public void afterAgentConfigurationLoaded(@NotNull BuildAgent agent) {
        registerDetectedPowerShells(detectors, new DetectionContext(agent.getConfiguration()));
      }
    });
  }

  private void registerDetectedPowerShells(@NotNull final List<PowerShellDetector> detectors,
                                           @NotNull final DetectionContext detectionContext) {
    Map<PowerShellBitness, PowerShellInfo> detected = new HashMap<PowerShellBitness, PowerShellInfo>();
    for (PowerShellDetector detector: detectors) {
      for (Map.Entry<PowerShellBitness, PowerShellInfo> e: detector.findPowerShells(detectionContext).entrySet()) {
        if (detected.get(e.getKey()) == null) {
          detected.put(e.getKey(), e.getValue());
        }
      }
    }
    for (PowerShellInfo info: detected.values()) {
      info.saveInfo(myConfig);
    }
  }

  boolean anyPowerShellDetected() {
    return !getPowerShells().isEmpty();
  }
  
  @Nullable
  public PowerShellInfo selectTool(@Nullable final PowerShellBitness bit,
                                   @Nullable final String version,
                                   @Nullable final PowerShellEdition edition) {
    Map<PowerShellBitness, PowerShellInfo> available = getPowerShellsMap();
    PowerShellInfo result;
    if (edition != null) {
      available = CollectionsUtil.filterMapByValues(available, new Filter<PowerShellInfo>() {
        @Override
        public boolean accept(@NotNull PowerShellInfo data) {
          return edition.equals(data.getEdition());
        }
      });
    }
    if (bit == null) {
      if (version != null) {
        available = CollectionsUtil.filterMapByValues(available, new Filter<PowerShellInfo>() {
          @Override
          public boolean accept(@NotNull PowerShellInfo data) {
            return VersionComparatorUtil.compare(data.getVersion(), version) >= 0;
          }
        });
      }
      result = available.get(PowerShellBitness.x64);
      if (result != null) {
        return result;
      }
      return available.get(PowerShellBitness.x86);
    } else {
      return available.get(bit);
    }
  }

  private Map<PowerShellBitness, PowerShellInfo> getPowerShellsMap() {
    final Map<PowerShellBitness, PowerShellInfo> result = new HashMap<PowerShellBitness, PowerShellInfo>(2);
    for (PowerShellBitness bit: PowerShellBitness.values()) {
      final PowerShellInfo info = PowerShellInfo.loadInfo(myConfig, bit);
      if (info != null) {
        result.put(bit, info);
      }
    }
    return result;
  }

  @NotNull
  private Collection<PowerShellInfo> getPowerShells() {
    Collection<PowerShellInfo> infos = new ArrayList<PowerShellInfo>(2);
    for (PowerShellBitness bit : PowerShellBitness.values()) {
      final PowerShellInfo i = PowerShellInfo.loadInfo(myConfig, bit);
      if (i != null) {
        infos.add(i);
      }
    }
    return infos;
  }
}

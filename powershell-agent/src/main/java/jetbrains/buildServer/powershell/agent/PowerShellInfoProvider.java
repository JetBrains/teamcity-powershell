/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.detect.DetectionContext;
import jetbrains.buildServer.powershell.agent.detect.PowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
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
@SuppressWarnings("WeakerAccess")
public class PowerShellInfoProvider {

  @NotNull
  private static final Logger LOG = Loggers.DETECTION_LOGGER;

  @NotNull
  private final BuildAgentConfiguration myConfig;

  @NotNull
  private final ShellInfoHolder myHolder;

  public PowerShellInfoProvider(@NotNull final BuildAgentConfiguration config,
                                @NotNull final EventDispatcher<AgentLifeCycleListener> events,
                                @NotNull final List<PowerShellDetector> detectors,
                                @NotNull final ShellInfoHolder holder) {
    myConfig = config;
    myHolder = holder;
    events.addListener(new AgentLifeCycleAdapter() {
      @Override
      public void afterAgentConfigurationLoaded(@NotNull BuildAgent agent) {
        registerDetectedPowerShells(detectors, new DetectionContext(agent.getConfiguration()));
      }
    });
  }

  private void registerDetectedPowerShells(@NotNull final List<PowerShellDetector> detectors,
                                           @NotNull final DetectionContext detectionContext) {
    Map<String, PowerShellInfo> shells = new HashMap<String, PowerShellInfo>();
    for (PowerShellDetector detector: detectors) {
      LOG.debug("Processing detected PowerShells from " + detector.getClass().getName());
      for (Map.Entry<String, PowerShellInfo> entry: detector.findShells(detectionContext).entrySet()) {
        LOG.debug("Processing detected PowerShell [" + entry.getKey() + "][" + entry.getValue() + "]");
        if (!shells.containsKey(entry.getKey())) {
          shells.put(entry.getKey(), entry.getValue());             
          entry.getValue().saveInfo(myConfig);
          myHolder.addShellInfo(entry.getValue());
        }
      }
    }
    // provide parameters for agent compatibility filters
    if (!myHolder.getShells().isEmpty()) {
      provideMaxVersions();
      provideCompatibilityParams();
    }
  }

  /**
   * Provides max version of all {@code edition x bitness} combinations.
   * Helps with agent requirements
   */
  private void provideMaxVersions() {
    for (PowerShellBitness bitness: PowerShellBitness.values()) {
      for (PowerShellEdition edition: PowerShellEdition.values()) {
        PowerShellInfo info = selectTool(bitness, null, edition);
        if (info != null) {
          myConfig.addConfigurationParameter(PowerShellConstants.generateGeneralKey(edition, bitness), info.getVersion());
        }
      }
    }
  }

  private void provideCompatibilityParams() {
    for (PowerShellBitness bitness: PowerShellBitness.values()) {
      // select shell info of max version of each bitness and provide legacy parameters
      PowerShellInfo info = selectTool(bitness, null, null);
      if (info != null) {
        LegacyKeys.fillLegacyKeys(myConfig, bitness, info);
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
      availableShells = CollectionsUtil.filterCollection(availableShells, new Filter<PowerShellInfo>() {
        @Override
        public boolean accept(@NotNull PowerShellInfo data) {
          return edition.equals(data.getEdition());
        }
      });
    }
    // filter by version
    if (version != null) {
      availableShells = CollectionsUtil.filterCollection(availableShells, new Filter<PowerShellInfo>() {
        @Override
        public boolean accept(@NotNull PowerShellInfo data) {
          return VersionComparatorUtil.compare(data.getVersion(), version) >= 0;
        }
      });
    }
    // filter by bitness
    if (bit != null) {
      availableShells = CollectionsUtil.filterCollection(availableShells, new Filter<PowerShellInfo>() {
        @Override
        public boolean accept(@NotNull PowerShellInfo data) {
          return data.getBitness().equals(bit);
        }
      });
    }
    if (availableShells.isEmpty()) {
      return null;
    }
    if (availableShells.size() == 1) {
      return availableShells.get(0);
    }
    // prefer desktop over core
    if (edition == null) {
      Map<PowerShellEdition, List<PowerShellInfo>> byEdition = new HashMap<PowerShellEdition, List<PowerShellInfo>>();
      for (PowerShellInfo info: availableShells) {
        if (!byEdition.containsKey(info.getEdition())) {
          byEdition.put(info.getEdition(), new ArrayList<PowerShellInfo>());
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
      Map<PowerShellBitness, List<PowerShellInfo>> byBits = new HashMap<PowerShellBitness, List<PowerShellInfo>>();
      for (PowerShellInfo info : availableShells) {
        if (!byBits.containsKey(info.getBitness())) {
          byBits.put(info.getBitness(), new ArrayList<PowerShellInfo>());
        }
        byBits.get(info.getBitness()).add(info);
      }
      if (byBits.containsKey(PowerShellBitness.x64)) {
        availableShells = byBits.get(PowerShellBitness.x64);
      } else {
        availableShells = byBits.get(PowerShellBitness.x86);
      }
    }
    if (availableShells.size() == 1) {
      return availableShells.get(0);
    }
    // select max available version
    Collections.sort(availableShells, new Comparator<PowerShellInfo>() {
      @Override
      public int compare(PowerShellInfo info1, PowerShellInfo info2) {
        return VersionComparatorUtil.compare(info1.getVersion(), info2.getVersion());
      }
    });
    return availableShells.get(0);
  }
}

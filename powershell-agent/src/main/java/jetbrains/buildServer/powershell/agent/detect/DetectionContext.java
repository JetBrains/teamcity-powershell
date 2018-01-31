/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.detect;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class DetectionContext {

  @NotNull
  private static final String PARAM_SEARCH_PATHS = "teamcity.powershell.detector.search.paths";

  @NotNull
  private final Map<PowerShellBitness, String> myPredefinedPaths;

  @NotNull
  private final List<String> mySearchPaths;

  private final Map<String, String> myAgentParameters;

  public DetectionContext(@NotNull final BuildAgentConfiguration configuration) {
    myAgentParameters = configuration.getConfigurationParameters();
    myPredefinedPaths = loadPredefinedPaths();
    mySearchPaths = loadSearchPaths();
  }

  /**
   * Loads predefined paths from agent configuration file
   * @see PowerShellBitness#getPathKey()
   *
   * @return map with specific PowerShell paths
   */
  private Map<PowerShellBitness, String> loadPredefinedPaths() {
    final Map<PowerShellBitness, String> result = new HashMap<PowerShellBitness, String>();
    for (PowerShellBitness bit: PowerShellBitness.values()) {
      String val = myAgentParameters.get(bit.getPathKey());
      if (!StringUtil.isEmptyOrSpaces(val)) {
        result.put(bit, val);
      }
    }
    return result;
  }

  private List<String> loadSearchPaths() {
    String val = myAgentParameters.get(PARAM_SEARCH_PATHS);
    List<String> result;
    if (!StringUtil.isEmptyOrSpaces(val)) {
      result = StringUtil.split(val, ";");
    } else {
      result = Collections.emptyList();
    }
    return result;
  }

  @NotNull
  public Map<PowerShellBitness, String> getPredefinedPaths() {
    return myPredefinedPaths;
  }

  @NotNull
  public List<String> getSearchPaths() {
    return mySearchPaths;
  }
}

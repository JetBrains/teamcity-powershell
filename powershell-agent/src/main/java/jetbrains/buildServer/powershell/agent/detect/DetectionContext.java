/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.detect;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
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
  private final List<String> mySearchPaths;

  private final Map<String, String> myAgentParameters;

  public DetectionContext(@NotNull final BuildAgentConfiguration configuration) {
    myAgentParameters = configuration.getConfigurationParameters();
    mySearchPaths = loadSearchPaths();
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
  public List<String> getSearchPaths() {
    return mySearchPaths;
  }
}

/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.server;

import jetbrains.buildServer.powershell.common.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 18:56
 */
public class PowerShellBean {

  @NotNull
  public String getBitnessKey() {
    return PowerShellConstants.RUNNER_BITNESS;
  }

  @NotNull
  public String getEditionKey() {
    return PowerShellConstants.RUNNER_EDITION;
  }

  @NotNull
  public Map<String, String> getBitnessValues() {
    final Map<String, String> result = new LinkedHashMap<>();
    result.put(PowerShellConstants.PARAM_VALUE_BITNESS_AUTO, "");
    result.putAll(Arrays.stream(PowerShellBitness.values()).collect(Collectors.toMap(PowerShellBitness::getValue, PowerShellBitness::getValue)));
    return result;
  }

  @NotNull
  public Map<String, String> getEditionValues() {
    final Map<String, String> result = new LinkedHashMap<>();
    result.put(PowerShellConstants.PARAM_VALUE_EDITION_ANY, "");
    result.putAll(Arrays.stream(PowerShellEdition.values()).collect(Collectors.toMap(PowerShellEdition::getDisplayName, PowerShellEdition::getValue)));
    return result;
  }
  
  @NotNull
  public String getScriptModeKey() {
    return PowerShellConstants.RUNNER_SCRIPT_MODE;
  }

  @NotNull
  public String getScriptModeFileValue() {
    return PowerShellScriptMode.FILE.getValue();
  }

  @NotNull
  public String getScriptModeCodeValue() {
    return PowerShellScriptMode.CODE.getValue();
  }

  @NotNull
  public String getScriptFileKey() {
    return PowerShellConstants.RUNNER_SCRIPT_FILE;
  }

  @NotNull
  public String getScriptCodeKey() {
    return PowerShellConstants.RUNNER_SCRIPT_CODE;
  }

  @NotNull
  public String getExecutionModeKey() {
    return PowerShellConstants.RUNNER_EXECUTION_MODE;
  }

  @NotNull
  public String getExecutionModeAsFileValue() {
    return PowerShellExecutionMode.PS1.getValue();
  }

  @NotNull
  public String getExecutionModeStdinValue() {
    return PowerShellExecutionMode.STDIN.getValue();
  }

  @NotNull
  public String getArgumentsKey() {
    return PowerShellConstants.RUNNER_CUSTOM_ARGUMENTS;
  }

  @NotNull
  public String getScriptArgmentsKey() {
    return PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS;
  }

  @NotNull
  public String getErrorToErrorKey() {
    return PowerShellConstants.RUNNER_LOG_ERR_TO_ERROR;
  }

  @NotNull
  public String getMinVersionKey() {
    return PowerShellConstants.RUNNER_MIN_VERSION;
  }

  @NotNull
  public String getNoProfileKey() {
    return PowerShellConstants.RUNNER_NO_PROFILE;
  }

}

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

package jetbrains.buildServer.powershell.server;

import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
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
  public Map<String, String> getBitnessValues() {
    final Map<String, String> result = new LinkedHashMap<>();
    result.put(PowerShellConstants.PARAM_VALUE_BITNESS_AUTO, "");
    result.putAll(Arrays.stream(PowerShellBitness.values()).collect(Collectors.toMap(PowerShellBitness::getDescription, PowerShellBitness::getValue)));
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

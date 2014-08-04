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

import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.powershell.common.PowerShellConstants.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class PowerShellCommandLineProvider {

  private static final Logger LOG = Logger.getInstance(PowerShellCommandLineProvider.class.getName());

  @NotNull
  public List<String> provideCommandLine(@NotNull final PowerShellInfo info,
                                         @NotNull final Map<String, String> runnerParams,
                                         @NotNull final File scriptFile,
                                         final boolean useExecutionPolicy,
                                         @NotNull final Map<String, String> configParams) throws RunBuildException {
    final List<String> result = new ArrayList<String>();

    result.add(info.getExecutablePath());
    addVersion(result, runnerParams); // version must be the 1st arg after executable path
    if (!StringUtil.isEmptyOrSpaces(runnerParams.get(RUNNER_NO_PROFILE))) {
      result.add("-NoProfile");
    }
    result.add("-NonInteractive");

    addCustomArguments(result, runnerParams, RUNNER_CUSTOM_ARGUMENTS, false);
    if (useExecutionPolicy) {
      addExecutionPolicyPreference(result);
    }

    PowerShellExecutionMode mod = PowerShellExecutionMode.fromString(runnerParams.get(RUNNER_EXECUTION_MODE));
    if (mod == null) {
      throw new RunBuildException("'" + RUNNER_EXECUTION_MODE + "' runner parameters is not defined");
    }
    addScriptBody(result, mod, scriptFile, runnerParams, configParams);
    return result;
  }

  private void addVersion(@NotNull final List<String> list,
                          @NotNull final Map<String, String> runnerParams) {

    final String minVersion = runnerParams.get(RUNNER_MIN_VERSION);
    if (!StringUtil.isEmptyOrSpaces(minVersion)) {
      list.add("-Version");
      list.add(minVersion);
    }
  }

  /**
   * Gets arguments from runner parameter with specified key.
   * Adds them to given pre-constructed list
   *
   * Supports 'escaped' mode: all arguments are passed as one escaped argument.
   * In escaped mode, inside quotes are escaped with triple quotes.
   * Escaped mode is needed parameters passing to the script when direct script execution is used

   *
   * @param args pre-constructed list of arguments
   * @param runnerParams runner parameters
   * @param key runner parameter key
   * @param escape if set to {@code true}, quotes will be escaped with triple quotes, all
   *               found arguments will be passed to the resulting list as one quoted argument
   */
  private void addCustomArguments(@NotNull final List<String> args,
                                  @NotNull final Map<String, String> runnerParams,
                                  @NotNull final String key,
                                  final boolean escape) {
    final List<String> result = new ArrayList<String>();
    final String custom = runnerParams.get(key);
    if (!StringUtil.isEmptyOrSpaces(custom)) {
      for (String _line : custom.split("[\\r\\n]+")) {
        String line = _line.trim();
        if (StringUtil.isEmptyOrSpaces(line)) continue;
        result.addAll(StringUtil.splitHonorQuotes(line));
      }
    }
    if (!result.isEmpty()) {
      if (escape) {
        final ParametersList parametersList = new ParametersList();
        parametersList.addAll(result);
        final String res = "\"" + parametersList.getParametersString().replace("\"", "\"\"\"") + "\"";
        args.add(res);
      } else {
        args.addAll(result);
      }
    }
  }

  private void addExecutionPolicyPreference(@NotNull final List<String> list) {
    final String cmdArg = "-ExecutionPolicy";
    for (String arg: list) {
      if (arg.toLowerCase().contains(cmdArg.toLowerCase())) {
        LOG.info(cmdArg  + " was specified explicitly");
        return;
      }
    }
    list.add(cmdArg);
    list.add("ByPass");
  }

  private void addScriptBody(@NotNull final List<String> args,
                             @NotNull final PowerShellExecutionMode mod,
                             @NotNull final File scriptFile,
                             @NotNull final Map<String, String> runnerParams,
                             @NotNull final Map<String, String> configParams) throws RunBuildException {
    switch (mod) {
      case STDIN:
        args.add("-Command");
        args.add("-");
        args.add("<");
        args.add(scriptFile.getPath());
        break;
      case PS1:
        if (!scriptFile.getPath().toLowerCase().endsWith(".ps1")) {
          throw new RunBuildException("PowerShell script should have '.ps1' extension");
        }
        final boolean useFile = configParams.get(PowerShellConstants.CONFIG_USE_FILE) != null;
        if (useFile) {
          args.add("-File");
        }
        args.add(getPSEscapedPath(scriptFile));
        addCustomArguments(args, runnerParams, RUNNER_SCRIPT_ARGUMENTS, !useFile);
        break;
      default:
        throw new RunBuildException("Unknown ExecutionMode: " + mod);
    }
  }

  /**
   * Escapes file path (if it contains spaces) with {@code `}
   * Handles name of the script as well
   *
   * http://blogs.technet.com/b/heyscriptingguy/archive/2012/08/07/powertip-run-a-powershell-script-with-space-in-the-path.aspx
   *
   * @param scriptFile file to take path to
   * @return escaped path to the script
   */
  private String getPSEscapedPath(@NotNull final File scriptFile) {
    return StringUtil.replace(scriptFile.getPath(), " ", "` ");
  }
}

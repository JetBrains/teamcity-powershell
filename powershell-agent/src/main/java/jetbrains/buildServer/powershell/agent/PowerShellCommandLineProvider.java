/*
 * Copyright 2000-2020 JetBrains s.r.o.
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
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
                                         @NotNull final Map<String, String> sharedConfigParams) throws RunBuildException {
    final List<String> result = new ArrayList<>();
    final PowerShellExecutionMode mod = PowerShellExecutionMode.fromString(runnerParams.get(RUNNER_EXECUTION_MODE));
    if (mod == null) {
      throw new RunBuildException("'" + RUNNER_EXECUTION_MODE + "' runner parameter is not defined");
    }
    addVersion(result, runnerParams, info); // version must be the 1st arg after executable path
    if (!StringUtil.isEmptyOrSpaces(runnerParams.get(RUNNER_NO_PROFILE))) {
      result.add("-NoProfile");
    }
    result.add("-NonInteractive");

    addCustomArguments(result, runnerParams, sharedConfigParams, RUNNER_CUSTOM_ARGUMENTS);
    if (useExecutionPolicy) {
      addExecutionPolicyPreference(result);
    }

    addScriptBody(result, mod, scriptFile, runnerParams, sharedConfigParams);
    return result;
  }

  private void addVersion(@NotNull final List<String> list,
                          @NotNull final Map<String, String> runnerParams,
                          @NotNull final PowerShellInfo info) {
    if (isExplicitVersionSupported(info)) {
      final String minVersion = runnerParams.get(RUNNER_MIN_VERSION);
      if (!StringUtil.isEmptyOrSpaces(minVersion)) {
        list.add("-Version");
        list.add(minVersion);
      }
    }
  }

  /**
   * Gets arguments from runner parameter with specified key.
   * Adds them to given pre-constructed list
   *  @param args pre-constructed list of arguments
   * @param runnerParams runner parameters
   * @param sharedConfigParams shared configuration parameters
   * @param key runner parameter key
   */
  private void addCustomArguments(@NotNull final List<String> args,
                                  @NotNull final Map<String, String> runnerParams,
                                  Map<String, String> sharedConfigParams,
                                  @NotNull final String key) {
    final List<String> result = new ArrayList<>();
    final String custom = runnerParams.get(key);
    if (!StringUtil.isEmptyOrSpaces(custom)) {
      List<String> lines;
      if (Boolean.parseBoolean(sharedConfigParams.get(PARAM_ARGS_MULTILINE))) {
        lines = Collections.singletonList(custom.replace('\r', ' ').replace('\n', ' '));
      } else {
        lines = StringUtil.split(custom, true, '\r', '\n');
      }
      lines.stream().map(String::trim)
              .filter(it -> !StringUtil.isEmptyOrSpaces(it))
              .map(StringUtil::splitHonorQuotes)
              .forEach(result::addAll);
    }
    args.addAll(result);
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
                             @NotNull final Map<String, String> sharedConfigParams) throws RunBuildException {
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
        args.add("-File");
        args.add(scriptFile.getPath());
        addCustomArguments(args, runnerParams, sharedConfigParams, RUNNER_SCRIPT_ARGUMENTS);
        break;
      default:
        throw new RunBuildException("Unknown ExecutionMode: " + mod);
    }
  }

  static boolean isExplicitVersionSupported(@NotNull final PowerShellInfo info) {
    return SystemInfo.isWindows
        && info.getEdition() == PowerShellEdition.DESKTOP
        && !info.isVirtual();
  }
}

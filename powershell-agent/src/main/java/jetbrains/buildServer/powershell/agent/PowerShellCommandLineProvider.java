/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import static jetbrains.buildServer.powershell.common.PowerShellConstants.RUNNER_EXECUTION_MODE;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.RUNNER_NO_PROFILE;

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
                                         final boolean useExecutionPolicy) throws RunBuildException {
    final List<String> result = new ArrayList<String>();

    result.add(info.getExecutablePath());
    addVersion(result, info); // version must be the 1st arg after executable path
    if (!StringUtil.isEmptyOrSpaces(runnerParams.get(RUNNER_NO_PROFILE))) {
      result.add("-NoProfile");
    }
    result.add("-NonInteractive");

    addCustomArguments(result, runnerParams);
    if (useExecutionPolicy) {
      addExecutionPolicyPreference(result);
    }

    PowerShellExecutionMode mod = PowerShellExecutionMode.fromString(runnerParams.get(RUNNER_EXECUTION_MODE));
    if (mod == null) {
      throw new RunBuildException("'" + RUNNER_EXECUTION_MODE + "' runner parameters is not defined");
    }
    addScriptBody(result, mod, scriptFile);
    return result;
  }

  private void addVersion(@NotNull final List<String> list, @NotNull final PowerShellInfo info) {
    list.add("-Version");
    list.add(info.getVersion().getVersion());
  }

  private void addCustomArguments(@NotNull final List<String> args,
                                  @NotNull final Map<String, String> runnerParams) {
    final String custom = runnerParams.get(PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS);
    if (!StringUtil.isEmptyOrSpaces(custom)) {
      for (String _line : custom.split("[\\r\\n]+")) {
        String line = _line.trim();
        if (StringUtil.isEmptyOrSpaces(line)) continue;
        args.addAll(StringUtil.splitHonorQuotes(line));
      }
    }
  }

  private void addExecutionPolicyPreference(@NotNull final List<String> list) {
    final String cmdArg = "-ExecutionPolicy";
    for (String arg : list) {
      if (arg.trim().toLowerCase().contains(cmdArg.toLowerCase())) {
        LOG.info(cmdArg  + " was specified explicitly");
        return;
      }
    }
    list.add(cmdArg);
    list.add("ByPass");
  }

  private void addScriptBody(@NotNull final List<String> args,
                             @NotNull final PowerShellExecutionMode mod,
                             @NotNull final File scriptFile) throws RunBuildException {
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
        break;
      default:
        throw new RunBuildException("Unknown ExecutionMode: " + mod);
    }
  }
}

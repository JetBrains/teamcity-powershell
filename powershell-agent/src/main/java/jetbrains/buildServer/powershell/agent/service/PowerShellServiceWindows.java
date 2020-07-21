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

package jetbrains.buildServer.powershell.agent.service;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import jetbrains.buildServer.powershell.agent.PowerShellCommandLineProvider;
import jetbrains.buildServer.powershell.agent.PowerShellInfoProvider;
import jetbrains.buildServer.powershell.agent.ScriptGenerator;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.agent.system.PowerShellCommands;
import jetbrains.buildServer.powershell.agent.virtual.VirtualPowerShellSupport;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 16:47
 */
public class PowerShellServiceWindows extends BasePowerShellService {

  private static final Logger LOG = Logger.getInstance(PowerShellServiceWindows.class.getName());

  public PowerShellServiceWindows(@NotNull final PowerShellInfoProvider infoProvider,
                                  @NotNull final ScriptGenerator scriptGenerator,
                                  @NotNull final PowerShellCommandLineProvider cmdProvider,
                                  @NotNull final PowerShellCommands commands,
                                  @NotNull final VirtualPowerShellSupport virtualSupport) {
    super(infoProvider, scriptGenerator, cmdProvider, commands, virtualSupport);
  }

  @Override
  protected SimpleProgramCommandLine getStdInCommandLine(@NotNull final PowerShellInfo info,
                                                         @NotNull final Map<String, String> env,
                                                         @NotNull final String workDir,
                                                         @NotNull final String command) throws RunBuildException {
    final List<String> args = generateRunScriptArguments(command);
    final String executable = myCommands.getCMDWrappedCommand(info, getEnvironmentVariables());
    final BuildProgressLogger buildLogger = getBuild().getBuildLogger();
    buildLogger.message("Executable wrapper: " + executable);
    buildLogger.message("Wrapper arguments: " + Arrays.toString(args.toArray()));
    buildLogger.message("Command: " + command);
    return new SimpleProgramCommandLine(env, workDir, executable, args);
  }

  @Override
  protected SimpleProgramCommandLine getFileCommandLine(@NotNull final PowerShellInfo info,
                                                        @NotNull final Map<String, String> env,
                                                        @NotNull final String workDir,
                                                        @NotNull final List<String> args) {
    final BuildProgressLogger buildLogger = getBuild().getBuildLogger();
    final String command = myCommands.getNativeCommand(info, getRunnerContext());
    buildLogger.message("Command: " + command);
    buildLogger.message("PowerShell arguments: " + StringUtil.join(args, ", "));
    return new SimpleProgramCommandLine(env, workDir, command, args);
  }

  @NotNull
  private List<String> generateRunScriptArguments(@NotNull final String argumentsToGenerate) throws RunBuildException {
    final File bat;
    try {
      bat = FileUtil.createTempFile(getBuildTempDirectory(), "powershell", ".bat", true);
      myFilesToRemove.add(bat);
      FileUtil.writeFileAndReportErrors(bat, "@" + argumentsToGenerate);
    } catch (IOException e) {
      throw new RunBuildException("Failed to generate .bat file");
    }
    return Arrays.asList("/c", bat.getPath());
  }

  private boolean isInternalPropertySetExecutionPolicy(@NotNull final String name, boolean def) {
    final String prop = getConfigParameters().get("teamcity.powershell." + name);
    if (StringUtil.isEmptyOrSpaces(prop)) {
      return def;
    } else {
      return "true".equalsIgnoreCase(prop);
    }
  }

  @Override
  @NotNull
  protected final Map<String, String> getEnv(@NotNull PowerShellInfo info) {
    Map<String, String> map = getEnvironmentVariables();
    // check internal property
    // supported only by powershell of version > 1 ('==' is therefore used)
    if (!isInternalPropertySetExecutionPolicy("set.executionPolicyEnv", info.getVersion().equals("1.0"))) return map;

    final String env = "PSExecutionPolicyPreference";

    //check if user had overridden the value
    if (map.containsKey(env)) {
      LOG.info(env + " environment variable was specified explicitly");
      return map;
    }

    map = new TreeMap<>(getEnvironmentVariables());
    map.put(env, "ByPass");
    return map;
  }

  private static final String EXECUTION_POLICY_MIN_VERSION = "1.0";

  /**
   * PowerShell {@code v1.0} does not support execution policy.
   * @param info PowerShell to use
   *
   * @return {@code true} if version of PowerShell is greater than {@code 1.0} and execution policy
   * setting is not overridden by internal property {@code teamcity.powershell.set.executionPolicyArg}
   */
  @Override
  protected boolean useExecutionPolicy(@NotNull final PowerShellInfo info) {
    return isInternalPropertySetExecutionPolicy(
        "set.executionPolicyArg",
        !EXECUTION_POLICY_MIN_VERSION.equals(info.getVersion()));
  }
}

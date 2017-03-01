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
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.agent.system.PowerShellCommands;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static jetbrains.buildServer.powershell.common.PowerShellBitness.fromString;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.*;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 16:47
 */
public class PowerShellService extends BuildServiceAdapter {

  private static final Logger LOG = Logger.getInstance(PowerShellService.class.getName());

  @NotNull
  private final Collection<File> myFilesToRemove = new ArrayList<File>();

  @NotNull
  private final PowerShellInfoProvider myInfoProvider;

  @NotNull
  private final PowerShellCommandLineProvider myCmdProvider;

  @NotNull
  private final ScriptGenerator myScriptGenerator;

  @NotNull
  private final PowerShellCommands myCommands;

  public PowerShellService(@NotNull final PowerShellInfoProvider powerShellInfoProvider,
                           @NotNull final PowerShellCommandLineProvider cmdProvider,
                           @NotNull final ScriptGenerator scriptGenerator,
                           @NotNull final PowerShellCommands powerShellCommands) {
    myInfoProvider = powerShellInfoProvider;
    myCmdProvider = cmdProvider;
    myScriptGenerator = scriptGenerator;
    myCommands = powerShellCommands;
  }

  @Override
  public boolean isCommandLineLoggingEnabled() {
    return false;
  }

  @NotNull
  @Override
  public List<ProcessListener> getListeners() {
    final boolean logToError = !StringUtil.isEmptyOrSpaces(getRunnerParameters().get(PowerShellConstants.RUNNER_LOG_ERR_TO_ERROR));
    final BuildProgressLogger logger = getLogger();
    return Collections.<ProcessListener>singletonList(new ProcessListenerAdapter() {
      private final org.apache.log4j.Logger OUT_LOG = org.apache.log4j.Logger.getLogger("teamcity.out");
      @Override
      public void onStandardOutput(@NotNull final String text) {
        logger.message(text);
        OUT_LOG.info(text);
      }

      @Override
      public void onErrorOutput(@NotNull final String text) {
        if (logToError) {
          logger.error(text);
        } else {
          logger.warning(text);
        }
        OUT_LOG.warn(text);
      }
    });
  }

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    final PowerShellInfo info = selectTool();
    final String psExecutable = info.getExecutablePath();
    final String workDir = getWorkingDirectory().getPath();
    final PowerShellExecutionMode mode = PowerShellExecutionMode.fromString(getRunnerParameters().get(RUNNER_EXECUTION_MODE));
    final BuildProgressLogger buildLogger = getBuild().getBuildLogger();
    buildLogger.message("PowerShell Executable: " + psExecutable);
    buildLogger.message("Working directory: " + workDir);
    if (PowerShellExecutionMode.STDIN == mode) {
      // stdin mode: wrapping call to powershell.exe with cmd, powershell.exe goes 1st argument in cmd file
      final String command = generateCommand(info);
      buildLogger.message("PowerShell command: " + command);
      final String executable = myCommands.getCMDWrappedCommand(info, getEnvironmentVariables());
      final List<String> args = new ArrayList<String>();
      args.addAll(generateRunScriptArguments(command));
      buildLogger.message("Executable wrapper: " + executable);
      buildLogger.message("Wrapper arguments: " + Arrays.toString(args.toArray()));
      return new SimpleProgramCommandLine(
              getActualEnvironmentVariables(info),
              workDir,
              executable,
              args
      );
    } else {
      final List<String> args = generateArguments(info);
      buildLogger.message("PowerShell arguments: " + Arrays.toString(args.toArray()));
      return new SimpleProgramCommandLine(
              getActualEnvironmentVariables(info),
              workDir,
              myCommands.getNativeCommand(info),
              args
      );
    }
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

  @NotNull
  private Map<String, String> getActualEnvironmentVariables(@NotNull PowerShellInfo info) {
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

    map = new TreeMap<String, String>(getEnvironmentVariables());
    map.put(env, "ByPass");
    return map;
  }

  /**
   * PowerShell {@code v1.0} does not support execution policy.
   * @param info PowerShell to use
   *
   * @return {@code true} if version of PowerShell is greater than {@code 1.0} and execution policy
   * setting is not overridden by internal property {@code teamcity.powershell.set.executionPolicyArg}
   */
  private boolean useExecutionPolicy(@NotNull final PowerShellInfo info) {
    return isInternalPropertySetExecutionPolicy("set.executionPolicyArg", !info.getVersion().equals("1.0"));
  }

  @Override
  public void afterProcessFinished() throws RunBuildException {
    super.afterProcessFinished();
    if (!shouldKeepGeneratedFiles()) {
      for (File file: myFilesToRemove) {
        FileUtil.delete(file);
      }
      myFilesToRemove.clear();
    }
  }

  private PowerShellInfo selectTool() throws RunBuildException {
    final PowerShellBitness bit = fromString(getRunnerParameters().get(RUNNER_BITNESS));
    if (bit == null) throw new RunBuildException("Failed to read: " + RUNNER_BITNESS);
    for (PowerShellInfo info : myInfoProvider.getPowerShells()) {
      if (info.getBitness() == bit) {
        return info;
      }
    }

    throw new RunBuildException("PowerShell " + bit + " was not found");
  }

  private List<String> generateArguments(@NotNull final PowerShellInfo info) throws RunBuildException {
    final Map<String, String> runnerParameters = getRunnerParameters();
    final File scriptFile = myScriptGenerator.generateScript(runnerParameters, getCheckoutDirectory(), getBuildTempDirectory());
    // if  we have script entered in runner params it will be dumped to temp file. This file must be removed after build finishes
    if (myScriptGenerator.shouldRemoveGeneratedScript(runnerParameters)) {
      myFilesToRemove.add(scriptFile);
    }
    return myCmdProvider.provideCommandLine(runnerParameters, scriptFile, useExecutionPolicy(info));
  }

  @NotNull
  private String generateCommand(@NotNull final PowerShellInfo info) throws RunBuildException {
    final ParametersList parametersList = new ParametersList();
    final Map<String, String> runnerParameters = getRunnerParameters();
    final File scriptFile = myScriptGenerator.generateScript(runnerParameters, getCheckoutDirectory(), getBuildTempDirectory());

    // if  we have script entered in runner params it will be dumped to temp file. This file must be removed after build finishes
    if (myScriptGenerator.shouldRemoveGeneratedScript(runnerParameters)) {
      myFilesToRemove.add(scriptFile);
    }
    parametersList.add(info.getExecutablePath());
    parametersList.addAll(
            myCmdProvider.provideCommandLine(runnerParameters, scriptFile, useExecutionPolicy(info))
    );
    return parametersList.getParametersString();
  }

  private boolean shouldKeepGeneratedFiles() {
    return getConfigParameters().containsKey(CONFIG_KEEP_GENERATED) || getConfigParameters().containsKey("teamcity.dont.delete.temp.files");
  }
}

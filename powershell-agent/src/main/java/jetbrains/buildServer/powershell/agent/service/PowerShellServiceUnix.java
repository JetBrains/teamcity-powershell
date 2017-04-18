/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.service;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ParametersList;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import jetbrains.buildServer.powershell.agent.PowerShellCommandLineProvider;
import jetbrains.buildServer.powershell.agent.PowerShellInfoProvider;
import jetbrains.buildServer.powershell.agent.ScriptGenerator;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.agent.system.PowerShellCommands;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class PowerShellServiceUnix extends BasePowerShellService {

  public PowerShellServiceUnix(@NotNull final PowerShellInfoProvider infoProvider,
                               @NotNull final ScriptGenerator scriptGenerator,
                               @NotNull final PowerShellCommandLineProvider cmdProvider,
                               @NotNull final PowerShellCommands commands) {
    super(infoProvider, scriptGenerator, cmdProvider, commands);
  }

  @Override
  protected SimpleProgramCommandLine getStdInCommandLine(@NotNull final PowerShellInfo info,
                                                         @NotNull final Map<String, String> env,
                                                         @NotNull final String workDir,
                                                         @NotNull final String command) throws RunBuildException {
    return executeWithWrapper(env, workDir, command);
  }

  @Override
  protected SimpleProgramCommandLine getFileCommandLine(@NotNull final PowerShellInfo info,
                                                        @NotNull final Map<String, String> env,
                                                        @NotNull final String workDir,
                                                        @NotNull final List<String> args) throws RunBuildException {
    final ParametersList parametersList = new ParametersList();
    parametersList.add(info.getExecutablePath());
    parametersList.addAll(args);
    return executeWithWrapper(env, workDir, parametersList.getParametersString());
  }

  private SimpleProgramCommandLine executeWithWrapper(@NotNull final Map<String, String> env,
                                                      @NotNull final String workDir,
                                                      @NotNull final String argsList) throws RunBuildException {
    final File scriptFile = generateNixScriptFile(argsList);
    final BuildProgressLogger buildLogger = getBuild().getBuildLogger();
    buildLogger.message("Wrapper script: " + scriptFile);
    buildLogger.message("Command: " + argsList);
    enableExecution(scriptFile);
    return new SimpleProgramCommandLine(env, workDir, scriptFile.getAbsolutePath(), Collections.<String>emptyList());
  }

  @Override
  protected boolean useExecutionPolicy(@NotNull final PowerShellInfo info) {
    return false;
  }

  @Override
  protected Map<String, String> getEnv(@NotNull final PowerShellInfo info) {
    return getEnvironmentVariables();
  }

  @NotNull
  private File generateNixScriptFile(@NotNull final String argumentsToGenerate) throws RunBuildException {
    final File script;
    try {
      script = FileUtil.createTempFile(getBuildTempDirectory(), "powershell_gen_" + System.currentTimeMillis(), ".sh", true);
      myFilesToRemove.add(script);
      FileUtil.writeFileAndReportErrors(script, argumentsToGenerate);
    } catch (IOException e) {
      throw new RunBuildException("Failed to generate .sh wrapper file");
    }
    return script;
  }

  private static void enableExecution(@NotNull final File filePath) {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    
    commandLine.setExePath("chmod");
    commandLine.addParameter("+x");
    commandLine.addParameter(filePath.getName());
    commandLine.setWorkDirectory(filePath.getParent());

    final ExecResult execResult = SimpleCommandLineProcessRunner.runCommand(commandLine, null);
    if (execResult.getExitCode() != 0) {
      LOG.warn("Failed to set executable attribute for " + filePath + ": chmod +x exit code is " + execResult.getExitCode());
    }
  }
}

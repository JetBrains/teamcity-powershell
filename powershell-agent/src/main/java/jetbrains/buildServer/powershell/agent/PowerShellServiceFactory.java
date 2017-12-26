/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */
package jetbrains.buildServer.powershell.agent;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import jetbrains.buildServer.powershell.agent.service.PowerShellServiceUnix;
import jetbrains.buildServer.powershell.agent.service.PowerShellServiceWindows;
import jetbrains.buildServer.powershell.agent.service.ProfileWriter;
import jetbrains.buildServer.powershell.agent.system.PowerShellCommands;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import org.jetbrains.annotations.NotNull;

public class PowerShellServiceFactory implements CommandLineBuildServiceFactory, AgentBuildRunnerInfo {

  private static final Logger LOG = Logger.getInstance(PowerShellServiceFactory.class.getName());

  @NotNull
  private final PowerShellInfoProvider myInfoProvider;

  @NotNull
  private final PowerShellCommandLineProvider myCmdProvider;

  @NotNull
  private final ScriptGenerator myGenerator;
  
  @NotNull
  private final PowerShellCommands myCommands;
  @NotNull
  private final ProfileWriter myProfileWriter;

  public PowerShellServiceFactory(@NotNull final PowerShellInfoProvider powerShellInfoProvider,
                                  @NotNull final PowerShellCommandLineProvider cmdProvider,
                                  @NotNull final ScriptGenerator generator,
                                  @NotNull final PowerShellCommands powerShellCommands,
                                  @NotNull final ProfileWriter profileWriter) {
    myInfoProvider = powerShellInfoProvider;
    myCmdProvider = cmdProvider;
    myGenerator = generator;
    myCommands = powerShellCommands;
    myProfileWriter = profileWriter;
  }

  @NotNull
  public CommandLineBuildService createService() {
    if (SystemInfo.isWindows) {
      return new PowerShellServiceWindows(myInfoProvider, myGenerator, myCmdProvider, myCommands, myProfileWriter);
    } else {
      return new PowerShellServiceUnix(myInfoProvider, myGenerator, myCmdProvider, myCommands, myProfileWriter);
    }
  }

  @NotNull
  public AgentBuildRunnerInfo getBuildRunnerInfo() {
    return this;
  }

  @NotNull
  public String getType() {
    return PowerShellConstants.RUN_TYPE;
  }

  public boolean canRun(@NotNull final BuildAgentConfiguration agentConfiguration) {     
    if (!myInfoProvider.anyPowerShellDetected()) {
      LOG.info("PowerShell runner is disabled: PowerShell was not found.");
      return false;
    }
    return true;
  }
}
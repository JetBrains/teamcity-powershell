
package jetbrains.buildServer.powershell.agent;

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import jetbrains.buildServer.powershell.agent.service.PowerShellServiceUnix;
import jetbrains.buildServer.powershell.agent.service.PowerShellServiceWindows;
import jetbrains.buildServer.powershell.agent.system.PowerShellCommands;
import jetbrains.buildServer.powershell.agent.virtual.VirtualPowerShellSupport;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import org.jetbrains.annotations.NotNull;

public class PowerShellServiceFactory implements CommandLineBuildServiceFactory, AgentBuildRunnerInfo {

  @NotNull
  private final PowerShellInfoProvider myInfoProvider;

  @NotNull
  private final PowerShellCommandLineProvider myCmdProvider;

  @NotNull
  private final ScriptGenerator myGenerator;
  
  @NotNull
  private final PowerShellCommands myCommands;

  @NotNull
  private final VirtualPowerShellSupport myVirtualSupport;

  public PowerShellServiceFactory(@NotNull final PowerShellInfoProvider powerShellInfoProvider,
                                  @NotNull final PowerShellCommandLineProvider cmdProvider,
                                  @NotNull final ScriptGenerator generator,
                                  @NotNull final PowerShellCommands powerShellCommands,
                                  @NotNull final VirtualPowerShellSupport virtualPowerShellSupport) {
    myInfoProvider = powerShellInfoProvider;
    myCmdProvider = cmdProvider;
    myGenerator = generator;
    myCommands = powerShellCommands;
    myVirtualSupport = virtualPowerShellSupport;
  }

  @NotNull
  public CommandLineBuildService createService() {
    if (SystemInfo.isWindows) {
      return new PowerShellServiceWindows(myInfoProvider, myGenerator, myCmdProvider, myCommands, myVirtualSupport);
    } else {
      return new PowerShellServiceUnix(myInfoProvider, myGenerator, myCmdProvider, myCommands, myVirtualSupport);
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
    return true;
  }
}
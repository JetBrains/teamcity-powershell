package jetbrains.buildServer.powershell.agent;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import org.jetbrains.annotations.NotNull;

public class PowerShellServiceFactory implements CommandLineBuildServiceFactory, AgentBuildRunnerInfo {
  private static final Logger LOG = Logger.getInstance(PowerShellServiceFactory.class.getName());
  private final PowerShellInfoProvider myInfos;

  public PowerShellServiceFactory(@NotNull final PowerShellInfoProvider infos) {
    myInfos = infos;
  }

  @NotNull
  public CommandLineBuildService createService() {
    return new PowerShellService(myInfos);
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
    final boolean isEmpty = myInfos.getPowerShells().isEmpty();
    if (isEmpty) {
      LOG.info("Powershell runner is disabled: Powershell was not found.");
      return false;
    }
    return true;
  }
}
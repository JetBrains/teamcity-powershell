package jetbrains.buildServer.powershell.agent.service;

import jetbrains.buildServer.RunBuildException;
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
import java.util.Arrays;
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
                                                         @NotNull final String command,
                                                         @NotNull final List<String> args) throws RunBuildException {
    final BuildProgressLogger buildLogger = getBuild().getBuildLogger();
    final String executable = myCommands.getShWrappedCommand() ;
    args.addAll(generateNixRunScriptArguments(command));
    buildLogger.message("Executable wrapper: " + executable);
    buildLogger.message("Wrapper arguments: " + Arrays.toString(args.toArray()));
    return new SimpleProgramCommandLine(env, workDir, executable, args);
  }

  @Override
  protected SimpleProgramCommandLine getFileCommandLine(@NotNull final PowerShellInfo info,
                                                        @NotNull final Map<String, String> env,
                                                        @NotNull final String workDir,
                                                        @NotNull final List<String> args) throws RunBuildException {
    return new SimpleProgramCommandLine(env, workDir, info.getExecutablePath(), args);
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
  private List<String> generateNixRunScriptArguments(@NotNull final String argumentsToGenerate) throws RunBuildException {
    final File script;
    try {
      script = FileUtil.createTempFile(getBuildTempDirectory(), "powershell", ".sh", true);
      myFilesToRemove.add(script);
      FileUtil.writeFileAndReportErrors(script, argumentsToGenerate);
    } catch (IOException e) {
      throw new RunBuildException("Failed to generate .sh wrapper file");
    }
    return Collections.singletonList(script.getPath());
  }
}

package jetbrains.buildServer.powershell.agent.detect.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.powershell.agent.Loggers;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class DetectionRunner {

  private static final Logger LOG = Loggers.DETECTION_LOGGER;

  /**
   * Runs detection script
   *
   * @param executablePath executable to run script with
   * @param detectionScriptPath file containing detection script
   * @param additionalParameters additional parameters for script runner
   * @return lines from stdout
   * @throws ExecutionException if there was an error during execution
   */
  List<String> runDetectionScript(@NotNull final String executablePath,
                                  @NotNull final String detectionScriptPath,
                                  @NotNull final List<String> additionalParameters) throws ExecutionException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(executablePath);
    cl.addParameter("-NoProfile");
    for (String str: additionalParameters) {
      cl.addParameter(str);
    }
    cl.addParameter("-File");
    cl.addParameter(detectionScriptPath);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Running detection script using command line: " + cl.getCommandLineString());
    }
    return runProcess(cl).getStdoutLines();
  }

  private ProcessOutput runProcess(@NotNull final GeneralCommandLine cl) throws ExecutionException {
    final int attempts = TeamCityProperties.getInteger("teamcity.powershell.detector.attempts", 1);
    final int timeout = TeamCityProperties.getInteger("teamcity.powershell.detector.timeout.msec", 20000);
    for (int attempt = 1; attempt <= attempts; attempt++) {
      final CapturingProcessHandler handler = new CapturingProcessHandler(cl.createProcess(), StandardCharsets.UTF_8);
      final ProcessOutput result = handler.runProcess(timeout);
      final String errorOutput = result.getStderr();
      if (!isEmptyOrSpaces(errorOutput)) {
        logProcessOutput(result);
        throw new ExecutionException(errorOutput);
      }
      if (result.isTimeout()) {
        logProcessOutput(result);
        int leftAttempts = attempts - attempt;
        if (leftAttempts > 0) {
          LOG.warn(String.format("PowerShell detection timed out, %d %s left",
                  leftAttempts, StringUtil.pluralize("attempt", leftAttempts)
          ));
          continue;
        }
        throw new ExecutionException("Process execution of [" + cl.getCommandLineString() + "] has timed out. Timeout is set to " + timeout + " msec.");
      }
      return result;
    }
    throw new ExecutionException("Failed to detect PowerShell"); // must be unreachable
  }

  private void logProcessOutput(@NotNull final ProcessOutput output) {
    final String stdOut = output.getStdout().trim();
    final String stdErr = output.getStderr().trim();
    StringBuilder b = new StringBuilder("PowerShell detection script output: \n");
    if (!isEmptyOrSpaces(stdOut)) {
      b.append("\n----- StdOut: -----\n").append(stdOut).append("\n");
    }
    if (!isEmptyOrSpaces(stdErr)) {
      b.append("\n----- StdErr: -----\n").append(stdErr).append("\n");
    }
    LOG.warn(b.toString());
  }

}
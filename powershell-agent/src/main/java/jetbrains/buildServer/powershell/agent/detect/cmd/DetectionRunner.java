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

package jetbrains.buildServer.powershell.agent.detect.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.powershell.agent.Loggers;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.List;

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
   * @return lines from stdout
   * @throws ExecutionException if there was an error during execution
   */
  List<String> runDetectionScript(@NotNull final String executablePath,
                                  @NotNull final String detectionScriptPath) throws ExecutionException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(executablePath);
    cl.addParameter("-NoProfile");
    cl.addParameter("-File");
    cl.addParameter(detectionScriptPath);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Running detection script using command line: " + cl.getCommandLineString());
    }
    final ProcessOutput execResult = runProcess(cl);
    return execResult.getStdoutLines();
  }

  private static ProcessOutput runProcess(@NotNull final GeneralCommandLine cl) throws ExecutionException {
    final CapturingProcessHandler handler = new CapturingProcessHandler(cl.createProcess(), Charset.forName("UTF-8"));
    final int timeout = TeamCityProperties.getInteger("teamcity.powershell.detector.timeout.msec", 20000);
    final ProcessOutput result = handler.runProcess(timeout);
    if (result.isTimeout()) {
      throw new ExecutionException("Process execution of [" + cl.getCommandLineString() + "] has timed out. Timeout is set to " + timeout + " msec.");
    }
    final String errorOutput = result.getStderr();
    if (!StringUtil.isEmptyOrSpaces(errorOutput)) {
      throw new ExecutionException(errorOutput);
    }
    return result;
  }
}

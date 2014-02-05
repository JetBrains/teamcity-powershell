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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.powershell.common.PowerShellVersion;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Map;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ScriptGenerator {

  private static final Logger LOG = Logger.getInstance(ScriptGenerator.class.getName());

  private static String[][] WRAPPINGS = new String[][] {
          {"trap { $host.SetShouldExit(1) }\r\n", "\r\n"},
          {"try\r\n {\r\n", "}\r\ncatch\r\n{\r\nWrite-Error -Exception $_.Exception\r\nExit 1\r\n}\r\n"}
  };

  /**
   * Wraps script (in a file or in plain text in error handling routines).
   * Creates new file inside checkout directory
   *
   * @param info powershell, that will execute script
   * @param runnerParameters runner parameters
   * @param buildCheckoutDir checkout directory
   * @param buildTempDir tmp directory
   * @return {@code file} with patched script to be used by runner
   * @throws RunBuildException if error occurs
   */
  @NotNull
  public File generate(@NotNull final PowerShellInfo info,
                       @NotNull final Map<String, String> runnerParameters,
                       @NotNull final File buildCheckoutDir,
                       @NotNull final File buildTempDir) throws RunBuildException {
    final PowerShellExecutionMode executionMode = PowerShellExecutionMode.fromString(runnerParameters.get(RUNNER_EXECUTION_MODE));
    // determine script mode
    final PowerShellScriptMode scriptMode = PowerShellScriptMode.fromString(runnerParameters.get(RUNNER_SCRIPT_MODE));
    // determine execution version
    String minVersion = runnerParameters.get(RUNNER_MIN_VERSION);
    if (isEmptyOrSpaces(minVersion)) {
      minVersion = info.getVersion().getVersion();          }
    final String[] wrappings = getWrappings(minVersion);
    Closeable handle = null;
    try {
      String sourceScript;
      String location = null;
      // get sourceScript code from file or from code
      if (PowerShellScriptMode.FILE == scriptMode) {
        final File scriptFile = FileUtil.resolvePath(buildCheckoutDir, runnerParameters.get(RUNNER_SCRIPT_FILE));
        sourceScript = FileUtil.readText(scriptFile);
        location = scriptFile.getParentFile().getCanonicalPath();
      } else {
        sourceScript = runnerParameters.get(RUNNER_SCRIPT_CODE);
        if (isEmptyOrSpaces(sourceScript)) {
          throw new RunBuildException("Empty build script");
        }
        //some newlines are necessary to workaround -Command - issues, like TW-19771
        sourceScript = "  \r\n  \r\n  \r\n" + jetbrains.buildServer.util.StringUtil.convertLineSeparators(sourceScript, "\r\n") + "\r\n  \r\n   \r\n   ";
      }
      // create new script file
      final File code = FileUtil.createTempFile(buildTempDir, "powershell", ".ps1", true);
      OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(code), "utf-8");
      handle = w;
      if (PowerShellExecutionMode.PS1 == executionMode) {
        // in case of file - execute script in source file dir
        if (!isEmptyOrSpaces(location)) {
          // write change location
          w.write("Set-Location ");
          w.write(location);
          w.write("\r\n");
        }
        // write prefix
        w.write(wrappings[0]);
        // write script
        w.write(sourceScript);
        // write suffix
        w.write(wrappings[1]);
      } else {
        w.write(sourceScript);
      }
      return code;
    } catch (IOException e) {
      LOG.error("Error occured while processing file for powershell script", e);
      throw new RunBuildException("Failed to generate temporary resulting powershell script due to exception", e);
    } finally {
      FileUtil.close(handle);
    }
  }

  private String[] getWrappings(@NotNull final String minVersion) {
    return minVersion.equals(PowerShellVersion.V_1_0.getVersion()) ? WRAPPINGS[0] : WRAPPINGS[1];
  }

}

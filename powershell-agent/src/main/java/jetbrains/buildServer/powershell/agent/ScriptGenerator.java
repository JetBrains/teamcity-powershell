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
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Map;

import static com.intellij.openapi.util.text.StringUtil.convertLineSeparators;
import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ScriptGenerator {

  private static final Logger LOG = Logger.getInstance(ScriptGenerator.class.getName());
  private static final char BOM = '\ufeff';

  /**
   * Gets script source file either from parameter by dumping it to temp file
   * or from file, specified in parameters
   *
   * @param runnerParameters runner parameters
   * @param buildCheckoutDir checkout directory
   * @param buildTempDir temp directory
   * @return if {@code PowerShellScriptMode.FILE} is used - file, that corresponds to {@code RUNNER_SCRIPT_FILE} param,
   * if {@code PowerShellScriptMode.CODE} is used - temp file, containing code from {@code RUNNER_SCRIPT_CODE} param
   *
   * @throws RunBuildException if value if {@code RUNNER_SCRIPT_CODE} param is empty, or file handling error occurred
   */
  @NotNull
  public File generateScript(@NotNull final Map<String, String> runnerParameters,
                      @NotNull final File buildCheckoutDir,
                      @NotNull final File buildTempDir) throws RunBuildException {
    final PowerShellScriptMode scriptMode = PowerShellScriptMode.fromString(runnerParameters.get(RUNNER_SCRIPT_MODE));
    File scriptFile;
    if (PowerShellScriptMode.FILE == scriptMode) {
      scriptFile = FileUtil.resolvePath(buildCheckoutDir, runnerParameters.get(RUNNER_SCRIPT_FILE));
    } else {
      String sourceScript = runnerParameters.get(RUNNER_SCRIPT_CODE);
      if (isEmptyOrSpaces(sourceScript)) {
        throw new RunBuildException("Empty build script");
      }
      sourceScript = convertLineSeparators(sourceScript, "\r\n");
      /*if (PowerShellExecutionMode.STDIN.equals(PowerShellExecutionMode.fromString(runnerParameters.get(RUNNER_EXECUTION_MODE)))) {
        //some newlines are necessary to workaround -Command - issues, like TW-19771
        sourceScript = "  \r\n  \r\n  \r\n" + sourceScript + "\r\n  \r\n   \r\n   ";
      }*/
      scriptFile = writeToTempFile(buildTempDir, sourceScript, runnerParameters);
    }
    if (!scriptFile.isFile()) {
      throw new RunBuildException("Cannot find PowerShell script by path specified in build configuration settings: "
          + scriptFile.getAbsolutePath() + " (absolute path on agent). Please check that the specified path is correct.");
    }
    return scriptFile;
  }

  public static boolean shouldRemoveGeneratedScript(@NotNull final Map<String, String> runnerParameters) {
    return PowerShellScriptMode.CODE == PowerShellScriptMode.fromString(runnerParameters.get(PowerShellConstants.RUNNER_SCRIPT_MODE));
  }

  @NotNull
  private File writeToTempFile(@NotNull final File buildTempDir,
                               @NotNull final String text,
                               @NotNull final  Map<String, String> runnerParameters) throws RunBuildException {
    Closeable handle = null;
    File file;
    try {
      file = FileUtil.createTempFile(buildTempDir, "powershell", ".ps1", true);
      OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
      handle = w;
      if (PowerShellExecutionMode.PS1 == PowerShellExecutionMode.fromString(runnerParameters.get(RUNNER_EXECUTION_MODE))) {
        w.write(BOM);
      }
      w.write(text);
      return file;
    } catch (IOException e) {
      LOG.error("Error occurred while processing file for PowerShell script", e);
      throw new RunBuildException("Failed to generate temporary resulting PowerShell script due to exception", e);
    } finally {
      FileUtil.close(handle);
    }
  }
}

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
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.powershell.common.PowerShellVersion;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
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

  @NotNull
  private static final String TOKEN_ORIGINAL_LOCATION = "@ORIGINAL_LOCATION@";

  @NotNull
  private static final String TOKEN_SCRIPT = "@SCRIPT@";

  private static String[] TEMPLATES = new String[] {"ver1.tpl.ps1", "ver2.tpl.ps1"};

  /**
   * Wraps script (in a file or in plain text in error handling routines).
   * Creates new file inside checkout directory
   *
   * @param info powershell, that will execute script
   * @param runnerParameters runner parameters
   * @param buildCheckoutDir checkout directory
   * @param buildTempDir tmp directory
   * @param noErrorHandling if {@code true}, error handling will be disabled
   * @return {@code file} with patched script to be used by runner
   * @throws RunBuildException if error occurs
   */
  @NotNull
  public File generate(@NotNull final PowerShellInfo info,
                       @NotNull final Map<String, String> runnerParameters,
                       @NotNull final File buildCheckoutDir,
                       @NotNull final File buildTempDir,
                       boolean noErrorHandling) throws RunBuildException {
    final File scriptFile = getScript(runnerParameters, buildCheckoutDir, buildTempDir);
    if (isWrapping(runnerParameters, noErrorHandling)) {
      return wrap(scriptFile, info, buildTempDir, runnerParameters);
    } else {
      return scriptFile;
    }
  }

  @NotNull
  private File wrap(@NotNull File scriptFile,
                    @NotNull final PowerShellInfo info,
                    @NotNull final File buildTempDir,
                    @NotNull final Map<String, String> runnerParameters) throws RunBuildException {
    String location;
    String scriptFilePath;
    try {
      location = scriptFile.getParentFile().getCanonicalPath();
      scriptFilePath = scriptFile.getCanonicalPath();
    } catch (IOException e) {
      LOG.error("Error occured while processing file for powershell script", e);
      throw new RunBuildException("Failed to generate temporary resulting powershell script due to exception", e);
    }
    // determine execution version
    String minVersion = runnerParameters.get(RUNNER_MIN_VERSION);
    if (isEmptyOrSpaces(minVersion)) {
      minVersion = info.getVersion().getVersion();
    }
    // get template
    final String templateContent = getTemplate(minVersion);
    // substitution map
    final Map<String, String> substMap = new HashMap<String, String>();
    substMap.put(TOKEN_ORIGINAL_LOCATION, location);
    substMap.put(TOKEN_SCRIPT, scriptFilePath);
    // process template
    String processed = processTemplate(templateContent, substMap);
    // write template to file
    return writeToTempFile(buildTempDir, processed);

  }

  /**
   * Tells, if this wrapper will be wrapping based on input params and {@code TeamCityProperties}
   * @param runnerParameters runner parameters
   * @param noErrorHandling if {@code true}, error handling will be disabled
   * @return {@code true} if {@code PowerShellExecutionMode.PS1} is used and
   * {@code powershell.disable.error.handling} is not set
   */
  public boolean isWrapping(@NotNull final Map<String, String> runnerParameters, boolean noErrorHandling) {
    final PowerShellExecutionMode executionMode = PowerShellExecutionMode.fromString(runnerParameters.get(RUNNER_EXECUTION_MODE));
    return !noErrorHandling && PowerShellExecutionMode.PS1 == executionMode;
  }

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
  private File getScript(@NotNull final Map<String, String> runnerParameters,
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
      //some newlines are necessary to workaround -Command - issues, like TW-19771
      sourceScript = "  \r\n  \r\n  \r\n" + jetbrains.buildServer.util.StringUtil.convertLineSeparators(sourceScript, "\r\n") + "\r\n  \r\n   \r\n   ";
      scriptFile = writeToTempFile(buildTempDir, sourceScript);
    }
    return scriptFile;
  }

  @NotNull
  private File writeToTempFile(@NotNull final File buildTempDir, @NotNull final String text) throws RunBuildException {
    Closeable handle = null;
    File file;
    try {
      file = FileUtil.createTempFile(buildTempDir, "powershell", ".ps1", true);
      OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
      handle = w;
      w.write(text);
      return file;
    } catch (IOException e) {
      LOG.error("Error occured while processing file for powershell script", e);
      throw new RunBuildException("Failed to generate temporary resulting powershell script due to exception", e);
    } finally {
      FileUtil.close(handle);
    }
  }


  @SuppressWarnings("ConstantConditions")
  private String processTemplate(@NotNull final String template, @NotNull final Map<String, String> substMap) {
    String result = template;
    for (String str: substMap.keySet()) {
      result = StringUtil.replace(result, str, substMap.get(str));
    }
    return result;
  }

  @NotNull
  public String getTemplate(@NotNull final String minVersion) throws RunBuildException {
    String templateName = minVersion.equals(PowerShellVersion.V_1_0.getVersion()) ? TEMPLATES[0] : TEMPLATES[1];
    try {
      String content = FileUtil.readResourceAsString(getClass(), "/data/" + templateName, Charset.forName("UTF-8"));
      if (content == null) {
        throw new RunBuildException("Script template " + templateName + " was not found!");
      }
      return content;
    } catch (IOException e) {
      throw new RunBuildException("Unable to read script template due to exception", e);
    }
  }
}

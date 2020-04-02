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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.Loggers;
import jetbrains.buildServer.powershell.agent.detect.DetectionContext;
import jetbrains.buildServer.powershell.agent.detect.PowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Detects PowerShell using command line and detection script
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CommandLinePowerShellDetector implements PowerShellDetector {

  @NotNull
  private static final Logger LOG = Loggers.DETECTION_LOGGER;

  @NotNull
  private final BuildAgentConfiguration myConfiguration;

  @NotNull
  private final DetectionRunner myRunner;
  @NotNull
  private final DetectionPaths myDetectionPaths;

  private static final List<String> EXECUTABLES_WIN = Collections.singletonList(
          "pwsh.exe"
  );

  private static final List<String> EXECUTABLES_NIX = Arrays.asList(
          "pwsh",
          "pwsh-preview"
  );

  private static final List<String> EXECUTABLES_NIX_LEGACY = Collections.singletonList(
          "powershell"
  );

  private static final List<String> EXECUTABLES_WIN_DESKTOP = Collections.singletonList(
          "powershell.exe"
  );

  private static final List<String> WIN_ADDITIONAL_PARAMETERS = Arrays.asList(
          "-ExecutionPolicy",
          "ByPass"
  );

  private static final String DETECTION_SCRIPT =
          "$edition = \"Desktop\"\n" +
                  "if (![string]::IsNullOrEmpty($PSVersionTable.PSEdition)) {\n" +
                  "  $edition = $PSVersionTable.PSEdition\n" +
                  "} \n" +
                  "Write-Output " +
                  "$PSVersionTable.PSVersion.toString() " + // shell version
                  "$edition " + // shell edition
                  "([IntPtr]::size -eq 8)";                 // shell bitness

  public CommandLinePowerShellDetector(@NotNull final BuildAgentConfiguration configuration,
                                       @NotNull final DetectionRunner runner,
                                       @NotNull final DetectionPaths detectionPaths) {
    myConfiguration = configuration;
    myRunner = runner;
    myDetectionPaths = detectionPaths;
  }

  @NotNull
  @Override
  public Map<String, PowerShellInfo> findShells(@NotNull DetectionContext detectionContext) {
    LOG.info("Detecting PowerShell using CommandLinePowerShellDetector");
    // group by home
    final Map<String, PowerShellInfo> shells = new HashMap<String, PowerShellInfo>();
    final List<String> pathsToCheck = myDetectionPaths.getPaths(detectionContext);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Will be detecting PowerShell in the following locations: [\n" + StringUtil.join(pathsToCheck, "\n") + "\n");
    }

    File script = null;
    try {
      script = prepareDetectionScript();
      if (script != null) {
        final String scriptPath = script.getAbsolutePath();
        if (LOG.isDebugEnabled()) {
          LOG.info("Detection script path is: " + scriptPath);
        }
        // try release versions. powershell has already been renamed to pwsh.
        // pwsh-preview is used for -preview versions of powershell core
        LOG.debug("Detecting PowerShell.Core...");
        doDetectionCycle(shells, pathsToCheck, SystemInfo.isWindows ? EXECUTABLES_WIN : EXECUTABLES_NIX, scriptPath);
        // handle the case when JetRegistry.exe could not be executed. Try to search for desktop edition inside given paths
        if (SystemInfo.isWindows) {
          doDetectionCycle(shells, pathsToCheck, EXECUTABLES_WIN_DESKTOP, scriptPath, WIN_ADDITIONAL_PARAMETERS);
        }
        if (shells.isEmpty() && !SystemInfo.isWindows) {
          LOG.debug("No release versions of PowerShell.Core were detected. Trying to detect legacy and beta versions...");
          doDetectionCycle(shells, pathsToCheck, EXECUTABLES_NIX_LEGACY, scriptPath);
        }
      }
      return shells;
    } finally {
      if (script != null) {
        FileUtil.delete(script);
      }
    }
  }

  private void doDetectionCycle(Map<String, PowerShellInfo> shells,
                                List<String> pathsToCheck,
                                List<String> executablesToCheck,
                                String scriptPath) {
    doDetectionCycle(shells, pathsToCheck, executablesToCheck, scriptPath, Collections.<String>emptyList());
  }

  private void doDetectionCycle(Map<String, PowerShellInfo> shells,
                                List<String> pathsToCheck,
                                List<String> executablesToCheck,
                                String scriptPath,
                                List<String> additionalParameters) {
    for (String path: pathsToCheck) {
      for (String executable: executablesToCheck) {
        final PowerShellInfo detected = doDetect(path, executable, scriptPath, additionalParameters);
        if (detected != null) {
          shells.put(detected.getHome().getAbsolutePath(), detected);
        }
      }
    }
  }

  @Nullable
  private PowerShellInfo doDetect(@NotNull final String homePath,
                                  @NotNull final String executable,
                                  @NotNull final String scriptPath,
                                  @NotNull final List<String> additionalParameters) {
    PowerShellInfo result = null;
    final File exeFile = new File(homePath, executable);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Searching for PowerShell at " + exeFile.getAbsolutePath());
    }
    if (exeFile.isFile()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Trying PowerShell executable: " + exeFile.getAbsolutePath());
      }
      String executablePath = exeFile.getAbsolutePath();
      try {
        final List<String> outputLines = myRunner.runDetectionScript(executablePath, scriptPath, additionalParameters);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Detection script output at " + executablePath + "\n" + StringUtil.join(outputLines, "\n"));
        }
        if (outputLines.size() == 3) {
          final PowerShellEdition edition = PowerShellEdition.fromString(outputLines.get(1));
          if (edition != null) {
            result = new PowerShellInfo(Boolean.parseBoolean(outputLines.get(2)) ? PowerShellBitness.x64 : PowerShellBitness.x86, exeFile.getParentFile(), outputLines.get(0), edition, executable);
            LOG.info("Found: " + result);
          } else {
            LOG.warn("Failed to determine PowerShell edition for [" + executablePath + "]");
            LOG.debug(StringUtil.join("\n", outputLines));
          }
        } else {
          LOG.warn("Failed to parse output from PowerShell executable [" + executablePath + "]");
          LOG.debug(StringUtil.join("\n", outputLines));
        }
      } catch (ExecutionException e) {
        LOG.warnAndDebugDetails("Failed to run PowerShell detection script [" + scriptPath + "] with executable [" + executablePath + "]", e);
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("PowerShell at " + exeFile.getAbsolutePath() + " was not found");
      }
    }
    return result;
  }

  private File prepareDetectionScript() {
    final File cacheDir = myConfiguration.getCacheDirectory(PowerShellConstants.PLUGIN_NAME);
    final File result = new File(cacheDir, "detect_" + System.currentTimeMillis() + ".ps1");
    try {
      FileUtil.writeFile(result, DETECTION_SCRIPT, "UTF-8");
    } catch (IOException e) {
      LOG.warnAndDebugDetails("Failed to write PowerShell detection script to file [" + result.getAbsolutePath() + "]", e);
      return null;
    }
    return result;
  }
}

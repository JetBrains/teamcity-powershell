/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.detect.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
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
 * Detects PowerShell on non-windows systems
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CommandLinePowerShellDetector implements PowerShellDetector {

  @NotNull
  private static final Logger LOG = Logger.getInstance(CommandLinePowerShellDetector.class.getName());

  @NotNull
  private final BuildAgentConfiguration myConfiguration;

  @NotNull
  private final DetectionRunner myRunner;

  private static final List<String> PATHS = Arrays.asList(
          "/usr/local/bin", // mac os
          "/usr/bin"        // linux
  );

  private static final List<String> EXECUTABLES = Arrays.asList(
          "pwsh",
          "powershell"
  );

  private static final String DETECTION_SCRIPT =
          "Write-Output " +
                  "$PSVersionTable.PSVersion.toString() " + // shell version
                  "$PSVersionTable.PSEdition.toString() " + // shell edition
                  "([IntPtr]::size -eq 8)";                 // shell bitness

  public CommandLinePowerShellDetector(@NotNull final BuildAgentConfiguration configuration,
                                       @NotNull final DetectionRunner runner) {
    myConfiguration = configuration;
    myRunner = runner;
  }

  @NotNull
  @Override
  public Map<PowerShellBitness, PowerShellInfo> findPowerShells(@NotNull final DetectionContext detectionContext) {
    if (SystemInfo.isWindows) {
      return Collections.emptyMap();
    }
    final Map<PowerShellBitness, PowerShellInfo> result = new HashMap<PowerShellBitness, PowerShellInfo>();

    File script = null;
    try {
      script = prepareDetectionScript();
      if (script != null) {
        final String scriptPath = script.getAbsolutePath();
        // check predefined paths
        if (!detectionContext.getPredefinedPaths().isEmpty()) {
          // what if 32 bit installation is in place of 64 bit? or otherwise?
          // need to override in constructor of PowerShellInfo
          for (Map.Entry<PowerShellBitness, String> e: detectionContext.getPredefinedPaths().entrySet()) {
            for (String executable : EXECUTABLES) {
              PowerShellInfo info = doDetect(e.getValue(), executable, scriptPath);
              if (info != null) {
                if (info.getBitness() != e.getKey()) { // if we are to substitute PowerShell installation explicitly, ignoring the bits of detected one
                  LOG.warn("Using configured bitness (" + e.getKey() + ") for PowerShell at [" + info.getHome() + "] instead of detected one (" + info.getBitness() + ")");
                }
                register(result, new PowerShellInfo(e.getKey(), info.getHome(), info.getVersion(), info.getEdition(), info.getExecutable()));
              }
            }
          }
        }

        final List<String> pathsToCheck = !detectionContext.getSearchPaths().isEmpty() ? detectionContext.getSearchPaths() : PATHS;
        for (String path: pathsToCheck) {
          for (String executable: EXECUTABLES) {
            final PowerShellInfo detected = doDetect(path, executable, scriptPath);
            if (detected != null) {
              if (result.get(detected.getBitness()) != null) {
                LOG.warn("Ignoring " + detected.getBitness() + " PowerShell at " + detected.getHome()
                        + " as it is explicitly replaced by " + result.get(detected.getBitness()).getHome()
                        + " in build agent properties");
              } else {
                register(result, detected);
              }
            }
          }
        }
      }
      return result;
    } finally {
      if (script != null) {
        FileUtil.delete(script);
      }
    }
  }
  private void register(@NotNull final Map<PowerShellBitness, PowerShellInfo> detected, @NotNull final PowerShellInfo info) {
    LOG.info("Registering " + info.getBitness() + " PowerShell at:" + info.getExecutablePath());
    detected.put(info.getBitness(), info);
  }

  @Nullable
  private PowerShellInfo doDetect(@NotNull final String homePath,
                                  @NotNull final String executable,
                                  @NotNull final String scriptPath) {
    PowerShellInfo result = null;
    final File exeFile = new File(homePath, executable);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Searching for PowerShell at " + exeFile.getAbsolutePath());
    }
    if (exeFile.isFile()) {
      String executablePath = exeFile.getAbsolutePath();
      try {
        final List<String> outputLines = myRunner.runDetectionScript(executablePath, scriptPath);
        if (outputLines.size() == 3) {
          final PowerShellEdition edition = PowerShellEdition.fromString(outputLines.get(1));
          if (edition != null) {
            result = new PowerShellInfo(Boolean.valueOf(outputLines.get(2)) ? PowerShellBitness.x64 : PowerShellBitness.x86, exeFile.getParentFile(), outputLines.get(0), edition, executable);
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

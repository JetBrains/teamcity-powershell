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
import java.io.FilenameFilter;
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

  /**
   * https://docs.microsoft.com/en-us/powershell/scripting/setup/installing-powershell-core-on-windows?view=powershell-5.1
   *
   * By default the package is installed to $env:ProgramFiles\PowerShell\
   */
  private static final List<String> WINDOWS_PATHS = Collections.singletonList(System.getenv("ProgramFiles") + "\\PowerShell");

  private static final List<String> PATHS = Arrays.asList(
      "/usr/local/bin", // mac os
      "/usr/bin"        // linux
  );

  private static final List<String> EXECUTABLES_WIN = Arrays.asList(
      "pwsh.exe",
      "poweshell.exe"
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
  public Map<String, PowerShellInfo> findShells(@NotNull DetectionContext detectionContext) {
    LOG.info("Detecting PowerShell using CommandLinePowerShellDetector");
    // group by home
    Map<String, PowerShellInfo> shells = new HashMap<String, PowerShellInfo>();
    // determine paths
    if (LOG.isDebugEnabled()) {
      if (!detectionContext.getSearchPaths().isEmpty()) {
        LOG.debug("Detection paths were overridden by [teamcity.powershell.detector.search.paths] property.");
      }
    }
    final List<String> pathsToCheck = !detectionContext.getSearchPaths().isEmpty() ?
        detectionContext.getSearchPaths() :
        (SystemInfo.isWindows ? getWindowsPaths() : PATHS);
    final List<String> executablesToCheck = SystemInfo.isWindows ? EXECUTABLES_WIN : EXECUTABLES;
    if (LOG.isDebugEnabled()) {
      LOG.debug("Will be detecting powershell at: " + Arrays.toString(pathsToCheck.toArray()));
    }
    File script = null;
    try {
      script = prepareDetectionScript();
      if (script != null) {
        final String scriptPath = script.getAbsolutePath();
        if (LOG.isDebugEnabled()) {
          LOG.info("Detection script path is: " + scriptPath);
        }
        for (String path: pathsToCheck) {
          for (String executable: executablesToCheck) {
            final PowerShellInfo detected = doDetect(path, executable, scriptPath);
            if (detected != null) {
              shells.put(detected.getHome().getAbsolutePath(), detected);
            }
          }
        }
      }
      return shells;
    } finally {
      if (script != null) {
        FileUtil.delete(script);
      }
    }
  }

  private List<String> getWindowsPaths() {
    List<String> result = new ArrayList<String>();
    for (String base: WINDOWS_PATHS) {
      File f = new File(base);
      if (f.isDirectory()) {
        result.addAll(populateWithChildren(f));
      }
    }
    return result;
  }

  private List<String> populateWithChildren(@NotNull File base) {
    List<String> result = new ArrayList<String>();
    for (File f: FileUtil.listFiles(base, new FilenameFilter() {
      @Override
      public boolean accept(File file, String s) {
        return file.isDirectory();
      }
    })) {
      result.add(f.getAbsolutePath());
    }
    return result;
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

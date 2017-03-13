package jetbrains.buildServer.powershell.agent.detect.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.detect.PowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detects PowerShell on non-windows systems
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CommandLinePowerShellDetector implements PowerShellDetector {

  private static final Logger LOG = Logger.getInstance(CommandLinePowerShellDetector.class.getName());

  @NotNull
  private final BuildAgentConfiguration myConfiguration;

  @NotNull
  private final DetectionRunner myRunner;

  private String[] paths = {
      "/usr/local/bin", // mac os
      "/usr/bin"        // linux
  };

  private static final String EXECUTABLE = "powershell";

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
  public List<PowerShellInfo> findPowerShells() {
    if (SystemInfo.isWindows) {
      return Collections.emptyList();
    }
    if (!TeamCityProperties.getBoolean("teamcity.powershell.crossplatform.enabled")) {
      LOG.info("Cross-platform PowerShell is disabled");
      return Collections.emptyList();
    }
    final List<PowerShellInfo> result = new ArrayList<PowerShellInfo>();
    File script = null;
    try {
      script = prepareDetectionScript();
      if (script != null) {
        final String scriptPath = script.getAbsolutePath();
        for (String path: paths) {
          File executableLocation = new File(path, EXECUTABLE);
          if (executableLocation.isFile()) {
            doDetect(executableLocation, scriptPath, result);
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

  private void doDetect(@NotNull final File executable,
                        @NotNull final String scriptPath,
                        @NotNull final List<PowerShellInfo> detected) {
    final String executablePath = executable.getAbsolutePath();
    try {
      final List<String> outputLines = myRunner.runDetectionScript(executablePath, scriptPath);
      if (outputLines.size() == 3) {
        LOG.info("Registering PowerShell at:" + executablePath);
        detected.add(new PowerShellInfo(Boolean.valueOf(outputLines.get(2)) ? PowerShellBitness.x64 : PowerShellBitness.x86, executable.getParentFile(), outputLines.get(0)));
      } else {
        LOG.warn("Failed to parse output from PowerShell executable [" + executablePath + "]");
        LOG.debug(StringUtil.join("\n", outputLines));
      }
    } catch (ExecutionException e) {
      LOG.warnAndDebugDetails("Failed to run PowerShell detection script [" + scriptPath + "] with executable [" + executablePath + "]", e);
    }
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

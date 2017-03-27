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
import jetbrains.buildServer.serverSide.TeamCityProperties;
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

  private List<String> paths = Arrays.asList(
      "/usr/local/bin", // mac os
      "/usr/bin"        // linux
  );

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
  public Map<PowerShellBitness, PowerShellInfo> findPowerShells(@NotNull final DetectionContext detectionContext) {
    if (SystemInfo.isWindows) {
      return Collections.emptyMap();
    }
    if (!TeamCityProperties.getBoolean("teamcity.powershell.crossplatform.enabled")) {
      LOG.info("Cross-platform PowerShell is disabled");
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
            File executableLocation = new File(e.getValue(), EXECUTABLE);
            PowerShellInfo info = doDetect(executableLocation, scriptPath);
            if (info != null) {
              if (info.getBitness() != e.getKey()) { // if we are to substitute PowerShell installation explicitly, ignoring the bits of detected one
                LOG.warn("Using configured bitness (" + e.getKey() + ") for PowerShell at [" + info.getHome() + "] instead of detected one (" + info.getBitness() + ")");
              }
              register(result, new PowerShellInfo(e.getKey(), info.getHome(), info.getVersion(), info.getEdition()));
            }
          }
        }

        final List<String> pathsToCheck = !detectionContext.getSearchPaths().isEmpty() ? detectionContext.getSearchPaths() : paths;
        for (String path: pathsToCheck) {
          File executableLocation = new File(path, EXECUTABLE);
          if (executableLocation.isFile()) {
            final PowerShellInfo detected = doDetect(executableLocation, scriptPath);
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
  private PowerShellInfo doDetect(@NotNull final File executable,
                                  @NotNull final String scriptPath) {
    PowerShellInfo result = null;
    final String executablePath = executable.getAbsolutePath();
    try {
      final List<String> outputLines = myRunner.runDetectionScript(executablePath, scriptPath);
      if (outputLines.size() == 3) {
        final PowerShellEdition edition = PowerShellEdition.fromString(outputLines.get(1));
        if (edition != null) {
          result = new PowerShellInfo(Boolean.valueOf(outputLines.get(2)) ? PowerShellBitness.x64 : PowerShellBitness.x86, executable.getParentFile(), outputLines.get(0), edition);
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

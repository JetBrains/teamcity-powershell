

package jetbrains.buildServer.powershell.agent.detect.registry;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.powershell.agent.Loggers;
import jetbrains.buildServer.powershell.agent.detect.DetectionContext;
import jetbrains.buildServer.powershell.agent.detect.PowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.Win32RegistryAccessor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 14:43
 */
public class RegistryPowerShellDetector implements PowerShellDetector {

  @NotNull
  private static final Logger LOG = Loggers.DETECTION_LOGGER;

  @NotNull
  private final Win32RegistryAccessor myAccessor;

  public RegistryPowerShellDetector(@NotNull final Win32RegistryAccessor accessor) {
    myAccessor = accessor;
  }

  @NotNull
  @Override
  public Map<String, PowerShellInfo> findShells(@NotNull DetectionContext detectionContext) {
    Map<String, PowerShellInfo> result = new HashMap<>();

    LOG.info("Detecting PowerShell using RegistryPowerShellDetector");
    if (!SystemInfo.isWindows) {
      LOG.info("RegistryPowerShellDetector is only available on Windows");
      return Collections.emptyMap();
    }
    for (PowerShellBitness bitness: PowerShellBitness.values()) {
      final PowerShellRegistry reg = new PowerShellRegistry(bitness.toBitness(), myAccessor);

      if (!reg.isPowerShellInstalled()) {
        LOG.debug("PowerShell for " + bitness + " was not found.");
        continue;
      }

      final String ver = reg.getInstalledVersion();
      final File home = reg.getPowerShellHome();

      if (ver == null || home == null) {
        LOG.debug("Found PowerShell: " + bitness + " " + ver + " " + home);
        continue;
      }
      final PowerShellInfo info = new PowerShellInfo(bitness, home, ver, PowerShellEdition.DESKTOP, "powershell.exe");
      LOG.info("Found through registry: " + info);
      result.put(info.getHome().getAbsolutePath(), info);
    }
    return result;
  }
}
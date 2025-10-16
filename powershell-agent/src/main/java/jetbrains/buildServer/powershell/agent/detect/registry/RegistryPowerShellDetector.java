package jetbrains.buildServer.powershell.agent.detect.registry;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.powershell.agent.Loggers;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.Win32RegistryAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static jetbrains.buildServer.util.Bitness.BIT32;
import static jetbrains.buildServer.util.Win32RegistryAccessor.Hive.LOCAL_MACHINE;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 14:43
 */
public class RegistryPowerShellDetector {

  @NotNull
  private static final Logger LOG = Loggers.DETECTION_LOGGER;

  @NotNull
  private final Win32RegistryAccessor myAccessor;

  public RegistryPowerShellDetector(@NotNull final Win32RegistryAccessor accessor) {
    myAccessor = accessor;
  }

  @NotNull
  public Map<String, PowerShellInfo> findShells() {
    LOG.info("Detecting PowerShell using RegistryPowerShellDetector");
    Map<String, PowerShellInfo> result = new HashMap<>();
    result.putAll(findDesktopEditions());
    result.putAll(findCoreEditions());
    return result;
  }

  @NotNull
  private Map<String, PowerShellInfo> findDesktopEditions() {
    Map<String, PowerShellInfo> result = new HashMap<>();
    boolean isV1Installed = isDesktopEditionInstalled("1");
    boolean isV3Installed = isDesktopEditionInstalled("3");
    if (!isV1Installed && !isV3Installed) {
      LOG.debug("PowerShell desktop edition for was not found");
      return result;
    }
    for (PowerShellBitness bitness: PowerShellBitness.values()) {
      PowerShellInfo info = null;
      if (isV3Installed) {
        info = fetchInfoForDesktopEdition(bitness, "3");
      }
      if (info == null) {
        info = fetchInfoForDesktopEdition(bitness, "1");
      }
      if (info != null) {
        logFound(info);
        result.put(info.getHome().getAbsolutePath(), info);
      }
    }
    return result;
  }

  @NotNull
  private Map<String, PowerShellInfo> findCoreEditions() {
    Map<String, PowerShellInfo> result = new HashMap<>();
    String rootPath = "SOFTWARE\\Microsoft\\PowerShellCore\\InstalledVersions";
    for (PowerShellBitness bitness : PowerShellBitness.values()) {

      Set<String> keys = myAccessor.listSubKeys(LOCAL_MACHINE, bitness.toBitness(), rootPath);
      for (String key : keys) {
        String keyPath = rootPath + "\\" + key;
        String homeStr = myAccessor.readRegistryText(LOCAL_MACHINE, bitness.toBitness(), keyPath, "InstallLocation");
        if (homeStr == null) {
          LOG.warn("Could not fetch InstallLocation from " + keyPath);
          continue;
        }
        File home = asDirectoryOrNull(homeStr);
        if (home == null) {
          LOG.warn("Found InstallLocation is not a valid directory: " + homeStr);
          continue;
        }
        String version = myAccessor.readRegistryText(LOCAL_MACHINE, bitness.toBitness(), keyPath, "SemanticVersion");
        if (version == null) {
          LOG.warn("Could not fetch SemanticVersion from " + keyPath);
          continue;
        }
        if (new File(home, "pwsh.exe").exists()) {
          PowerShellInfo info = new PowerShellInfo(bitness, home, version, PowerShellEdition.CORE, "pwsh.exe");
          logFound(info);
          result.put(info.getHome().getAbsolutePath(), info);
        }
      }
    }
    return result;
  }

  private boolean isDesktopEditionInstalled(String oneOrThree) {
    String path = "SOFTWARE\\Microsoft\\PowerShell\\" + oneOrThree;
    // TODO check, maybe it's a bug that BIT32 is always passed, this logic was initially introduced in 2010 or before
    return "1".equals(myAccessor.readRegistryText(LOCAL_MACHINE, BIT32, path, "Install"));
  }

  @Nullable
  private PowerShellInfo fetchInfoForDesktopEdition(@NotNull PowerShellBitness bitness, @NotNull String oneOrThree) {
    String path = "SOFTWARE\\Microsoft\\PowerShell\\" + oneOrThree + "\\PowerShellEngine";
    String version = myAccessor.readRegistryText(LOCAL_MACHINE, bitness.toBitness(), path, "PowerShellVersion");
    File home = asDirectoryOrNull(myAccessor.readRegistryText(LOCAL_MACHINE, bitness.toBitness(), path, "ApplicationBase"));
    if (version == null || home == null) {
      LOG.debug("Skip PowerShell: " + bitness + " " + version + " " + home);
      return null;
    }
    return new PowerShellInfo(bitness, home, version, PowerShellEdition.DESKTOP, "powershell.exe");
  }

  @Nullable
  private static File asDirectoryOrNull(@Nullable String path) {
    if (path == null) return null;
    final File directory = FileUtil.getCanonicalFile(new File(path));
    return directory.isDirectory() ? directory : null;
  }

  private static void logFound(PowerShellInfo info) {
    LOG.info("Found through registry: " + info);
  }
}
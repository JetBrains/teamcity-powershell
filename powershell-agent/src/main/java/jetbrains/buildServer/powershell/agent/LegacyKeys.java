package jetbrains.buildServer.powershell.agent;

import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import org.jetbrains.annotations.NotNull;

/**
 * Class {@code LegacyKeys}
 *
 * Fills agent configuration with legacy PowerShell keys used before PowerShell.Core integration
 *
 * For each {@code Bitness} fills {@code Version}, {@code Path}, {@code Edition} and {@code Executable}
 * properties.
 *
 * These properties are no longer used by the plugin.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
class LegacyKeys {

  private static String getVersionKey(@NotNull final PowerShellBitness bitness) {
    return "powershell_" + bitness.getValue();
  }

  private static String getPathKey(@NotNull final PowerShellBitness bitness) {
    return getVersionKey(bitness) + "_Path";
  }

  private static String getEditionKey(@NotNull final PowerShellBitness bitness) {
    return getVersionKey(bitness) + "_Edition";
  }

  private static String getExecutableKey(@NotNull final PowerShellBitness bitness) {
    return getVersionKey(bitness) + "_Executable";
  }

  static void fillLegacyKeys(@NotNull final BuildAgentConfiguration config,
                             @NotNull final PowerShellBitness bit,
                             @NotNull final PowerShellInfo info) {
    config.addConfigurationParameter(getVersionKey(bit), info.getVersion());
    config.addConfigurationParameter(getPathKey(bit), info.getHome().toString());
    if (info.getEdition() != null) {
      config.addConfigurationParameter(getEditionKey(bit), info.getEdition().getValue());
    }
    config.addConfigurationParameter(getExecutableKey(bit), info.getExecutable());
  }
}

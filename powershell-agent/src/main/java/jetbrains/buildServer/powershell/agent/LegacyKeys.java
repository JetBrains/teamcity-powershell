

package jetbrains.buildServer.powershell.agent;

import java.util.Map;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
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

  public static String getExecutableKey(@NotNull final PowerShellBitness bitness) {
    return getVersionKey(bitness) + PowerShellConstants.EXECUTABLE_SUFFIX;
  }

  static void fillLegacyKeys(@NotNull final Map<String, String> parameters,
                             @NotNull final PowerShellBitness bit,
                             @NotNull final PowerShellInfo info) {
    parameters.put(getVersionKey(bit), info.getVersion());
    parameters.put(getPathKey(bit), info.getHome().toString());
    if (info.getEdition() != null) {
      parameters.put(getEditionKey(bit), info.getEdition().getValue());
    }
    parameters.put(getExecutableKey(bit), info.getExecutable());
  }
}
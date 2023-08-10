/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

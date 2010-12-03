/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package jetbrains.buildServer.powershell.agent.detect;

import jetbrains.buildServer.powershell.common.PowerShellVersion;
import jetbrains.buildServer.util.Bitness;
import jetbrains.buildServer.util.Win32RegistryAccessor;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 14:43
 */
public class PowerShellDetector {
  private static final Logger LOG = Logger.getLogger(PowerShellDetector.class);

  private final Win32RegistryAccessor myAccesor;

  public PowerShellDetector(@NotNull final Win32RegistryAccessor accesor) {
    myAccesor = accesor;
  }

  public Collection<PowerShellInfo> findPowerShells() {
    Collection<PowerShellInfo> col = new ArrayList<PowerShellInfo>(2);
    for (Bitness bitness : Bitness.values()) {
      final PowerShellRegistry reg = new PowerShellRegistry(bitness, myAccesor);

      if (!reg.isPowerShellInstalled()) {
        LOG.debug("Powershell for " + bitness + " was not found.");
        continue;
      }

      final PowerShellVersion ver = reg.getInstalledVersion();
      final File home = reg.getPowerShellHome();



      if (ver == null || home == null) {
        LOG.debug("Found powershell: " + bitness + " " + ver + " " + home);
        continue;
      }

      final PowerShellInfo info = new PowerShellInfo(bitness, home, ver);
      LOG.info("Found: " + info);
      col.add(info);
    }
    return col;
  }
}

/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.Win32RegistryAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static jetbrains.buildServer.util.Bitness.BIT32;
import static jetbrains.buildServer.util.Win32RegistryAccessor.Hive.LOCAL_MACHINE;

/**
 * Powershell detection logic is described at
 * http://blogs.msdn.com/b/powershell/archive/2009/06/25/detection-logic-poweshell-installation.aspx
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 13:30
 */
public class PowerShellRegistry {
  private final Bitness myBitness;
  private final Win32RegistryAccessor myRegistryAccessor;

  private final VersionedPowerShell[] myDetectors = {new VersionedPowerShell("3"), new VersionedPowerShell("1")};

  public PowerShellRegistry(@NotNull final Bitness bitness, @NotNull Win32RegistryAccessor registryAccessor) {
    myRegistryAccessor = registryAccessor;
    myBitness = bitness;
  }

  public boolean isPowerShellInstalled() {
    for (VersionedPowerShell shell : myDetectors) {
      if (shell.isPowerShellInstalled()) return true;
    }
    return false;
  }

  @Nullable
  public PowerShellVersion getInstalledVersion() {
    for (VersionedPowerShell shell : myDetectors) {
      PowerShellVersion version = shell.getInstalledVersion();
      if (version != null) return version;
    }
    return null;
  }

  @Nullable
  public File getPowerShellHome() {
    for (VersionedPowerShell shell : myDetectors) {
      File home = shell.getPowerShellHome();
      if (home != null) return home;
    }
    return null;
  }

  private class VersionedPowerShell {
    private final String myVersion;

    private VersionedPowerShell(@NotNull final String myVersion) {
      this.myVersion = myVersion;
    }

    public boolean isPowerShellInstalled() {
      final String val = myRegistryAccessor.readRegistryText(
              LOCAL_MACHINE,
              BIT32,
              "SOFTWARE\\Microsoft\\PowerShell\\" + myVersion,
              "Install");

      return "1".equals(val);
    }

    @Nullable
    public PowerShellVersion getInstalledVersion() {
      final String ver = myRegistryAccessor.readRegistryText(
              LOCAL_MACHINE,
              myBitness,
              "SOFTWARE\\Microsoft\\PowerShell\\" + myVersion + "\\PowerShellEngine",
              "PowerShellVersion");

      return PowerShellVersion.fromString(ver);
    }

    @Nullable
    public File getPowerShellHome() {
      final String home = myRegistryAccessor.readRegistryText(
              LOCAL_MACHINE,
              myBitness,
              "SOFTWARE\\Microsoft\\PowerShell\\" + myVersion + "\\PowerShellEngine",
              "ApplicationBase");

      if (home == null) return null;
      final File path = FileUtil.getCanonicalFile(new File(home));
      return path.isDirectory() ? path : null;
    }
  }
}

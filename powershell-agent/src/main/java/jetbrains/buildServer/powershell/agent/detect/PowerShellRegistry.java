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

  public PowerShellRegistry(@NotNull final Bitness bitness, @NotNull Win32RegistryAccessor registryAccessor) {
    myRegistryAccessor = registryAccessor;
    myBitness = bitness;
  }

  public boolean isPowerShellInstalled() {
    final String val = myRegistryAccessor.readRegistryText(
            LOCAL_MACHINE,
            BIT32,
            "SOFTWARE\\Microsoft\\PowerShell\\1",
            "Install");

    return "1".equals(val);
  }

  @Nullable
  public PowerShellVersion getInstalledVersion() {
    final String ver = myRegistryAccessor.readRegistryText(
            LOCAL_MACHINE,
            myBitness,
            "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine",
            "PowerShellVersion");

    return PowerShellVersion.fromString(ver);
  }

  @Nullable
  public File getPowerShellHome() {
    final String home = myRegistryAccessor.readRegistryText(
            LOCAL_MACHINE,
            myBitness,
            "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine",
            "ApplicationBase");

    if (home == null) return null;
    final File path = FileUtil.getCanonicalFile(new File(home));
    return path.isDirectory() ? path : null;
  }

}

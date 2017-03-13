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

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 14:06
 */
public class PowerShellInfo {

  @NotNull
  private final PowerShellBitness myBitness;

  @NotNull
  private final File myHome;

  @NotNull
  private final String myVersion;

  public PowerShellInfo(@NotNull final PowerShellBitness bitness,
                        @NotNull final File home,
                        @NotNull final String version) {
    myBitness = bitness;
    myHome = home;
    myVersion = version;
  }

  @NotNull
  public PowerShellBitness getBitness() {
    return myBitness;
  }

  @NotNull
  File getHome() {
    return myHome;
  }

  @NotNull
  public String getVersion() {
    return myVersion;
  }

  @Override
  public String toString() {
    return "PowerShell v" + myVersion + " " + myBitness + "(" + getHome() + ")";
  }

  @Nullable
  public static PowerShellInfo loadInfo(@NotNull final BuildAgentConfiguration config,
                                        @Nullable final PowerShellBitness bitness) {
    if (bitness == null) return null;

    final Map<String, String> ps = config.getConfigurationParameters();
    final String ver = ps.get(bitness.getVersionKey());
    final String path = ps.get(bitness.getPathKey());

    if (path != null && ver != null) {
      return new PowerShellInfo(bitness, new File(path), ver);
    }
    return null;
  }

  public void saveInfo(@NotNull final BuildAgentConfiguration config) {
    config.addConfigurationParameter(myBitness.getVersionKey(), myVersion);
    config.addConfigurationParameter(myBitness.getPathKey(), myHome.toString());
  }

  @NotNull
  public String getExecutablePath() {
    return FileUtil.getCanonicalFile(new File(myHome, "powershell" + (SystemInfo.isWindows ? ".exe" : ""))).getPath();
  }
}

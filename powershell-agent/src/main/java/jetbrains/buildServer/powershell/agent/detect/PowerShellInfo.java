/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.powershell.agent.Loggers.DETECTION_LOGGER;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.PATH_SUFFIX;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.generateFullKey;

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

  @Nullable
  private final PowerShellEdition myEdition;

  @NotNull
  private final String myExecutable;

  private final boolean myVirtual;
  
  public PowerShellInfo(@NotNull final PowerShellBitness bitness,
                        @NotNull final File home,
                        @NotNull final String version,
                        @Nullable final PowerShellEdition edition,
                        @NotNull final String executable) {
    this(bitness, home, version, edition, executable, false);
  }

  public PowerShellInfo(@NotNull final PowerShellBitness bitness,
                        @NotNull final File home,
                        @NotNull final String version,
                        @Nullable final PowerShellEdition edition,
                        @NotNull final String executable,
                        boolean isVirtual) {
    myBitness = bitness;
    myHome = home;
    myVersion = version;
    myEdition = edition;
    myExecutable = executable;
    myVirtual = isVirtual;
  }

  @NotNull
  public PowerShellBitness getBitness() {
    return myBitness;
  }

  @NotNull
  public File getHome() {
    return myHome;
  }

  @NotNull
  public String getVersion() {
    return myVersion;
  }

  @Override
  public String toString() {
    return (myVirtual ? "(virtual) " : "") + "PowerShell "
        + myEdition + " Edition v"
        + myVersion + " "
        + myBitness
        + "(" + (myVirtual ? "" : ( getHome() + File.separator)) + myExecutable + ")";
  }

  @NotNull
  public String getExecutable() {
    return myExecutable;
  }

  @Nullable
  public PowerShellEdition getEdition() {
    return myEdition;
  }

  public void saveInfo(@NotNull final Map<String, String> agentParameters) {
    if (!myVirtual) {
      Map<String, String> parameters = toConfigurationParameters();
      DETECTION_LOGGER.debug("Saving configuration parameters:");
      for (Map.Entry<String, String> entry: parameters.entrySet()) {
        DETECTION_LOGGER.debug(entry.getKey() + " -> " + entry.getValue());
        agentParameters.put(entry.getKey(), entry.getValue());
      }
    }
  }

  @NotNull
  public String getExecutablePath() {
    if (myVirtual) {
      return myExecutable;
    } else {
      return FileUtil.getCanonicalFile(new File(myHome, myExecutable)).getPath();
    }
  }

  public boolean isVirtual() {
    return myVirtual;
  }

  private Map<String, String> toConfigurationParameters() {
    final String key = generateFullKey(myEdition, myBitness, myVersion);
    final Map<String, String> result = new HashMap<>();
    result.put(key, myVersion);
    result.put(key + PATH_SUFFIX, myHome.toString());
    return result;
  }
}

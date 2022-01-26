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

package jetbrains.buildServer.powershell.agent.system;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class PowerShellCommands {

  @NotNull
  private final SystemBitness mySystemBitness;

  @NotNull
  private static final Logger LOG = Logger.getInstance(PowerShellCommands.class.getName());

  @NotNull
  private static final String NATIVE   = "sysnative";

  @NotNull
  private static final String SYSTEM32 = "System32";

  public PowerShellCommands(@NotNull final SystemBitness systemBitness) {
    mySystemBitness = systemBitness;
  }

  /**
   * Patches path to PowerShell executable path to force it to go into 64bit mode if
   * Windows is 64bit, Java process is 32 bit, PowerShell x64 is required
   *
   * @link http://www.samlogic.net/articles/sysnative-folder-64-bit-windows.htm
   *
   * @param info powershell info
   * @return patched path to powershell executable path
   */
  public String getNativeCommand(@NotNull final PowerShellInfo info, @NotNull final BuildRunnerContext context) {
    if (!context.isVirtualContext()) {
      if (info.getBitness() == PowerShellBitness.x64) {
        if (mySystemBitness.is32bit()) {
          return info.getExecutablePath().replace(SYSTEM32, NATIVE);
        }
      }
    }
    return info.getExecutablePath();
  }

  /**
   * Gets path to {@code cmd.exe} wrapper for powershell process.
   * Wrapper implicitly causes child process to inherit its bitness.
   *
   * @param info powershell info
   * @param env environment variables
   * @return path to {@code cmd.exe}
   */
  public String getCMDWrappedCommand(@NotNull final PowerShellInfo info, @NotNull final Map<String, String> env) {
    final String windir = env.get("windir");
    if (StringUtil.isEmptyOrSpaces(windir)) {
      LOG.warn("Failed to find %windir%");
      return "cmd.exe";
    }
    switch (info.getBitness()) {
      case x64:
        if (mySystemBitness.is32bit()) {
          return windir + "\\sysnative\\cmd.exe";
        }
        return windir + "\\System32\\cmd.exe";
      case x86:
        if (mySystemBitness.is64bit()) {
          return windir + "\\SysWOW64\\cmd.exe";
        }
        return windir + "\\System32\\cmd.exe";
    }
    return "cmd.exe";
  }
}

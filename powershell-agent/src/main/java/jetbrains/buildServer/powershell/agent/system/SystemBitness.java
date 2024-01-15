

package jetbrains.buildServer.powershell.agent.system;

import com.intellij.openapi.util.SystemInfo;

/**
 * Wrapper for SystemInfo constants access.
 * Provided for ease of testing
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@SuppressWarnings("SameReturnValue")
public class SystemBitness {
  
  public boolean is32bit() {
    return SystemInfo.is32Bit;
  }

  public boolean is64bit() {
    return SystemInfo.is64Bit;
  }
}
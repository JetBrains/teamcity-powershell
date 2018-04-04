package jetbrains.buildServer.powershell.agent.virtual;

import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class VirtualPowerShellSupport {

  public static PowerShellInfo getVirtualPowerShell() {
    return new PowerShellInfo(
        PowerShellBitness.x64,
        new File("."),
        "-1",
        PowerShellEdition.CORE,
       "pwsh",
        true
    );
  }
}

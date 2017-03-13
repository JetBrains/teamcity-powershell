package jetbrains.buildServer.powershell.agent.detect;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface PowerShellDetector {

  @NotNull
  Collection<PowerShellInfo> findPowerShells();
  
}

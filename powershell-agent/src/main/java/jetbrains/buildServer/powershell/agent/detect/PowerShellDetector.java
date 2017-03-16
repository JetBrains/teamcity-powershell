package jetbrains.buildServer.powershell.agent.detect;

import jetbrains.buildServer.powershell.common.PowerShellBitness;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface PowerShellDetector {

  @NotNull
  Map<PowerShellBitness, PowerShellInfo> findPowerShells(@NotNull final DetectionContext detectionContext);
  
}



package jetbrains.buildServer.powershell.agent.detect;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface PowerShellDetector {

  /**
   * Detects PowerShells installed on the agent
   *
   * @param detectionContext context of detection (agent parameters, etc.)
   * @return map of detected PowerShells in the format of {@code install_path -> PowerShellInfo}
   * @see PowerShellInfo
   */
  @NotNull
  Map<String, PowerShellInfo> findShells(@NotNull final DetectionContext detectionContext);

}
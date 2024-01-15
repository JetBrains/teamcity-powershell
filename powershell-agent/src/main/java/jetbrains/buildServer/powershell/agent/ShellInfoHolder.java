

package jetbrains.buildServer.powershell.agent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ShellInfoHolder {

  private final Map<String, PowerShellInfo> myShells = new LinkedHashMap<>();

  public void addShellInfo(@NotNull String key, @NotNull final PowerShellInfo info) {
    myShells.put(key, info);
  }

  public Map<String, PowerShellInfo> getShells() {
    return Collections.unmodifiableMap(myShells);
  }
}
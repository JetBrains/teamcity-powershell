package jetbrains.buildServer.powershell.agent;

import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ShellInfoHolder {

  private final List<PowerShellInfo> myShells = new ArrayList<PowerShellInfo>();

  public void addShellInfo(@NotNull final PowerShellInfo info) {
    myShells.add(info);
  }

  public List<PowerShellInfo> getShells() {
    return myShells;
  }
}

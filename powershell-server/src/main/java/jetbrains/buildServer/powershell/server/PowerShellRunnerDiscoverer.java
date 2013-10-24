package jetbrains.buildServer.powershell.server;

import com.intellij.openapi.util.io.FileUtil;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.serverSide.BreadthFirstRunnerDiscoveryExtension;
import jetbrains.buildServer.serverSide.DiscoveredBuildRunner;
import jetbrains.buildServer.util.browser.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class PowerShellRunnerDiscoverer extends BreadthFirstRunnerDiscoveryExtension {

  /** Default PowerShell script file extension */
  private static final String PS_EXT = "ps1";

  @NotNull
  @Override
  protected List<DiscoveredBuildRunner> discoverRunnersInDirectory(@NotNull final Element dir, @NotNull final List<Element> filesAndDirs) {
    final List<DiscoveredBuildRunner> runners = new ArrayList<DiscoveredBuildRunner>();
    for (Element e: filesAndDirs) {
      if (e.isLeaf() && PS_EXT.equals(FileUtil.getExtension(e.getName()))) {
        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PowerShellConstants.RUNNER_SCRIPT_FILE, e.getFullName());
        parameters.put(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.values()[0].getValue());
        parameters.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
        parameters.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
        runners.add(new DiscoveredBuildRunner(PowerShellConstants.RUN_TYPE, parameters, PowerShellConstants.RUN_TYPE + " " + e.getName()));
      }
    }
    return runners;
  }
}

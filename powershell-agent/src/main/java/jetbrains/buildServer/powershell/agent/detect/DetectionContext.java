package jetbrains.buildServer.powershell.agent.detect;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class DetectionContext {

  @NotNull
  private static final String PARAM_SEARCH_PATHS = "teamcity.powershell.detector.search.paths";

  @NotNull
  private final Map<PowerShellBitness, String> myPredefinedPaths;

  @NotNull
  private final List<String> mySearchPaths;

  public DetectionContext(@NotNull final BuildAgentConfiguration configuration) {
    final Map<String, String> params = configuration.getConfigurationParameters();
    myPredefinedPaths = loadPredefinedPaths(params);
    mySearchPaths = loadSearchPaths(params);
  }

  /**
   * Loads predefined paths from agent configuration file
   * @param params map of agent configuration parameters
   * @see PowerShellBitness#getPathKey()
   *
   * @return map with specific PowerShell paths
   */
  private Map<PowerShellBitness, String> loadPredefinedPaths(@NotNull final Map<String, String> params) {
    final Map<PowerShellBitness, String> result = new HashMap<PowerShellBitness, String>();
    for (PowerShellBitness bit: PowerShellBitness.values()) {
      String val = params.get(bit.getPathKey());
      if (!StringUtil.isEmptyOrSpaces(val)) {
        result.put(bit, val);
      }
    }
    return result;
  }

  private List<String> loadSearchPaths(@NotNull final Map<String, String> params) {
    String val = params.get(PARAM_SEARCH_PATHS);
    List<String> result;
    if (!StringUtil.isEmptyOrSpaces(val)) {
      result = StringUtil.split(val, ";");
    } else {
      result = Collections.emptyList();
    }
    return result;
  }

  @NotNull
  public Map<PowerShellBitness, String> getPredefinedPaths() {
    return myPredefinedPaths;
  }

  @NotNull
  public List<String> getSearchPaths() {
    return mySearchPaths;
  }
}

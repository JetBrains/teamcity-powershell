

package jetbrains.buildServer.powershell.agent.detect;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class DetectionContextImpl implements DetectionContext {

  @NotNull
  private static final String PARAM_SEARCH_PATHS = "teamcity.powershell.detector.search.paths";

  @NotNull
  private final List<String> mySearchPaths;

  public DetectionContextImpl(@NotNull final BuildAgentConfiguration configuration) {
    mySearchPaths = loadSearchPaths(configuration.getConfigurationParameters().get(PARAM_SEARCH_PATHS));
  }

  private List<String> loadSearchPaths(@Nullable final String paramValue) {
    return StringUtil.isEmptyOrSpaces(paramValue) ? Collections.emptyList() : StringUtil.split(paramValue, ";");
  }

  @Override
  @NotNull
  public List<String> getSearchPaths() {
    return mySearchPaths;
  }
}
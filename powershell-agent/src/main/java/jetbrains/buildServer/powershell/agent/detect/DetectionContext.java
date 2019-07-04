package jetbrains.buildServer.powershell.agent.detect;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DetectionContext {

  /**
   * Additional paths to be used for PowerShell detection
   *
   * @return additional paths
   */
  @NotNull
  List<String> getSearchPaths();
}

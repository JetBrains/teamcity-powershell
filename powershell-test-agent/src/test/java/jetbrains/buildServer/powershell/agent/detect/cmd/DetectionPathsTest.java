package jetbrains.buildServer.powershell.agent.detect.cmd;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.agent.detect.DetectionContext;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DetectionPathsTest extends BaseTestCase {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testPredefinedPathsChildren() throws Exception {
    final File root = createTempDir();
    final File child = new File(root, "child");
    child.mkdir();
    final DetectionContext context = createContext(root.getAbsolutePath());
    List<String> paths = new DetectionPaths().getPaths(context);
    assertTrue(paths.size() >= 2);
    assertEquals(root.getAbsolutePath(), paths.get(0));
    assertEquals(child.getAbsolutePath(), paths.get(1));
  }

  private DetectionContext createContext(final String... paths) {
    return new DetectionContext() {
      @NotNull
      @Override
      public List<String> getSearchPaths() {
        return Arrays.asList(paths);
      }
    };
  }
}

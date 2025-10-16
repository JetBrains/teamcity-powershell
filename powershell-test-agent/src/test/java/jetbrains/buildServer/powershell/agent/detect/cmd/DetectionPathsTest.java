package jetbrains.buildServer.powershell.agent.detect.cmd;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

public class DetectionPathsTest extends BaseTestCase {

  @Test
  public void testPredefinedPathsChildren() throws Exception {
    final File root = createTempDir();
    final File child = new File(root, "child");
    assertTrue(child.mkdir());

    final Mockery m = new Mockery();
    final BuildAgentConfiguration conf = m.mock(BuildAgentConfiguration.class);

    final Map<String, String> confParams = new HashMap<>();
    confParams.put("teamcity.powershell.detector.search.paths", root.getAbsolutePath());
    m.checking(new Expectations(){{
      allowing(conf).getConfigurationParameters(); will(returnValue(Collections.unmodifiableMap(confParams)));
    }});

    List<String> paths = new DetectionPaths(conf).getPaths();
    assertTrue(paths.size() >= 2);
    assertEquals(root.getAbsolutePath(), paths.get(0));
    assertEquals(child.getAbsolutePath(), paths.get(1));
  }
}
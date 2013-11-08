package jetbrains.buildServer.powershell.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.util.browser.Browser;
import jetbrains.buildServer.util.browser.Element;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class PowerShellRunnerDiscovererTest extends BaseTestCase {

  private Mockery m;

  private Browser myBrowser;

  private PowerShellRunnerDiscoverer myDiscoverer;

  private Element myRootElement;

  private BuildTypeSettings myBuildTypeSettings;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myBrowser = m.mock(Browser.class);
    myRootElement = m.mock(Element.class, "root-element");
    myDiscoverer = new PowerShellRunnerDiscoverer();
    myBuildTypeSettings = m.mock(BuildTypeSettings.class);
  }

  @Test
  public void testDiscover_NoFiles() {
    final List<Element> children = Collections.emptyList();
    m.checking(new Expectations() {{
      oneOf(myBrowser).getRoot();
      will(returnValue(myRootElement));

      oneOf(myRootElement).getChildren();
      will(returnValue(children));
    }});

    final List<DiscoveredObject> runners = myDiscoverer.discover(myBuildTypeSettings, myBrowser);
    assertNull(runners);
  }

  @Test
  public void testDiscover_NoSuitableFiles() {
    final Element file1 = m.mock(Element.class, "file1.txt");
    final Element dir1 = m.mock(Element.class, "dir1");
    final Element file2 = m.mock(Element.class, "file2");
    final Element dir2 = m.mock(Element.class, "dir2");

    final List<Element> topLevel = new ArrayList<Element>(Arrays.asList(file1, dir1));
    final List<Element> bottomLevel = new ArrayList<Element>(Arrays.asList(file2, dir2));

    m.checking(new Expectations() {{
      oneOf(myBrowser).getRoot();
      will(returnValue(myRootElement));

      oneOf(myRootElement).getChildren();
      will(returnValue(topLevel));

      atLeast(1).of(file1).isLeaf();
      will(returnValue(true));

      atLeast(1).of(dir1).isLeaf();
      will(returnValue(false));

      oneOf(file1).getName();
      will(returnValue("file1.txt"));

      oneOf(dir1).getChildren();
      will(returnValue(bottomLevel));

      atLeast(1).of(file2).isLeaf();
      will(returnValue(true));

      atLeast(1).of(dir2).isLeaf();
      will(returnValue(false));

      oneOf(file2).getName();
      will(returnValue("file2"));

    }});

    final List<DiscoveredBuildRunner> runners = myDiscoverer.discover(myBrowser);
    assertNull(runners);

  }

  @Test
  public void testDiscover_Deep() {
    final Element file1 = m.mock(Element.class, "file1.txt");
    final Element dir1 = m.mock(Element.class, "dir1");
    final Element file2 = m.mock(Element.class, "file2.ps1");
    final Element dir2 = m.mock(Element.class, "dir2");

    final List<Element> topLevel = new ArrayList<Element>(Arrays.asList(file1, dir1));
    final List<Element> bottomLevel = new ArrayList<Element>(Arrays.asList(file2, dir2));
    final String fullName = "dir1/fil2.ps1";

    m.checking(new Expectations() {{
      oneOf(myBrowser).getRoot();
      will(returnValue(myRootElement));

      oneOf(myRootElement).getChildren();
      will(returnValue(topLevel));

      atLeast(1).of(file1).isLeaf();
      will(returnValue(true));

      atLeast(1).of(dir1).isLeaf();
      will(returnValue(false));

      oneOf(file1).getName();
      will(returnValue("file1.txt"));

      oneOf(dir1).getChildren();
      will(returnValue(bottomLevel));

      atLeast(1).of(file2).isLeaf();
      will(returnValue(true));

      atLeast(1).of(dir2).isLeaf();
      will(returnValue(false));

      atLeast(1).of(file2).getName();
      will(returnValue("file2.ps1"));

      atLeast(1).of(file2).getFullName();
      will(returnValue(fullName));

    }});

    final List<DiscoveredObject> runners = myDiscoverer.discover(myBrowser);
    assertNotNull(runners);
    assertEquals(1, runners.size());
    final DiscoveredObject runner = runners.get(0);
    validateRunner(runner, fullName);
  }

  @Test
  public void testDiscover_Shallow() {
    final Element file1 = m.mock(Element.class, "file1.ps1");
    final Element dir1 = m.mock(Element.class, "dir1");
    final List<Element> topLevel = new ArrayList<Element>(Arrays.asList(file1, dir1));
    final String fullName = "file1.ps1";

    m.checking(new Expectations() {{
      oneOf(myBrowser).getRoot();
      will(returnValue(myRootElement));

      oneOf(myRootElement).getChildren();
      will(returnValue(topLevel));

      atLeast(1).of(file1).isLeaf();
      will(returnValue(true));

      atLeast(1).of(dir1).isLeaf();
      will(returnValue(false));

      atLeast(1).of(file1).getName();
      will(returnValue("file1.ps1"));

      atLeast(1).of(file1).getFullName();
      will(returnValue(fullName));

    }});

    final List<DiscoveredObject> runners = myDiscoverer.discover(myBrowser);
    assertNotNull(runners);
    assertEquals(1, runners.size());
    validateRunner(runners.get(0), fullName);
  }

  private void validateRunner(DiscoveredObject runner, String scriptFullName) {
    assertNotNull(runner);
    assertEquals(PowerShellConstants.RUN_TYPE, runner.getType());
    final Map<String, String> params = runner.getParameters();
    assertEquals(params.get(PowerShellConstants.RUNNER_SCRIPT_FILE), scriptFullName);
    assertEquals(params.get(PowerShellConstants.RUNNER_BITNESS), PowerShellBitness.values()[0].getValue());
    assertEquals(params.get(PowerShellConstants.RUNNER_EXECUTION_MODE), PowerShellExecutionMode.STDIN.getValue());
    assertEquals(params.get(PowerShellConstants.RUNNER_SCRIPT_MODE), PowerShellScriptMode.FILE.getValue());
  }
}
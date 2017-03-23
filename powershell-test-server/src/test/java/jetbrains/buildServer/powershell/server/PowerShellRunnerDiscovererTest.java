/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.buildServer.powershell.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject;
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
    assertNotNull(runners);
    assertEquals(0, runners.size());
  }

  @Test
  public void testDiscover_NoSuitableFiles() {
    final Element file1 = m.mock(Element.class, "file1.txt");
    final Element dir1 = m.mock(Element.class, "dir1");
    final Element file2 = m.mock(Element.class, "file2");
    final Element dir2 = m.mock(Element.class, "dir2");

    final List<Element> topLevel = new ArrayList<>(Arrays.asList(file1, dir1));
    final List<Element> bottomLevel;
    bottomLevel = new ArrayList<>(Arrays.asList(file2, dir2));

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

    final List<DiscoveredObject> runners = myDiscoverer.discover(myBuildTypeSettings, myBrowser);
    assertNotNull(runners);
    assertEquals(0, runners.size());
  }

  @Test
  public void testDiscover_Deep() {
    final Element file1 = m.mock(Element.class, "file1.txt");
    final Element dir1 = m.mock(Element.class, "dir1");
    final Element file2 = m.mock(Element.class, "file2.ps1");
    final Element dir2 = m.mock(Element.class, "dir2");

    final List<Element> topLevel = new ArrayList<>(Arrays.asList(file1, dir1));
    final List<Element> bottomLevel = new ArrayList<>(Arrays.asList(file2, dir2));
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

      allowing(myBuildTypeSettings).getBuildRunners();
      will(returnValue(Collections.emptyList()));

    }});

    final List<DiscoveredObject> runners = myDiscoverer.discover(myBuildTypeSettings, myBrowser);
    assertNotNull(runners);
    assertEquals(1, runners.size());
    final DiscoveredObject runner = runners.get(0);
    validateRunner(runner, fullName);
  }

  @Test
  public void testDiscover_Shallow() {
    final Element file1 = m.mock(Element.class, "file1.ps1");
    final Element dir1 = m.mock(Element.class, "dir1");
    final List<Element> topLevel = new ArrayList<>(Arrays.asList(file1, dir1));
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

      allowing(myBuildTypeSettings).getBuildRunners();
      will(returnValue(Collections.emptyList()));
    }});

    final List<DiscoveredObject> runners = myDiscoverer.discover(myBuildTypeSettings, myBrowser);
    assertNotNull(runners);
    assertEquals(1, runners.size());
    validateRunner(runners.get(0), fullName);
  }

  @Test
  public void testExcludeAlreadyUsedFiles() throws Exception {
    final SBuildRunnerDescriptor definedRunner = m.mock(SBuildRunnerDescriptor.class, "already-defined-descriptor");
    final String scriptFileName = "file1.ps1";
    final Map<String, String> definedRunnerParams = new HashMap<String, String>() {{
      put(PowerShellConstants.RUNNER_SCRIPT_FILE, scriptFileName);
      put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    }};
    final Element file1 = m.mock(Element.class, "file1.ps1");
    final String file2Name = "file2.ps1";
    final Element file2 = m.mock(Element.class, file2Name);
    m.checking(new Expectations() {{
      oneOf(myBrowser).getRoot();
      will(returnValue(myRootElement));

      oneOf(myRootElement).getChildren();
      will(returnValue(Arrays.asList(file1, file2)));

      atLeast(1).of(file1).isLeaf();
      will(returnValue(true));

      atLeast(1).of(file2).isLeaf();
      will(returnValue(true));

      atLeast(1).of(file1).getName();
      will(returnValue(scriptFileName));

      atLeast(1).of(file2).getName();
      will(returnValue(file2Name));

      atLeast(1).of(file1).getFullName();
      will(returnValue(scriptFileName));

      atLeast(1).of(file2).getFullName();
      will(returnValue(file2Name));

      atLeast(1).of(myBuildTypeSettings).getBuildRunners();
      will(returnValue(Collections.singletonList(definedRunner)));

      oneOf(definedRunner).getType();
      will(returnValue(PowerShellConstants.RUN_TYPE));

      allowing(definedRunner).getParameters();
      will(returnValue(definedRunnerParams));
    }});

    final List<DiscoveredObject> runners = myDiscoverer.discover(myBuildTypeSettings, myBrowser);
    assertNotNull(runners);
    assertEquals(1, runners.size());
    validateRunner(runners.get(0), file2Name);
  }

  private void validateRunner(DiscoveredObject runner, String scriptFullName) {
    assertNotNull(runner);
    assertEquals(PowerShellConstants.RUN_TYPE, runner.getType());
    final Map<String, String> params = runner.getParameters();
    assertEquals(params.get(PowerShellConstants.RUNNER_SCRIPT_FILE), scriptFullName);
    assertEquals(params.get(PowerShellConstants.RUNNER_EXECUTION_MODE), PowerShellExecutionMode.PS1.getValue());
    assertEquals(params.get(PowerShellConstants.RUNNER_SCRIPT_MODE), PowerShellScriptMode.FILE.getValue());
    assertNull(params.get(PowerShellConstants.RUNNER_BITNESS));
  }
}
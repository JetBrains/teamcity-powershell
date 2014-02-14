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

package jetbrains.buildServer.powershell.agent;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.powershell.common.PowerShellVersion;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = ScriptGenerator.class)
public class ScriptGeneratorTest extends BaseTestCase {

  private static final String SAMPLE_SCRIPT = "Get-Host\r\n";

  private Mockery m;

  private ScriptGenerator myGenerator;

  private PowerShellInfo myInfo;

  private File myTempDir;

  private File myCheckoutDir;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myInfo = m.mock(PowerShellInfo.class);
    myTempDir = createTempDir();
    myCheckoutDir = createTempDir();
    myGenerator = new ScriptGenerator();
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @Test
  @TestFor(issues = "TW-21554")
  public void testLeaveAsIsForCommandMode() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, SAMPLE_SCRIPT);
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());

    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir, false);
    registerAsTempFile(result);
    // script created in temp directory
    assertEquals(myTempDir, result.getParentFile());
    final String text = FileUtil.readText(result, "UTF-8");
    assertTrue(text.trim().equals(SAMPLE_SCRIPT.trim()));
  }

  @Test
  public void testNoEmptyScriptAllowed() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, "");
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());

    try {
      myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir, false);
      fail("Expected RunBuildException");
    } catch (RunBuildException ignored) {
    }
  }

  @Test
  public void testShouldWrapFile() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    final File temp = createTempFile(SAMPLE_SCRIPT);
    final File script = FileUtil.renameFileNameOnly(temp, temp.getName() + ".ps1");
    registerAsTempFile(script);
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_FILE, script.getCanonicalPath());
    assertTrue(myGenerator.isWrapping(runnerParams, true));
    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir, true);
    registerAsTempFile(result);
    assertFalse(result.getCanonicalPath().equals(script.getCanonicalPath()));
  }

  @Test
  @TestFor(issues = "TW-35069")
  public void testShouldNotWrapCommand() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    final File temp = createTempFile(SAMPLE_SCRIPT);
    final File script = FileUtil.renameFileNameOnly(temp, temp.getName() + ".ps1");
    registerAsTempFile(script);
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_FILE, script.getCanonicalPath());
    assertFalse(myGenerator.isWrapping(runnerParams, true));
    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir, true);
    registerAsTempFile(result);
    assertTrue(result.getCanonicalPath().equals(script.getCanonicalPath()));
  }

  @Test
  public void testShouldTurnOffOnProperty() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, SAMPLE_SCRIPT);
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    assertFalse(myGenerator.isWrapping(runnerParams, false));
    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir, false);
    registerAsTempFile(result);
    // script created in temp directory
    assertEquals(myTempDir, result.getParentFile());
    final String text = FileUtil.readText(result, "UTF-8");
    assertTrue(text.trim().equals(SAMPLE_SCRIPT.trim()));
  }
}

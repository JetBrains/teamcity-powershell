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
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.powershell.common.PowerShellVersion;
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
  public void testNoEmptyScriptAllowed() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, "");
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());

    try {
      myGenerator.generateScript(runnerParams, myCheckoutDir, myTempDir);
      fail("Expected RunBuildException");
    } catch (RunBuildException ignored) {
    }
  }

  /**
   * Script is taken from runner parameters as plain text and dumped into file.
   * After build finishes, file should be removed
   *
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor(issues = "TW-36704")
  public void testShouldRemove_CODE() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, SAMPLE_SCRIPT);
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());

    assertTrue(myGenerator.shouldRemoveGeneratedScript(runnerParams));
  }

  /**
   * Script is taken from file (from VCS). It should not be deleted after build finishes
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor(issues = "TW-36704")
  public void testShouldRemove_FILE() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, SAMPLE_SCRIPT);
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());

    assertFalse(myGenerator.shouldRemoveGeneratedScript(runnerParams));
  }
}

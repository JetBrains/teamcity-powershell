/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
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
public class ScriptGeneratorTest extends BasePowerShellUnitTest {

  private static final String SAMPLE_SCRIPT = "Get-Host\r\n";

  private Mockery m;

  private ScriptGenerator myGenerator;

  private File myTempDir;

  private File myWorkingDir;

  private File myCheckoutDir;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myTempDir = createTempDir();
    myCheckoutDir = createTempDir();
    myWorkingDir = createTempDir();
    myGenerator = new ScriptGenerator();
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @Test(expectedExceptions = RunBuildException.class)
  public void testNoEmptyScriptAllowed() throws Exception {
    final Map<String, String> runnerParams = new HashMap<>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "1.0");
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, "");
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    myGenerator.generateScript(runnerParams, myCheckoutDir, myTempDir, myWorkingDir);
  }

  /**
   * Script is taken from runner parameters as plain text and dumped into file.
   * After build finishes, file should be removed
   *
   */
  @Test
  @TestFor(issues = "TW-36704")
  public void testShouldRemove_CODE() {
    final Map<String, String> runnerParams = new HashMap<>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "1.0");
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, SAMPLE_SCRIPT);
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());

    assertTrue(ScriptGenerator.shouldRemoveGeneratedScript(runnerParams));
  }

  /**
   * Script is taken from file (from VCS). It should not be deleted after build finishes
   */
  @Test
  @TestFor(issues = "TW-36704")
  public void testShouldRemove_FILE() {
    final Map<String, String> runnerParams = new HashMap<>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "1.0");
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, SAMPLE_SCRIPT);
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());

    assertFalse(ScriptGenerator.shouldRemoveGeneratedScript(runnerParams));
  }

  @Test(expectedExceptions = RunBuildException.class)
  @TestFor(issues = "TW-49208")
  public void testGenerateScript_FILE_NoFileExists() throws Exception {
    final Map<String, String> runnerParams = new HashMap<>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "1.0");
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_FILE, "non_existent_script.ps1");
    myGenerator.generateScript(runnerParams, myCheckoutDir, myTempDir, myWorkingDir);
  }

  @Test
  @TestFor(issues = "TW-49208")
  public void testGenerateScript_FILE_Exists() throws Exception {
    final String fileName = "script.ps1";
    final Map<String, String> runnerParams = new HashMap<>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "1.0");
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    File scriptFile = new File(myCheckoutDir, fileName);
    registerAsTempFile(scriptFile);
    FileUtil.writeFile(scriptFile, "Write-Output \"works\"", "UTF-8");
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_FILE, fileName);
    final File resultingScript = myGenerator.generateScript(runnerParams, myCheckoutDir, myTempDir, myWorkingDir);
    assertEquals(scriptFile.getAbsolutePath(), resultingScript.getAbsolutePath());
  }

  @Test
  @TestFor(issues = "TW-66762")
  public void generateScript_FILE_WorkingDir() throws Exception {
    final String fileName = "script.ps1";
    final Map<String, String> runnerParams = new HashMap<>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "1.0");
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    // set working directory
    File scriptFile = new File(myWorkingDir, fileName);
    registerAsTempFile(scriptFile);
    FileUtil.writeFile(scriptFile, "Write-Output \"works\"", "UTF-8");
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_FILE, fileName);
    final File resultingScript = myGenerator.generateScript(runnerParams, myCheckoutDir, myTempDir, myWorkingDir);
    assertEquals(scriptFile.getAbsolutePath(), resultingScript.getAbsolutePath());
  }
}

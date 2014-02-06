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
import org.jetbrains.annotations.NotNull;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
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

  private final String SCRIPT_FILE_NAME = "someMadeUpScriptName.ps1";

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

  // data provider idea - [mode, workingDir, tempDir]

  @Test
  @TestFor(issues = "TW-21554")
  public void testWrapVersion1_Code() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, SAMPLE_SCRIPT);
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());

    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir);
    registerAsTempFile(result);
    // script created in temp directory
    assertEquals(myTempDir, result.getParentFile());
    final String text = FileUtil.readText(result, "UTF-8");
    assertTrue(text.startsWith("trap"));
    assertTrue(text.contains(SAMPLE_SCRIPT));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  @TestFor(issues = "TW-21554")
  public void testWrapVersion1_File() throws Exception {
    final File scriptFile = new File(myCheckoutDir, SCRIPT_FILE_NAME);
    scriptFile.createNewFile();
    FileUtil.writeFile(scriptFile, SAMPLE_SCRIPT, "UTF-8");

    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_FILE, SCRIPT_FILE_NAME);

    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir);
    registerAsTempFile(result);

    // script created in temp directory
    assertEquals(myTempDir, result.getParentFile());
    final String text = FileUtil.readText(result, "UTF-8");
    final String locationPrefix = getLocationPrefix(scriptFile);
    // location switched
    assertTrue(text.startsWith(locationPrefix));
    assertTrue(text.substring(locationPrefix.length()).startsWith("trap"));
    assertTrue(text.contains(SAMPLE_SCRIPT));
  }

  @Test
  @TestFor(issues = "TW-21554")
  public void testWrapVersion2_Code() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_2_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, SAMPLE_SCRIPT);
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());

    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir);
    registerAsTempFile(result);
    // script created in temp directory
    assertEquals(myTempDir, result.getParentFile());
    final String text = FileUtil.readText(result, "UTF-8");
    assertTrue(text.startsWith("try\r\n {\r\n"));
    assertTrue(text.contains(SAMPLE_SCRIPT));
    assertTrue(text.endsWith("Exit 1\r\n}\r\n"));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  @TestFor(issues = "TW-21554")
  public void testWrapVersion2_File() throws Exception {
    final File scriptFile = new File(myCheckoutDir, SCRIPT_FILE_NAME);
    scriptFile.createNewFile();
    FileUtil.writeFile(scriptFile, SAMPLE_SCRIPT, "UTF-8");

    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_2_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_FILE, SCRIPT_FILE_NAME);

    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir);
    registerAsTempFile(result);

    // script created in temp directory
    assertEquals(myTempDir, result.getParentFile());
    final String text = FileUtil.readText(result, "UTF-8");
    final String locationPrefix = getLocationPrefix(scriptFile);
    // location switched
    assertTrue(text.startsWith(locationPrefix));
    assertTrue(text.substring(locationPrefix.length()).startsWith("try\r\n {\r\n"));
    assertTrue(text.contains(SAMPLE_SCRIPT));
    assertTrue(text.endsWith("Exit 1\r\n}\r\n"));
  }

  @Test
  @TestFor(issues = "TW-21554")
  public void testLeaveAsIsForCommandMode() throws Exception {
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_1_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_CODE, SAMPLE_SCRIPT);
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());

    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir);
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
      myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir);
      fail("Expected RunBuildException");
    } catch (RunBuildException ignored) {
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testSwitchLocationToOriginalFile() throws Exception {
    final File subDir = new File(myCheckoutDir + "/1/2/3/4/");
    subDir.mkdirs();
    registerAsTempFile(subDir);
    final File scriptFile = new File(subDir, SCRIPT_FILE_NAME);
    scriptFile.createNewFile();
    FileUtil.writeFile(scriptFile, SAMPLE_SCRIPT, "UTF-8");
    registerAsTempFile(scriptFile);

    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, PowerShellVersion.V_2_0.getVersion());
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_FILE, "1/2/3/4/" + SCRIPT_FILE_NAME);

    final File result = myGenerator.generate(myInfo, runnerParams, myCheckoutDir, myTempDir);
    registerAsTempFile(result);
    // script created in temp directory
    assertEquals(myTempDir, result.getParentFile());
    final String text = FileUtil.readText(result, "UTF-8");
    final String locationPrefix = getLocationPrefix(scriptFile);
    assertTrue(text.startsWith(locationPrefix));
  }

  private String getLocationPrefix(@NotNull final File scriptFile) throws IOException {
    return "Set-Location " + scriptFile.getParentFile().getCanonicalPath() + "\r\n";
  }

}

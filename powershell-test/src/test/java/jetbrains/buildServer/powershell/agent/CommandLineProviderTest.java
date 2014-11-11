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
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellVersion;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CommandLineProviderTest extends BaseTestCase {

  private static final String SCRIPT_FILE_NAME = "script.ps1";

  private Mockery m;

  private PowerShellCommandLineProvider myProvider;

  private File myScriptFile;

  private File myScriptsRootDir;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myProvider = new PowerShellCommandLineProvider();
    myScriptsRootDir = createTempDir();
    myScriptFile = new File(myScriptsRootDir, SCRIPT_FILE_NAME);
    myScriptFile.createNewFile();
    super.registerAsTempFile(myScriptFile);
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @Test(dataProvider = "powerShellVersions")
  @TestFor(issues = "TW-33472")
  public void testPowerShellVersionProvided(@NotNull final PowerShellVersion version) throws Exception {
    final String expectedVersionArg = "-Version";
    final String expectedVersionValue = version.getVersion();
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, version.getVersion());

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(version));
    }});

    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, Collections.<String, String>emptyMap());
    // powershell.exe -Version $version
    assertTrue(result.size() >= 2);
    assertEquals(expectedVersionArg, result.get(0));
    assertEquals(expectedVersionValue, result.get(1));
  }

  @Test(dataProvider = "powerShellVersions")
  @TestFor(issues = "TW-34557")
  public void testScriptArgumentsProvided(@NotNull final PowerShellVersion version) throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    final String args = "arg1 arg2 arg3";
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS, args);
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, version.getVersion());

    final Map<String, String> configParams = new HashMap<String, String>() {{
      put(PowerShellConstants.CONFIG_USE_FILE, "true");
    }};

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(version));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(version.getVersion());
      add("-NonInteractive");
      add("-File");
      add(myScriptFile.getPath());
      addAll(Arrays.asList(args.split("\\s+")));
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }

  @Test
  @TestFor(issues = "TW-34557")
  public void testUseDefaultPowershellIfVersionAny() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      never(info).getVersion();
    }});

    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, Collections.<String, String>emptyMap());
    for (String str: result) {
      if ("-Version".equals(str)) {
        fail("Powershell version should not be supplied if Any is selected in runner parameters");
      }
    }
  }

  @Test
  @TestFor(issues = "TW-34410")
  public void testDirectScript() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "3.0");

    final Map<String, String> configParams = Collections.emptyMap();

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(PowerShellVersion.V_3_0));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(PowerShellVersion.V_3_0.getVersion());
      add("-NonInteractive");
      add(myScriptFile.getPath());
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }

  @Test
  @TestFor(issues = "TW-34410")
  public void testDirectScript_ToLegacy() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "3.0");

    final Map<String, String> configParams = new HashMap<String, String>() {{
      put(PowerShellConstants.CONFIG_USE_FILE, "true");
    }};

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(PowerShellVersion.V_3_0));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(PowerShellVersion.V_3_0.getVersion());
      add("-NonInteractive");
      add("-File");
      add(myScriptFile.getPath());
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }

  @Test
  @SuppressWarnings({"ResultOfMethodCallIgnored"})
  public void testNotEscapeSpacesForFile() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "3.0");
    final String subdirName = "sub dir";
    final File subDir = new File(myScriptsRootDir, subdirName);
    subDir.mkdir();
    final String fileName = "some script.ps1";
    final File scriptFile = new File(subDir, fileName);
    scriptFile.createNewFile();

    final Map<String, String> configParams = new HashMap<String, String>() {{
      put(PowerShellConstants.CONFIG_USE_FILE, "true");
    }};

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(PowerShellVersion.V_3_0));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(PowerShellVersion.V_3_0.getVersion());
      add("-NonInteractive");
      add("-File");
      add(myScriptFile.getPath());
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }

  @Test
  @SuppressWarnings({"ResultOfMethodCallIgnored"})
  public void testEscapeSpacesForDirect() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "3.0");
    final String subdirName = "sub dir";
    final File subDir = new File(myScriptsRootDir, subdirName);
    subDir.mkdir();
    final String fileName = "some script.ps1";
    final File scriptFile = new File(subDir, fileName);
    scriptFile.createNewFile();

    final Map<String, String> configParams = Collections.emptyMap();

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(PowerShellVersion.V_3_0));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(PowerShellVersion.V_3_0.getVersion());
      add("-NonInteractive");
      add(myScriptFile.getPath().replace(" ", "` "));
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }

  @Test
  @SuppressWarnings({"ResultOfMethodCallIgnored"})
  public void testLeavePathAsIsForCommand() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "3.0");
    final String subdirName = "sub dir";
    final File subDir = new File(myScriptsRootDir, subdirName);
    subDir.mkdir();

    final Map<String, String> configParams = Collections.emptyMap();

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(PowerShellVersion.V_3_0));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(PowerShellVersion.V_3_0.getVersion());
      add("-NonInteractive");
      add("-Command");
      add("-");
      add("<");
      add(myScriptFile.getPath());
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }

  /**
   * Direct execution of the script results in arguments being handled by cmd itself.
   * Arguments with quotes must be escaped
   * Arguments must be passed as a single multi-word argument
   *
   * @throws Exception
   */
  @Test
  @TestFor(issues = "TW-35063")
  public void testMultiWordArgs_Direct() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "3.0");
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS, "arg1\r\n\"arg2.1 arg2.2\"\r\narg3\r\narg4 arg5");

    final Map<String, String> configParams = Collections.emptyMap();

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(PowerShellVersion.V_3_0));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(PowerShellVersion.V_3_0.getVersion());
      add("-NonInteractive");
      add(myScriptFile.getPath());
      add("\" arg1 \"\"\"arg2.1 arg2.2\"\"\" arg3 arg4 arg5\"");
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }

  @Test
  @TestFor(issues = "TW-35063")
  public void testMultiWordArgs_File() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "3.0");
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS, "arg1\r\n\"arg2.1 arg2.2\"\r\narg3\r\narg4 arg5");

    final Map<String, String> configParams = new HashMap<String, String>() {{
      put(PowerShellConstants.CONFIG_USE_FILE, "true");
    }};

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(PowerShellVersion.V_3_0));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(PowerShellVersion.V_3_0.getVersion());
      add("-NonInteractive");
      add("-File");
      add(myScriptFile.getPath());
      add("arg1");
      add("\"arg2.1 arg2.2\"");
      add("arg3");
      add("arg4");
      add("arg5");
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }


  /**
   * Since {@code ^} is an escape character in {@code cmd.exe},
   * it must be escaped if we are using direct script execution mode
   *
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor(issues = "TW-37730")
  public void testEscapeCmdChar_Direct() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "3.0");
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS, "-PassToPowerShell\n^MatchTheWholeString$");

    final Map<String, String> configParams = Collections.emptyMap();

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(PowerShellVersion.V_3_0));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(PowerShellVersion.V_3_0.getVersion());
      add("-NonInteractive");
      add(myScriptFile.getPath());
      add("\" -PassToPowerShell ^MatchTheWholeString$\"");
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }

  @Test
  @TestFor(issues = "TW-37730")
  public void testEscapeCmdChar_File() throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    runnerParams.put(PowerShellConstants.RUNNER_MIN_VERSION, "3.0");
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS, "-PassToPowerShell\n^MatchTheWholeString$");

    final Map<String, String> configParams = new HashMap<String, String>() {{
      put(PowerShellConstants.CONFIG_USE_FILE, "true");
    }};

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(PowerShellVersion.V_3_0));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add("-Version");
      add(PowerShellVersion.V_3_0.getVersion());
      add("-NonInteractive");
      add("-File");
      add(myScriptFile.getPath());
      add("-PassToPowerShell");
      add("^MatchTheWholeString$");
    }};
    final List<String> result = myProvider.provideCommandLine(runnerParams, myScriptFile, false, configParams);
    assertSameElements(result, expected);
  }

  @DataProvider(name = "powerShellVersions")
  public Object[][] getVersions() {
    PowerShellVersion[] versions = PowerShellVersion.values();
    final Object[][] result = new Object[versions.length][];
    for (int i = 0; i < versions.length; i++) {
      result[i] = new Object[] {versions[i]};
    }
    return result;
  }
}

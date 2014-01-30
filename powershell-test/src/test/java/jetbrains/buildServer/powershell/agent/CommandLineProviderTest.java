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

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myProvider = new PowerShellCommandLineProvider();
    final File dir = createTempDir();
    myScriptFile = new File(dir, SCRIPT_FILE_NAME);
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

    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(version));
    }});

    final List<String> result = myProvider.provideCommandLine(info, runnerParams, myScriptFile, false);
    // powershell.exe -Version $version
    assertTrue(result.size() >= 3);
    assertEquals(expectedVersionArg, result.get(1));
    assertEquals(expectedVersionValue, result.get(2));
  }

  @Test(dataProvider = "powerShellVersions")
  @TestFor(issues = "TW-34557")
  public void testScriptArgumentsProvided(@NotNull final PowerShellVersion version) throws Exception {
    final PowerShellInfo info = m.mock(PowerShellInfo.class);
    final Map<String, String> runnerParams = new HashMap<String, String>();
    runnerParams.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    final String args = "arg1 arg2 arg3";
    runnerParams.put(PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS, args);
    m.checking(new Expectations() {{
      allowing(info).getExecutablePath();
      will(returnValue("executablePath"));

      allowing(info).getVersion();
      will(returnValue(version));
    }});
    final List<String> expected = new ArrayList<String>() {{
      add(info.getExecutablePath());
      add("-Version");
      add(version.getVersion());
      add("-NonInteractive");
      add("-File");
      add(myScriptFile.getPath());
      addAll(Arrays.asList(args.split("\\s+")));
    }};
    final List<String> result = myProvider.provideCommandLine(info, runnerParams, myScriptFile, false);
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

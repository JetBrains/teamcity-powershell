/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import jetbrains.buildServer.powershell.agent.PowerShellInfoProvider;
import jetbrains.buildServer.powershell.agent.PowerShellService;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellVersion;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CommandLineProviderTest extends BaseTestCase {

  private Mockery m;

  private PowerShellCommandLineProvider myProvider;

  private File myScriptFile;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myProvider = new PowerShellCommandLineProvider();
    myScriptFile = createTempFile();
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

  @DataProvider(name = "powerShellVersions")
  public Object[][] getVersions() {
    PowerShellVersion[] versions = PowerShellVersion.values();
    final Object[][] result = new Object[versions.length][];
    for (int i = 0; i < versions.length; i++) {
      result[i] = new Object[] {versions[i]};
    }
    return result;
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }
}

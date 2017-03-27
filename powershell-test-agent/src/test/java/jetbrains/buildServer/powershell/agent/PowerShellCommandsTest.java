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

import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.agent.system.PowerShellCommands;
import jetbrains.buildServer.powershell.agent.system.SystemBitness;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class PowerShellCommandsTest extends BasePowerShellUnitTest {

  private Mockery m;
  private static final String PS32 = "32bit";
  private static final String PS64 = "64bit";
  private SystemBitness mySystemBitness;
  private PowerShellCommands myCommands;
  private PowerShellInfo my32Info;
  private PowerShellInfo my64Info;
  private Map<String, PowerShellInfo> myShells = new HashMap<String, PowerShellInfo>();
  private final Map<String, String> myEnv = new HashMap<String, String>() {{
    put("windir", "C:\\Windows");
  }};

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myShells.clear();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    my32Info = m.mock(PowerShellInfo.class, "32bit PowerShell");
    m.checking(new Expectations() {{
      allowing(my32Info).getExecutablePath();
      will(returnValue("C:\\Windows\\SysWOW64\\WindowsPowerShell\\v1.0\\powershell.exe"));

      allowing(my32Info).getBitness();
      will(returnValue(PowerShellBitness.x86));
    }});
    myShells.put(PS32, my32Info);

    my64Info = m.mock(PowerShellInfo.class, "64bit PowerShell");
    m.checking(new Expectations() {{
      allowing(my64Info).getExecutablePath();
      will(returnValue("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe"));

      allowing(my64Info).getBitness();
      will(returnValue(PowerShellBitness.x64));
    }});
    myShells.put(PS64, my64Info);
    mySystemBitness = m.mock(SystemBitness.class);
    myCommands = new PowerShellCommands(mySystemBitness);
  }

  @Test(dataProvider = "bitnessVariations")
  public void testPatchPowerShellExecutable(final boolean is32Bit, final String shellName, final String search) {
    addBitnessExpectations(is32Bit);
    final String result = myCommands.getNativeCommand(myShells.get(shellName));
    assertContains(result, search);
  }

  @Test(dataProvider = "commandConditions")
  public void testGetNativeCommand(final boolean is32Bit, final String shellName, final String search) {
    addBitnessExpectations(is32Bit);
    final String result = myCommands.getCMDWrappedCommand(myShells.get(shellName), myEnv);
    assertContains(result, search);
  }

  private void addBitnessExpectations(final boolean is32Bit) {
    m.checking(new Expectations() {{
      allowing(mySystemBitness).is32bit();
      will(returnValue(is32Bit));

      allowing(mySystemBitness).is64bit();
      will(returnValue(!is32Bit));
    }});
  }

  @DataProvider(name = "bitnessVariations")
  public Object[][] getBitnessVariations() {
    final List<Object[]> data = new ArrayList<Object[]>();
    // use 32bit PS from 32bit java
    data.add(new Object[] {true, PS32, "SysWOW64"});
    // use 64bit PS from 32bit java
    data.add(new Object[] {true, PS64, "sysnative"});
    // use 32bit PS from 64bit java
    data.add(new Object[] {false, PS32, "SysWOW64"});
    // use 64bit PS from 64bit java
    data.add(new Object[] {false, PS64, "System32"});
    return data.toArray(new Object[data.size()][]);
  }

  @DataProvider(name = "commandConditions")
  public Object[][] getCommandConditions() {
    final List<Object[]> data = new ArrayList<Object[]>();
    // use 32bit PS from 32bit java
    data.add(new Object[] {true, PS32, "System32"});
    // use 64bit PS from 32bit java
    data.add(new Object[] {true, PS64, "sysnative"});
    // use 32bit PS from 64bit java
    data.add(new Object[] {false, PS32, "SysWOW64"});
    // use 64bit PS from 64bit java
    data.add(new Object[] {false, PS64, "System32"});
    return data.toArray(new Object[data.size()][]);
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }
}

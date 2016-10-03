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
package jetbrains.buildServer.powershell.agent.detect;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.common.PowerShellVersion;
import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.util.Win32RegistryAccessor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import static jetbrains.buildServer.util.Bitness.BIT32;
import static jetbrains.buildServer.util.Win32RegistryAccessor.Hive.LOCAL_MACHINE;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 14:57
 */
public class PowerShellDetectorTest extends BaseTestCase {

  private Mockery m;

  private Win32RegistryAccessor acc;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    acc = m.mock(Win32RegistryAccessor.class);
  }

  @Test
  public void test_readPowerShellVersion_1() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("1.0"));
    }});
    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    assertEquals(ver, PowerShellVersion.V_1_0);
  }

  @Test
  public void test_readPowerShellVersion_2() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    assertEquals(ver, PowerShellVersion.V_2_0);
  }

  @Test
  public void test_readPowerShellVersion_3() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue("3.0"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    assertEquals(ver, PowerShellVersion.V_3_0);
  }

  @Test
  public void test_readPowerShellVersion_4() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue("4.0"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    assertEquals(ver, PowerShellVersion.V_4_0);
  }

  @Test
  public void test_readPowerShellVersion_none() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue(null));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    assertNull(ver);
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void test_readPowerShellHome() throws IOException {
    final File hom = createTempDir();
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(hom.getPath()));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    assertEquals(home, hom);
  }

  @Test
  public void test_readPowerShellHome3() throws IOException {
    final File hom = createTempDir();
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase"); will(returnValue(hom.getPath()));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    assertEquals(home, hom);
  }

  @Test
  public void test_readPowerShellHomeNone() throws IOException {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    assertNull(home);
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void test_readPowerShellHome_notExists() throws IOException {
    final File hom = new File("zzz");
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(hom.getPath()));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    assertNull(home);
  }

  @Test
  public void test_isInstalled() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue("1"));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    assertTrue(is);
  }

  @Test
  public void test_isInstalled_3() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue("1"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue(null));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    assertTrue(is);
  }

  @Test
  public void test_isInstalled_3x() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue("1"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue("1"));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    assertTrue(is);
  }

  @Test
  public void test_isInstalled_not() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue("z"));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    assertFalse(is);
  }

  @Test
  public void test_isInstalled_not2() {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue(null));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    assertFalse(is);
  }

  @Test
  @TestFor(issues = "TW-41000")
  public void testDetectPowerShell_5() throws Exception {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue("5.0"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    assertEquals(ver, PowerShellVersion.V_5_0);
  }

  @Test
  @TestFor(issues = "TW-46689")
  public void testDetectPowerShell_5_1() throws Exception {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue("5.1.14393.0"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    assertEquals(ver, PowerShellVersion.V_5_1);
  }

  @Test
  @TestFor(issues = "TW-41000")
  public void testDetectPowerShell_ExtendedVersion() throws Exception {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue("5.0.10105"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    assertEquals(ver, PowerShellVersion.V_5_0);
  }

  @Test
  @TestFor(issues = "TW-41000")
  public void testAgentRequirements_ExtendedVersionRegexp() throws Exception {
    for (PowerShellVersion v: PowerShellVersion.values()) {
      final Pattern p = Pattern.compile(v.getVersionRegex());
      for (String str: getVersions(v)) {
        assertTrue(p.matcher(str).matches());
        assertEquals(v, PowerShellVersion.fromString(str));
      }
    }
  }

  @NotNull
  private String[] getVersions(PowerShellVersion v) {
    return new String[] {
            v.getVersion(),
            v.getVersion() + ".100",
            v.getVersion() + ".100.1000"
    };
  }


}

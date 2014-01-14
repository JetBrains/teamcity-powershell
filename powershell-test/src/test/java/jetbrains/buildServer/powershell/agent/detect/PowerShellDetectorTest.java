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
import jetbrains.buildServer.util.Win32RegistryAccessor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static jetbrains.buildServer.util.Bitness.BIT32;
import static jetbrains.buildServer.util.Win32RegistryAccessor.Hive.LOCAL_MACHINE;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 14:57
 */
public class PowerShellDetectorTest extends BaseTestCase {

  @Test
  public void test_readPowerShellVersion_1() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("1.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    Assert.assertEquals(ver, PowerShellVersion.V_1_0);
  }

  @Test
  public void test_readPowerShellVersion_2() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    Assert.assertEquals(ver, PowerShellVersion.V_2_0);
  }

  @Test
  public void test_readPowerShellVersion_3() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue("3.0"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    Assert.assertEquals(ver, PowerShellVersion.V_3_0);
  }

  @Test
  public void test_readPowerShellVersion_4() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue("4.0"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    Assert.assertEquals(ver, PowerShellVersion.V_4_0);
  }

  @Test
  public void test_readPowerShellVersion_none() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue(null));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    Assert.assertNull(ver);
  }

  @Test
  public void test_readPowerShellHome() throws IOException {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    final File hom = createTempDir();

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(hom.getPath()));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    Assert.assertEquals(home, hom);
  }

  @Test
  public void test_readPowerShellHome3() throws IOException {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    final File hom = createTempDir();

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase"); will(returnValue(hom.getPath()));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    Assert.assertEquals(home, hom);
  }

  @Test
  public void test_readPowerShellHomeNone() throws IOException {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    Assert.assertNull(home);
  }

  @Test
  public void test_readPowerShellHome_notExists() throws IOException {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    final File hom = new File("zzz");
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(hom.getPath()));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    Assert.assertNull(home);
  }

  @Test
  public void test_isInstalled() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue("1"));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    Assert.assertTrue(is);
  }

  @Test
  public void test_isInstalled_3() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue("1"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue(null));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    Assert.assertTrue(is);
  }

  @Test
  public void test_isInstalled_3x() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue("1"));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue("1"));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    Assert.assertTrue(is);
  }

  @Test
  public void test_isInstalled_not() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue("z"));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    Assert.assertFalse(is);
  }

  @Test
  public void test_isInstalled_not2() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install"); will(returnValue(null));
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue(null));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    Assert.assertFalse(is);
  }

}

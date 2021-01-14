/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class PowerShellInfoProviderTest extends BasePowerShellUnitTest {

  private Mockery m;

  private PowerShellInfoProvider myProvider;

  private BuildAgentConfiguration myConfig;

  private ShellInfoHolder myHolder;

  private EventDispatcher<AgentLifeCycleListener> myEvents;

  private File myTempHome;

  @Override
  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myEvents = m.mock(EventDispatcher.class);
    myConfig = m.mock(BuildAgentConfiguration.class);
    myHolder = new ShellInfoHolder();
    m.checking(new Expectations() {{
      allowing(myEvents);
    }});
    myTempHome = createTempDir();
    myProvider = new PowerShellInfoProvider(myConfig, myEvents, Collections.emptyList(), myHolder);
  }

  @Test
  public void testNull_NoToolDetected() {
    assertNull(myProvider.selectTool(PowerShellBitness.x86, null, null));
  }

  @Test(dataProvider = "editionProvider")
  public void testNull_BitnessNotSatisfied_32_64(@NotNull final PowerShellEdition edition) throws Exception {
    myHolder.addShellInfo(new PowerShellInfo(PowerShellBitness.x86, createTempDir(), "1.0", edition, "powershell.exe"));
    assertNull(myProvider.selectTool(PowerShellBitness.x64, null, null));
  }

  @Test(dataProvider = "editionProvider")
  public void testNull_BitnessNotSatisfied_64_32(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<>();
    mock64bit("1.0", edition);
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    assertNull(myProvider.selectTool(PowerShellBitness.x86, null, null));
  }


  /**
   * Neither 64 bit, not 32 bit PowerShells has good enough version
   */
  @Test(dataProvider = "editionProvider")
  public void testNull_MinVersionNotSatisfied_32bit(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<>();
    mock32Bit("1.0", edition);
    mock64bit("2.0", edition);
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    assertNull(myProvider.selectTool(null, "3.0", null));
  }

  /**
   * Both 64 bit and 32 bit PowerShells are installed
   * Any bitness is required
   * Any version is required
   * 64bit should be used
   */
  @Test(dataProvider = "editionProvider")
  public void testSelect64Over32(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<>();
    mock32Bit("2.0", edition);
    mock64bit("2.0", edition);
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    final PowerShellInfo info = myProvider.selectTool(null, null, null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  /**
   * Both 64 bit and 32 bit PowerShells have good enough versions.
   * Any bitness is required
   * 64bit should be used
   */
  @Test(dataProvider = "editionProvider")
  public void testSelect32OVer64_Version(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<>();
    mock32Bit("5.0", edition);
    mock64bit("5.0", edition);
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    final PowerShellInfo info = myProvider.selectTool(null, "3.0", null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  @Test(dataProvider = "editionProvider")
  public void testSelectExact(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<>();
    mock32Bit("5.0", edition);
    mock64bit("6.0", edition);
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    final PowerShellInfo info = myProvider.selectTool(PowerShellBitness.x64, "6.0", null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  @Test(dataProvider = "editionProvider")
  public void testUseSemanticVersion(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<>();
    mock32Bit("3.0-beta4", edition);
    mock64bit("6.0-alpha6", edition);
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    final PowerShellInfo info = myProvider.selectTool(null, "4.0", null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
    assertEquals("6.0-alpha6", info.getVersion());
  }

  @Test(dataProvider = "editionProvider")
  public void testLoadInfo(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<>();
    mock32Bit("1.0", edition);
    mock64bit("6.0", edition);
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    assertTrue(myProvider.anyPowerShellDetected());
  }

  @Test
  public void testFilterByEdition() {
    final Map<String, String> params = new HashMap<>();
    mock32Bit("5.0", PowerShellEdition.DESKTOP);
    mock64bit("6.0", PowerShellEdition.CORE);
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});

    final PowerShellInfo infoCore = myProvider.selectTool(null, null, PowerShellEdition.CORE);
    assertNotNull(infoCore);
    assertEquals(PowerShellBitness.x64, infoCore.getBitness());
    assertEquals("6.0", infoCore.getVersion());
    assertEquals(PowerShellEdition.CORE, infoCore.getEdition());

    final PowerShellInfo infoDesktop = myProvider.selectTool(null, null, PowerShellEdition.DESKTOP);
    assertNotNull(infoDesktop);
    assertEquals(PowerShellBitness.x86, infoDesktop.getBitness());
    assertEquals("5.0", infoDesktop.getVersion());
    assertEquals(PowerShellEdition.DESKTOP, infoDesktop.getEdition());
  }

  @Test
  @TestFor(issues = "TW-55922")
  public void testSelectTool_MultipleShells() {
    final Map<String, String> params = new HashMap<>();
    mock64bit("6.1.0-preview.2", PowerShellEdition.CORE);
    mock64bit("6.0", PowerShellEdition.CORE);
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    final PowerShellInfo info = myProvider.selectTool(PowerShellBitness.x64, null, PowerShellEdition.CORE);
    assertNotNull(info);
    assertEquals("6.1.0-preview.2", info.getVersion());
  }

  private void mock32Bit(@NotNull final String version, @NotNull final PowerShellEdition edition) {
    mockInstance(PowerShellBitness.x86, version, edition);
  }

  private void mock64bit(@NotNull final String version, @NotNull final PowerShellEdition edition) {
    mockInstance(PowerShellBitness.x64, version, edition);
  }

  private void mockInstance(@NotNull final PowerShellBitness bits,
                            @NotNull final String version,
                            @NotNull final PowerShellEdition edition) {
    myHolder.addShellInfo(new PowerShellInfo(bits, myTempHome, version, edition, "powershell"));
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }
}

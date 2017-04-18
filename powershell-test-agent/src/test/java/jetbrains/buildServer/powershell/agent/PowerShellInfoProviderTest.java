/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent;

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.detect.PowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

  private EventDispatcher<AgentLifeCycleListener> myEvents;

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
    m.checking(new Expectations() {{
      allowing(myEvents);
    }});
    myProvider = new PowerShellInfoProvider(myConfig, myEvents, Collections.<PowerShellDetector>emptyList());
  }

  @Test
  public void testNull_NoToolDetected() {
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(Collections.emptyMap()));
    }});
    assertNull(myProvider.selectTool(PowerShellBitness.x86, null, null));
  }

  @Test(dataProvider = "editionProvider")
  public void testNull_BitnessNotSatisfied_32_64(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("1.0", edition));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    assertNull(myProvider.selectTool(PowerShellBitness.x64, null, null));
  }

  @Test(dataProvider = "editionProvider")
  public void testNull_BitnessNotSatisfied_64_32(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock64bit("1.0", edition));
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
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("1.0", edition));
    params.putAll(mock64bit("2.0", edition));
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
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("2.0", edition));
    params.putAll(mock64bit("2.0", edition));
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
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("5.0", edition));
    params.putAll(mock64bit("5.0", edition));
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
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("5.0", edition));
    params.putAll(mock64bit("6.0", edition));
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
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("3.0-beta4", edition));
    params.putAll(mock64bit("6.0-alpha6", edition));
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
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("1.0", edition));
    params.putAll(mock64bit("6.0", edition));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    assertTrue(myProvider.anyPowerShellDetected());
  }
  
  @Test
  public void testFilterByEdition() {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("5.0", PowerShellEdition.DESKTOP));
    params.putAll(mock64bit("6.0", PowerShellEdition.CORE));
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
  
  private Map<String, String> mock32Bit(@NotNull final String version, @NotNull final PowerShellEdition edition) {
    return mockInstance(PowerShellBitness.x86, version, edition);
  }

  private Map<String, String> mock64bit(@NotNull final String version, @NotNull final PowerShellEdition edition) {
    return mockInstance(PowerShellBitness.x64, version, edition);
  }

  private Map<String, String> mockInstance(@NotNull final PowerShellBitness bits,
                                           @NotNull final String version,
                                           @NotNull final PowerShellEdition edition) {
    final Map<String, String> result = new HashMap<String, String>();
    result.put(bits.getVersionKey(), version);
    result.put(bits.getPathKey(), "/path/to/" + bits.name());
    result.put(bits.getEditionKey(), edition.getValue());
    return result;
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }
}

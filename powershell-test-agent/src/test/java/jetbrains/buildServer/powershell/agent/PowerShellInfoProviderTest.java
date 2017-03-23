package jetbrains.buildServer.powershell.agent;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.detect.PowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
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
public class PowerShellInfoProviderTest extends BaseTestCase {

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
    assertNull(myProvider.selectTool(PowerShellBitness.x86, null));
  }

  @Test
  public void testNull_BitnessNotSatisfied_32_64() {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("1.0"));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    assertNull(myProvider.selectTool(PowerShellBitness.x64, null));
  }

  @Test
  public void testNull_BitnessNotSatisfied_64_32() {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock64bit("1.0"));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    assertNull(myProvider.selectTool(PowerShellBitness.x86, null));
  }


  /**
   * Neither 64 bit, not 32 bit PowerShells has good enough version
   */
  @Test
  public void testNull_MinVersionNotSatisfied_32bit() {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("1.0"));
    params.putAll(mock64bit("2.0"));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    assertNull(myProvider.selectTool(null, "3.0"));
  }

  /**
   * Both 64 bit and 32 bit PowerShells are installed
   * Any bitness is required
   * Any version is required
   * 64bit should be used
   */
  @Test
  public void testSelect64Over32() {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("2.0"));
    params.putAll(mock64bit("2.0"));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    final PowerShellInfo info = myProvider.selectTool(null, null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  /**
   * Both 64 bit and 32 bit PowerShells have good enough versions.
   * Any bitness is required
   * 64bit should be used
   */
  @Test
  public void testSelect32OVer64_Version() {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("5.0"));
    params.putAll(mock64bit("5.0"));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    final PowerShellInfo info = myProvider.selectTool(null, "3.0");
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  @Test
  public void testSelectExact() {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("5.0"));
    params.putAll(mock64bit("6.0"));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    final PowerShellInfo info = myProvider.selectTool(PowerShellBitness.x64, "6.0");
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  @Test
  public void testUseSemanticVersion() {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("3.0-beta4"));
    params.putAll(mock64bit("6.0-alpha6"));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    final PowerShellInfo info = myProvider.selectTool(null, "4.0");
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
    assertEquals("6.0-alpha6", info.getVersion());
  }

  @Test
  public void testLoadInfo() {
    final Map<String, String> params = new HashMap<String, String>();
    params.putAll(mock32Bit("1.0"));
    params.putAll(mock64bit("6.0"));
    m.checking(new Expectations() {{
      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});
    assertTrue(myProvider.anyPowerShellDetected());
  }

  private Map<String, String> mock32Bit(@NotNull final String version) {
    return mockInstance(PowerShellBitness.x86, version);
  }

  private Map<String, String> mock64bit(@NotNull final String version) {
    return mockInstance(PowerShellBitness.x64, version);
  }

  private Map<String, String> mockInstance(@NotNull final PowerShellBitness bits,
                                           @NotNull final String version) {
    final Map<String, String> result = new HashMap<String, String>();
    result.put(bits.getVersionKey(), version);
    result.put(bits.getPathKey(), "/path/to/" + bits.name());
    return result;
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }
}

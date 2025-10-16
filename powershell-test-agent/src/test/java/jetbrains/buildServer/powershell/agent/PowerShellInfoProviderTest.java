package jetbrains.buildServer.powershell.agent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.agent.detect.cmd.CommandLinePowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.registry.RegistryPowerShellDetector;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

  private ExtensionHolder myExtensionHolder;

  private File myTempHome;
  private EventDispatcher<AgentLifeCycleListener> myDispatcher;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myExtensionHolder = m.mock(ExtensionHolder.class);
    myConfig = m.mock(BuildAgentConfiguration.class);
    RegistryPowerShellDetector registryPowerShellDetector = m.mock(RegistryPowerShellDetector.class);
    CommandLinePowerShellDetector commandLinePowerShellDetector = m.mock(CommandLinePowerShellDetector.class);
    myHolder = new ShellInfoHolder();
    m.checking(new Expectations() {{
      allowing(myExtensionHolder);
    }});
    myTempHome = createTempDir();
    myDispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    myProvider = new PowerShellInfoProvider(myExtensionHolder, registryPowerShellDetector, commandLinePowerShellDetector, myDispatcher, myHolder);
  }

  @Test
  public void testNull_NoToolDetected() {
    assertNull(myProvider.selectTool(PowerShellBitness.x86, null, null));
  }

  @Test(dataProvider = "editionProvider")
  public void testNull_BitnessNotSatisfied_32_64(@NotNull final PowerShellEdition edition) throws Exception {
    myHolder.addShellInfo("mockKey", new PowerShellInfo(PowerShellBitness.x86, createTempDir(), "1.0", edition, "powershell.exe"));
    assertNull(myProvider.selectTool(PowerShellBitness.x64, null, null));
  }

  @Test(dataProvider = "editionProvider")
  public void testNull_BitnessNotSatisfied_64_32(@NotNull final PowerShellEdition edition) {
    mock64bit("1.0", edition);
    assertNull(myProvider.selectTool(PowerShellBitness.x86, null, null));
  }


  /**
   * Neither 64 bit, not 32 bit PowerShells has good enough version
   */
  @Test(dataProvider = "editionProvider")
  public void testNull_MinVersionNotSatisfied_32bit(@NotNull final PowerShellEdition edition) {
    mock32Bit("1.0", edition);
    mock64bit("2.0", edition);
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
    mock32Bit("2.0", edition);
    mock64bit("2.0", edition);
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
    mock32Bit("5.0", edition);
    mock64bit("5.0", edition);
    final PowerShellInfo info = myProvider.selectTool(null, "3.0", null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  @Test(dataProvider = "editionProvider")
  public void testSelectExact(@NotNull final PowerShellEdition edition) {
    mock32Bit("5.0", edition);
    mock64bit("6.0", edition);
    final PowerShellInfo info = myProvider.selectTool(PowerShellBitness.x64, "6.0", null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  @Test(dataProvider = "editionProvider")
  public void testUseSemanticVersion(@NotNull final PowerShellEdition edition) {
    mock32Bit("3.0-beta4", edition);
    mock64bit("6.0-alpha6", edition);
    final PowerShellInfo info = myProvider.selectTool(null, "4.0", null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
    assertEquals("6.0-alpha6", info.getVersion());
  }

  @Test(dataProvider = "editionProvider")
  public void testLoadInfo(@NotNull final PowerShellEdition edition) {
    mock32Bit("1.0", edition);
    mock64bit("6.0", edition);
    assertTrue(myProvider.anyPowerShellDetected());
  }

  @Test
  public void testFilterByEdition() {
    mock32Bit("5.0", PowerShellEdition.DESKTOP);
    mock64bit("6.0", PowerShellEdition.CORE);

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
    mock64bit("6.1.0-preview.2", PowerShellEdition.CORE);
    mock64bit("6.0", PowerShellEdition.CORE);
    final PowerShellInfo info = myProvider.selectTool(PowerShellBitness.x64, null, PowerShellEdition.CORE);
    assertNotNull(info);
    assertEquals("6.1.0-preview.2", info.getVersion());
  }

  @Test(dataProvider = "editionProvider")
  public void testSelectExact_OnAgenStart(@NotNull final PowerShellEdition edition) {
    final Map<String, String> params = new HashMap<>();
    PowerShellInfo powerShellInfo = getMockPowershellInfo(PowerShellBitness.x64, "5.0", edition);
    powerShellInfo.saveInfo(params);
    LegacyKeys.fillLegacyKeys(params, powerShellInfo.getBitness(), powerShellInfo);

    BuildAgent buildAgent = m.mock(BuildAgent.class);
    m.checking(new Expectations() {{
      allowing(buildAgent).getConfiguration();
      will(returnValue(myConfig));

      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});

    myDispatcher.getMulticaster().agentStarted(buildAgent);
    final PowerShellInfo info = myProvider.selectTool(PowerShellBitness.x64, "5.0", null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  @Test
  public void testSelectExact_OnAgenStartWithNullEdition() {
    final Map<String, String> params = new HashMap<>();
    PowerShellInfo powerShellInfo = getMockPowershellInfo(PowerShellBitness.x64, "5.0", null);
    powerShellInfo.saveInfo(params);
    LegacyKeys.fillLegacyKeys(params, powerShellInfo.getBitness(), powerShellInfo);

    BuildAgent buildAgent = m.mock(BuildAgent.class);
    m.checking(new Expectations() {{
      allowing(buildAgent).getConfiguration();
      will(returnValue(myConfig));

      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});

    myDispatcher.getMulticaster().agentStarted(buildAgent);
    final PowerShellInfo info = myProvider.selectTool(PowerShellBitness.x64, "5.0", null);
    assertNotNull(info);
    assertEquals(PowerShellBitness.x64, info.getBitness());
  }

  @Test
  public void testSelectExact_OnAgenStartWithWrongBadKey() {
    final Map<String, String> params = new HashMap<>();
    params.put(PowerShellConstants.POWERSHELL_PREFIX + "something_fake", "value");

    BuildAgent buildAgent = m.mock(BuildAgent.class);
    m.checking(new Expectations() {{
      allowing(buildAgent).getConfiguration();
      will(returnValue(myConfig));

      allowing(myConfig).getConfigurationParameters();
      will(returnValue(params));
    }});

    myDispatcher.getMulticaster().agentStarted(buildAgent);
    final PowerShellInfo info = myProvider.selectTool(null, null, null);
    assertNull(info);
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
    myHolder.addShellInfo(PowerShellConstants.generateFullKey(edition, bits, version), getMockPowershellInfo(bits, version, edition));
  }

  @NotNull
  private PowerShellInfo getMockPowershellInfo(@NotNull PowerShellBitness bits,
                                               @NotNull String version,
                                               @Nullable PowerShellEdition edition) {
    return new PowerShellInfo(bits, myTempHome, version, edition, "powershell");
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }
}
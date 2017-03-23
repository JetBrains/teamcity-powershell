package jetbrains.buildServer.powershell.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.requirements.RequirementType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class PowerShellRunTypeTest extends BaseTestCase {

  private Mockery m;
  private PowerShellRunType runType;
  private RunTypeRegistry registry;
  private PluginDescriptor descriptor;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    registry = m.mock(RunTypeRegistry.class);
    descriptor = m.mock(PluginDescriptor.class);
    m.checking(new Expectations() {{
      allowing(registry);
      allowing(descriptor);
    }});
    runType = new PowerShellRunType(registry, descriptor);

  }

  @Test(dataProvider = "bitnessAndVersionProvider")
  @TestFor(issues = "TW-33570")
  public void shouldProviderSemanticVersionRestriction(@NotNull final PowerShellBitness bitness, @Nullable final String version) {
    final Map<String, String> parameters = createDummyParameters(bitness);
    if (version != null) {
      parameters.put(PowerShellConstants.RUNNER_MIN_VERSION, version);
    }
    final Collection<Requirement> requirements = runType.getRunnerSpecificRequirements(parameters);
    assertEquals(1, requirements.size());
    final Requirement req = requirements.iterator().next();
    assertEquals(bitness.getVersionKey(), req.getPropertyName());
    if (version == null) {
      assertEquals(RequirementType.EXISTS, req.getType());
    } else {
      assertEquals(RequirementType.VER_NO_LESS_THAN, req.getType());
      assertEquals(version, req.getPropertyValue());
    }
  }

  @Test(dataProvider = "bitnessProvider")
  public void shouldUseInternalValueAsPArtOfRequirement(@NotNull final PowerShellBitness bit) {
    final Collection<Requirement> requirements = runType.getRunnerSpecificRequirements(createDummyParameters(bit));
    assertEquals(1, requirements.size());
    final Requirement req = requirements.iterator().next();
    final String versionKey = bit.getVersionKey();
    assertEquals(versionKey, req.getPropertyName());
    assertEquals("powershell_" + bit.getValue(), versionKey);
  }

  // do we need migration here? what is chosen by default? is the default setting even stored?

  @Test
  @TestFor(issues = "TW-44808")
  public void testGenerateAnyBitnessRequirement_NoMinVersion() throws Exception {
    final Map<String, String> input = createDummyParameters(null);
    final Collection<Requirement> requirements = runType.getRunnerSpecificRequirements(input);
    assertEquals(1, requirements.size());
    final Requirement r = requirements.iterator().next();
    assertEquals("Exists=>(powershell_x86|powershell_x64)", r.getPropertyName());
    assertNull(r.getPropertyValue());
    assertEquals(RequirementType.EXISTS, r.getType());
  }

  @Test(dataProvider = "versionProvider")
  @TestFor(issues = "TW-44808")
  public void testGenerateAnyBitnessRequirement_WithMinVersion(@NotNull final String version) throws Exception {
    final Map<String, String> input = createDummyParameters(null);
    input.put(PowerShellConstants.RUNNER_MIN_VERSION, version);
    final Collection<Requirement> requirements = runType.getRunnerSpecificRequirements(input);
    assertEquals(1, requirements.size());
    final Requirement r = requirements.iterator().next();
    assertEquals("Exists=>(powershell_x86|powershell_x64)", r.getPropertyName());
    assertEquals(version, r.getPropertyValue());
    assertEquals(RequirementType.VER_NO_LESS_THAN, r.getType());
  }
  
  private Map<String, String> createDummyParameters(@Nullable final PowerShellBitness bit) {
    final Map<String, String> result = CollectionsUtil.asMap(
        PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue(),
        PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue(),
        PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works"
    );
    if (bit != null) {
      result.put(PowerShellConstants.RUNNER_BITNESS, bit.getValue());
    }
    return result;
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @DataProvider(name = "bitnessAndVersionProvider")
  public Object[][] getBitnessAndVersion() {
    String[] versions = sampleVersions();
    final PowerShellBitness[] bits = PowerShellBitness.values();
    final Object[][] result = new Object[versions.length * bits.length + 2][];
    int k = 0;
    for (PowerShellBitness bit: bits) {
      for (String version: versions) {
        result[k++] = new Object[]{bit, version};
      }
      result[k++] = new Object[]{bit, null};
    }
    return result;
  }

  @DataProvider(name = "versionProvider")
  public Object[][] getVersion() {
    String[] versions = sampleVersions();
    final Object[][] result = new Object[versions.length][1];
    int k = 0;
    for (String ver: versions) {
      result[k++] = new Object[] {ver};
    }
    return result;
  }

  @DataProvider(name = "bitnessProvider")
  public Object[][] getBitness() {
    final PowerShellBitness[] bits = PowerShellBitness.values();
    Object[][] result = new Object[bits.length][];
    int k = 0;
    for (PowerShellBitness bit: bits) {
      result[k++] = new Object[] {bit};
    }
    return result;
  }

  private String[] sampleVersions() {
    return new String[] {"1.0", "2.0", "3.0", "4.0", "5.0", "5.1", "6.0"};
  }
}

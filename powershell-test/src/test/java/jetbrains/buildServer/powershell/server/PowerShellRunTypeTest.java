package jetbrains.buildServer.powershell.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.common.*;
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

  @Test(dataProvider = "versionProvider")
  @TestFor(issues = "TW-33570")
  public void shouldProviderSemanticVersionRestriction(@NotNull final PowerShellBitness bitness, @Nullable final String version) {
    final Map<String, String> parameters = CollectionsUtil.asMap(
            PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue(),
            PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue(),
            PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works",
            PowerShellConstants.RUNNER_BITNESS, bitness.getValue()
    );
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

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @DataProvider(name = "versionProvider")
  public Object[][] getVersion() {
    String[] versions = {"1.0", "2.0", "3.0", "4.0", "5.0", "5.1", "6.0"};
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
}

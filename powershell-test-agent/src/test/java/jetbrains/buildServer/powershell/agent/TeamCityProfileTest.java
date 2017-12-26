package jetbrains.buildServer.powershell.agent;

import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class TeamCityProfileTest  extends AbstractPowerShellIntegrationTest {

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_provide_build_profile(PowerShellBitness bits) throws Throwable {
    setBuildConfigurationParameter(PowerShellConstants.CONFIG_KEEP_GENERATED, "true");
    setBuildConfigurationParameter(PowerShellConstants.PARAM_NAME_LOAD_TC_PROFILE, "true");
    setBuildSystemProperty("system.custom.property.name", "this_is_system_property_value");
    setRunnerParameter(PowerShellConstants.RUNNER_NO_PROFILE, ""); // todo: this should be ignored when profile is loaded
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "Write-Output $TEAMCITY['custom.property.name']");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue("Build log does not contain system property value", getBuildLog(build).contains("this_is_system_property_value"));
  }
}

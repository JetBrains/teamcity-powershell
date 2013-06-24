package jetbrains.buildServer.powershell.agent.detect;

import jetbrains.buildServer.RunnerTest2Base;
import jetbrains.buildServer.powershell.common.*;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.util.TestFor;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created 18.06.13 12:59
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class PowerShellIntegrationTests extends RunnerTest2Base {
  @BeforeMethod
  @Override
  protected void setUp1() throws Throwable {
    super.setUp1();
    setPartialMessagesCheckerEx();
  }

  @NotNull
  @Override
  protected String getRunnerType() {
    return PowerShellConstants.RUN_TYPE;
  }

  @Override
  protected String getTestDataSuffixPath() {
    return "";
  }

  @Test
  @TestFor(issues = "TW-29803")
  public void should_run_simple_command() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
  }
}

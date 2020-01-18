/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import org.junit.Assert;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildFailureTests extends AbstractPowerShellIntegrationTest {

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_fail_on_uncaught_exception_stdin(@NotNull final PowerShellBitness bitness) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, "2.0");
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "throw \"You shall not pass!\"");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bitness.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_fail_on_exception_file(@NotNull final PowerShellBitness bitness) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE,
            "try { \n" +
                    " throw \"You shall not pass!\"" +
                    "} \n" +
                    "Catch\n" +
                    "{\n" +
                    "    $ErrorMessage = $_.Exception.Message\n" +
                    "    Write-Output $ErrorMessage\n" +
                    "    exit(1) \n"+
                    "}");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bitness.getValue());
    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_fail_syntax_error_file(@NotNull final PowerShellBitness bitness) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "callToSomeNonExistentFunction(param1, param2)");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bitness.getValue());

    setRunnerParameter(PowerShellConstants.RUNNER_LOG_ERR_TO_ERROR, "true");
    getBuildType().setOption(SBuildType.BT_FAIL_ON_ANY_ERROR_MESSAGE, true);

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_fail_syntax_error_cmd(@NotNull final PowerShellBitness bitness) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, "2.0");
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "callToSomeNonExistentFunction(param1, param2)");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bitness.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_LOG_ERR_TO_ERROR, "true");

    getBuildType().setOption(SBuildType.BT_FAIL_ON_ANY_ERROR_MESSAGE, true);

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_fail_on_error_output_cmd(@NotNull final PowerShellBitness bitness) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, "2.0");
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "$res = \"Epic fail\" \nWrite-Error $res");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bitness.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_fail_on_error_output_file(@NotNull final PowerShellBitness bitness) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "$res = \"Epic fail\" \nWrite-Error $res");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bitness.getValue());

    setRunnerParameter(PowerShellConstants.RUNNER_LOG_ERR_TO_ERROR, "true");
    getBuildType().setOption(SBuildType.BT_FAIL_ON_ANY_ERROR_MESSAGE, true);

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_pass_explicit_exit_code_cmd(@NotNull final PowerShellBitness bitness) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, "2.0");
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "$res = \"Test is running\"\nWrite-Output $res\nexit(123)");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bitness.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
    Assert.assertTrue(getBuildLog(build).contains("Process exited with code 123"));
  }

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_pass_explicit_exit_code_file(@NotNull final PowerShellBitness bitness) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "$res = \"Test is running\"\nWrite-Output $res\nexit(123)");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bitness.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
    Assert.assertTrue(getBuildLog(build).contains("Process exited with code 123"));
  }

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_error_to_warning_on_false(@NotNull final PowerShellBitness bitness) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "$res = \"This should be warning\" \nWrite-Error $res");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bitness.getValue());

    setRunnerParameter(PowerShellConstants.RUNNER_LOG_ERR_TO_ERROR, "false");
    getBuildType().setOption(SBuildType.BT_FAIL_ON_ANY_ERROR_MESSAGE, true);

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
  }
}

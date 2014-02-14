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

package jetbrains.buildServer.powershell.agent;

import jetbrains.buildServer.powershell.common.*;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.TestFor;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ErrorHandlingTests extends AbstractPowerShellIntegrationTest {

  private final String code = "\n\n someNonExistentFileOnTheEdgeOfTheUniverse.exe param1 param2\n\n";

  @Test(dataProvider = "powerShellVersions")
  @TestFor(issues = "TW-21554")
  public void should_fail_nonzero_exit_code_CODE_STDIN(@NotNull final PowerShellVersion version) throws Throwable {
    setBuildConfigurationParameter(PowerShellConstants.CONFIG_POWERSHELL_USE_ERROR_DETECTION, "true");
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());

    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, code);
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, version.getVersion());
    assertCompatible(getBuildType());
    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "powerShellVersions")
  @TestFor(issues = "TW-21554")
  public void should_fail_nonzero_exit_code_CODE_PS1(@NotNull final PowerShellVersion version) throws Throwable {
    setBuildConfigurationParameter(PowerShellConstants.CONFIG_POWERSHELL_USE_ERROR_DETECTION, "true");
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());

    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, code);
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, version.getVersion());
    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "powerShellVersions")
  @TestFor(issues = "TW-21554")
  public void should_fail_nonzero_exit_code_FILE_STDIN(@NotNull final PowerShellVersion version) throws Throwable {
    setBuildConfigurationParameter(PowerShellConstants.CONFIG_POWERSHELL_USE_ERROR_DETECTION, "true");
    final File temp = createTempFile(code);
    final File script = FileUtil.renameFileNameOnly(temp, temp.getName() + ".ps1");
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());

    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_FILE, script.getCanonicalPath());
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, version.getVersion());
    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "powerShellVersions")
  @TestFor(issues = "TW-21554")
  public void should_fail_nonzero_exit_code_FILE_PS1(@NotNull final PowerShellVersion version) throws Throwable {
    setBuildConfigurationParameter(PowerShellConstants.CONFIG_POWERSHELL_USE_ERROR_DETECTION, "true");
    final File temp = createTempFile(code);
    final File script = FileUtil.renameFileNameOnly(temp, temp.getName() + ".ps1");
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());

    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_FILE, script.getCanonicalPath());
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, version.getVersion());
    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  @Test(dataProvider = "powerShellVersions")
  @TestFor(issues = "TW-21554")
  public void should_fail_incorrect_syntax_FILE_PS1(@NotNull final PowerShellVersion version) throws Throwable {
    setBuildConfigurationParameter(PowerShellConstants.CONFIG_POWERSHELL_USE_ERROR_DETECTION, "true");
    final File temp = createTempFile("param (\r\n[string]$PowerShellParam = \"value\",\r\n)\r\n");
    final File script = FileUtil.renameFileNameOnly(temp, temp.getName() + ".ps1");
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());

    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_FILE, script.getCanonicalPath());
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, version.getVersion());
    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isFailed());
  }

  /**
   * Simple powershell version provider for tests
   * that need to check pre- and post- 2.0 powershell capabilities
   * @return versions 1 and 2 of powershell
   */
  @DataProvider(name = "powerShellVersions")
  public Object[][] getVersions() {
    final Object[][] result = new Object[2][];
    result[0] = new Object[] {PowerShellVersion.V_1_0};
    result[1] = new Object[] {PowerShellVersion.V_2_0};
    return result;
  }



}

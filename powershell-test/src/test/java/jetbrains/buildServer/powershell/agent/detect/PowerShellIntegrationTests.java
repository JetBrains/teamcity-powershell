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
package jetbrains.buildServer.powershell.agent.detect;

import jetbrains.buildServer.RunnerTest2Base;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.TestFor;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

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
  public void should_run_simple_command_code_stdin() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @Test
  @TestFor(issues = "TW-29803")
  public void should_run_simple_command_file_stdin() throws Throwable {
    final File code = createTempFile("echo works");
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_FILE, code.getPath());
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @Test
  @TestFor(issues = "TW-29803")
  public void should_run_simple_command_code_ps1() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @Test
  @TestFor(issues = "TW-29803")
  public void should_run_simple_command_file_ps1() throws Throwable {
    final File dir = createTempDir();
    final File code = new File(dir, "code.ps1");
    FileUtil.writeFileAndReportErrors(code, "echo works");

    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_FILE, code.getPath());
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());

    Assert.assertTrue(getBuildLog(build).contains("works"));
  }


  @Test
  public void should_run_x86() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo \"ptr: $([IntPtr]::size)\"");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());

    Assert.assertTrue(getBuildLog(build).contains("ptr: 4 NO"));
  }

  @Test
  public void should_run_x64() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo \"ptr: $([IntPtr]::size)\"");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x64.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());

    Assert.assertTrue(getBuildLog(build).contains("ptr: 8 NO"));
  }
}

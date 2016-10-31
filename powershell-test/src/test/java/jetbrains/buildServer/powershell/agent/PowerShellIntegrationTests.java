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

import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.TestFor;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.*;

/**
 * Created 18.06.13 12:59
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class PowerShellIntegrationTests extends AbstractPowerShellIntegrationTest {

  @Test
  @TestFor(issues = "TW-29803")
  public void should_run_simple_command_code_stdin() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, "2.0");
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
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, "2.0");
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
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "Write-Host \"ptr: $([IntPtr]::size)\"\r\n");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());

    Assert.assertTrue(getBuildLog(build).contains("ptr: 4 NO"));
  }

  @Test
  public void should_run_x64() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "Write-Host \"ptr: $([IntPtr]::size)\"");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x64.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());

    Assert.assertTrue(getBuildLog(build).contains("ptr: 8 NO"));
  }

  @Test
  @TestFor(issues = "TW-39841")
  public void testShouldKeepGeneratedFiles_PowershellSpecific() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());
    setBuildConfigurationParameter(PowerShellConstants.CONFIG_KEEP_GENERATED, "");
    final SFinishedBuild build = doTest(null);
    assertEquals(1, getTempFiles().length);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @Test
  @TestFor(issues = "TW-39841")
  public void testShouldKeepGeneratedFiles_Global() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works\r\n");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());
    setBuildConfigurationParameter("teamcity.dont.delete.temp.files", "");
    final SFinishedBuild build = doTest(null);
    assertEquals(1, getTempFiles().length);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @Test
  @TestFor(issues = "TW-44082")
  public void testShouldWriteBOMinExternalFileMode() throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.x86.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "$var = \"Value is \u00f8\u00e5\u00e6\"\r\n Write-Output $var\r\n");
    setBuildConfigurationParameter(PowerShellConstants.CONFIG_KEEP_GENERATED, "");
    final SFinishedBuild build = doTest(null);
    assertEquals(1, getTempFiles().length);
    final File generatedScript = getTempFiles()[0];
    InputStreamReader reader = null;
    try {
       reader = new InputStreamReader(new FileInputStream(generatedScript), FILES_ENCODING);
       char[] buf = new char[1];
       assertEquals(1, reader.read(buf));
       assertEquals("BOM is not written to external file", '\ufeff', buf[0]);
    } catch (IOException e) {
      fail(e.getMessage());
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
    final String fileContents = FileUtil.readText(getTempFiles()[0], FILES_ENCODING);
    assertTrue("Non-ASCII symbols were not written to generated script", fileContents.contains("\u00f8\u00e5\u00e6"));
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
  }

  @NotNull
  private File[] getTempFiles() {
    File tempDir = new File(getCurrentTempDir(), "buildTmp");
    return FileUtil.listFiles(tempDir, new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith("powershell") && name.endsWith("ps1");
      }
    });
  }
}

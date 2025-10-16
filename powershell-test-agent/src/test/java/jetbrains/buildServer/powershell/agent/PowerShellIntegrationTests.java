package jetbrains.buildServer.powershell.agent;

import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.*;

/**
 * Created 18.06.13 12:59
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class PowerShellIntegrationTests extends AbstractPowerShellIntegrationTest {

  @Test(dataProvider = "supportedBitnessProvider")
  @TestFor(issues = "TW-29803")
  public void should_run_simple_command_code_stdin(@NotNull final PowerShellBitness bits) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, "2.0");
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @Test(dataProvider = "supportedBitnessProvider")
  @TestFor(issues = "TW-29803")
  public void should_run_simple_command_file_stdin(@NotNull final PowerShellBitness bits) throws Throwable {
    final File code = createTempFile("echo works");
    setRunnerParameter(PowerShellConstants.RUNNER_MIN_VERSION, "2.0");
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_FILE, code.getPath());
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @Test(dataProvider = "supportedBitnessProvider")
  @TestFor(issues = "TW-29803")
  public void should_run_simple_command_code_ps1(@NotNull final PowerShellBitness bits) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @Test(dataProvider = "supportedBitnessProvider")
  @TestFor(issues = "TW-29803")
  public void should_run_simple_command_file_ps1(@NotNull final PowerShellBitness bits) throws Throwable {
    final File dir = createTempDir();
    final File code = new File(dir, "code.ps1");
    FileUtil.writeFileAndReportErrors(code, "echo works");

    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_FILE, code.getPath());
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());

    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @Test(dataProvider = "supportedBitnessProvider")
  public void should_proper_bitness_run(@NotNull final PowerShellBitness bits) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "Write-Host \"ptr: $([IntPtr]::size)\"\r\n");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());

    final String output = bits == PowerShellBitness.x64 ? "8" : "4";
    Assert.assertTrue(getBuildLog(build).contains("ptr: " + output + " NO"));
  }

  @SuppressWarnings("TestMethodWithIncorrectSignature")
  @Test(dataProvider = "supportedBitnessProvider")
  @TestFor(issues = {"TW-39841", "TW-49772"})
  public void testShouldKeepGeneratedFiles_PowerShellSpecific(@NotNull final PowerShellBitness bits) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());

    setBuildConfigurationParameter(PowerShellConstants.CONFIG_KEEP_GENERATED, "true");
    final SFinishedBuild build = doTest(null);
    assertEquals(1, getTempFiles().length);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @SuppressWarnings("TestMethodWithIncorrectSignature")
  @Test(dataProvider = "supportedBitnessProvider")
  @TestFor(issues = {"TW-39841", "TW-49772"})
  public void testShouldKeepGeneratedFiles_Global(@NotNull final PowerShellBitness bits) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "echo works\r\n");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());

    setBuildConfigurationParameter("teamcity.dont.delete.temp.files", "true");
    final SFinishedBuild build = doTest(null);
    assertEquals(1, getTempFiles().length);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("works"));
  }

  @SuppressWarnings("TestMethodWithIncorrectSignature")
  @Test(dataProvider = "supportedBitnessProvider")
  @TestFor(issues = "TW-44082")
  public void testShouldWriteBOMinExternalFileMode(@NotNull final PowerShellBitness bits) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "$var = \"Value is \u00f8\u00e5\u00e6\"\r\n Write-Output $var\r\n");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());
    setBuildConfigurationParameter(PowerShellConstants.CONFIG_KEEP_GENERATED, "true");

    final SFinishedBuild build = doTest(null);
    assertEquals(1, getTempFiles().length);
    final File generatedScript = getTempFiles()[0];
    try (InputStreamReader reader = new InputStreamReader(new FileInputStream(generatedScript), FILES_ENCODING)) {
      char[] buf = new char[1];
      assertEquals(1, reader.read(buf));
      assertEquals("BOM is not written to external file", '\ufeff', buf[0]);
    } catch (IOException e) {
      fail(e.getMessage());
    }
    final String fileContents = FileUtil.readText(getTempFiles()[0], FILES_ENCODING);
    assertTrue("Non-ASCII symbols were not written to generated script", fileContents.contains("\u00f8\u00e5\u00e6"));
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
  }

  @SuppressWarnings("TestMethodWithIncorrectSignature")
  @Test(dataProvider = "supportedBitnessProvider")
  @TestFor(issues = "TW-34775")
  public void testOutputIsWrittenFromScriptInFile(@NotNull final PowerShellBitness bits) throws Throwable {
    final File dir = createTempDir();
    final File code = new File(dir, "code.ps1");
    FileUtil.writeFileAndReportErrors(code,
        "param ([string]$PowerShellParam = \"value\",)\n" +
            "Write-Host \"String from Write-Host\"\n" +
            "Write-Output \"String from Write-Output\"\n" +
            "Write-Host \"Function call from Write-Host $((Get-Date -Year 2000 -Month 12 -Day 31).DayOfYear)\"\n" +
            "Write-Output \"Function call from Write-Output $((Get-Date -Year 2000 -Month 12 -Day 31).DayOfYear)\"\n"
    );

    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_FILE, code.getPath());
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());

    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("String from Write-Host"));
    Assert.assertTrue(getBuildLog(build).contains("String from Write-Output"));
    Assert.assertTrue(getBuildLog(build).contains("Function call from Write-Host 366"));
    Assert.assertTrue(getBuildLog(build).contains("Function call from Write-Output 366"));
  }

  @SuppressWarnings("TestMethodWithIncorrectSignature")
  @Test(dataProvider = "supportedBitnessProvider")
  @TestFor(issues = "TW-65627")
  public void testMultilineArguments(@NotNull final PowerShellBitness bits) throws Throwable {
    setRunnerParameter(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.PS1.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.CODE.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_CODE, "param(\n" +
            "\t[string] $param1 = \"\"\n" +
            ")\n" +
            "\n" +
            "Write-Output \"praram1: $param1.\"");
    setRunnerParameter(PowerShellConstants.RUNNER_BITNESS, bits.getValue());
    setRunnerParameter(PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS, "-param1 \"line1a = line1b\nline2a = line2b\"");
    setBuildConfigurationParameter(PowerShellConstants.PARAM_ARGS_MULTILINE, "true");
    final SFinishedBuild build = doTest(null);
    dumpBuildLogLocally(build);
    Assert.assertTrue(build.getBuildStatus().isSuccessful());
    Assert.assertTrue(getBuildLog(build).contains("\"line1a = line1b line2a = line2b\""));
    Assert.assertTrue(getBuildLog(build).contains("praram1: line1a = line1b line2a = line2b."));
  }

  @NotNull
  private File[] getTempFiles() {
    File tempDir = new File(getCurrentTempDir(), "buildTmp");
    return FileUtil.listFiles(tempDir, (dir, name) -> name.startsWith("powershell") && name.endsWith("ps1"));
  }
}
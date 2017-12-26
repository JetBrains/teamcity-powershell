package jetbrains.buildServer.powershell.agent.service;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

import static jetbrains.buildServer.messages.DefaultMessagesInfo.createTextMessage;
import static jetbrains.buildServer.messages.DefaultMessagesInfo.internalize;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ProfileWriter {

  private static final Logger LOG = Logger.getInstance(ProfileWriter.class.getName());

  private static final String PROFILE_LOADER =
      "New-Item $Profile -ItemType File -Force\r\n"
              + "Add-Content -Path $Profile -Value 'if (Test-Path \"__PROFILE_PATH__\") {. \"__PROFILE_PATH__\"}' -Force\r\n";

  public void writeProfile(@NotNull final BuildProgressLogger buildLogger,
                           @NotNull final PowerShellInfo info,
                           @NotNull final File buildTempDirectory,
                           @NotNull final Map<String, String> systemProperties) {

    final String generatedProfile = generateProfileScriptContent(systemProperties);
    LOG.info("Generated profile: " + generatedProfile);
    buildLogger.message(generatedProfile);
    try {
      // todo: publish artifacts
      buildLogger.message("Writing profile");
      // create build profile
      File buildProfile = FileUtil.createTempFile(buildTempDirectory, "build_profile", ".ps1", true);
      // write profile to some file in build temp folder
      FileUtil.writeFileAndReportErrors(buildProfile, generatedProfile);
      // todo: add generated profile path to internal artifacts
      executeProfileLoader(buildLogger, info, buildTempDirectory, buildProfile);
    } catch (Exception e) {
      LOG.error(e);
    }

    // clean profile after the build? generate only for THIS build? so the profile could not be loaded by another build
    // clean will not be needed when the actual profile will be loaded from external file
  }

  private void executeProfileLoader(@NotNull final BuildProgressLogger buildLogger,
                                    @NotNull final PowerShellInfo info,
                                    @NotNull final File buildTempDirectory,
                                    @NotNull final File buildProfile) throws Exception {
    File profileLoader = FileUtil.createTempFile(buildTempDirectory, "profile_loader", ".ps1", true);
    final String content = PROFILE_LOADER.replace("__PROFILE_PATH__", buildProfile.getAbsolutePath());
    buildLogger.message(content);
    FileUtil.writeFileAndReportErrors(profileLoader, content);

    //execute profile loader
    final GeneralCommandLine line = new GeneralCommandLine();
    line.setExePath(info.getExecutablePath());
    line.addParameter("-ExecutionPolicy");
    line.addParameter("ByPass");
    line.addParameter("-File");
    line.addParameter(profileLoader.getAbsolutePath());
    buildLogger.logMessage(internalize(createTextMessage("Writing profile using command line: " + line.getCommandLineString())));

    final ExecResult r = SimpleCommandLineProcessRunner.runCommand(line, new byte[0]);
    if (LOG.isDebugEnabled()) {
      for (String outLine : r.getOutLines()) {
        LOG.info(outLine);
      }
    }
  }

  @NotNull
  private String generateProfileScriptContent(@NotNull Map<String, String> systemProperties) {
    StringBuilder builder = new StringBuilder();
    builder.append("$TEAMCITY = @{}\r\n");
    for (Map.Entry<String, String> prop: systemProperties.entrySet()) {
      builder.append("$TEAMCITY.Add(\"")
          .append(prop.getKey())
          .append("\", \"")
          .append(prop.getValue())
          .append("\")\r\n");
    }
    return builder.toString();
  }

}

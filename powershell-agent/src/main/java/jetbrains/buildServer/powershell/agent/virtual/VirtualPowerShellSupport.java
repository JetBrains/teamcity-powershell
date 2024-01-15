

package jetbrains.buildServer.powershell.agent.virtual;

import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static jetbrains.buildServer.messages.DefaultMessagesInfo.createTextMessage;
import static jetbrains.buildServer.messages.DefaultMessagesInfo.internalize;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.RUNNER_EDITION;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class VirtualPowerShellSupport {

  /**
   * Full path to PowerShell executable
   */
  private static final String PARAM_EXECUTABLE = "teamcity.powershell.virtual.executable";

  /**
   * Default executable: PowerShell.Core that is in PATH
   */
  private static final String EXECUTABLE_VALUE_DEFAULT = "pwsh";

  public PowerShellInfo getVirtualPowerShell(@NotNull final BuildRunnerContext context) {
    final PowerShellEdition edition = PowerShellEdition.fromString(context.getRunnerParameters().get(RUNNER_EDITION));
    String executable = context.getConfigParameters().get(PARAM_EXECUTABLE);
    if (isEmptyOrSpaces(executable)) {
      if (edition == null) {
        executable = EXECUTABLE_VALUE_DEFAULT; // edition is set to auto -> defaulting to CORE
      } else if (edition == PowerShellEdition.CORE) {
        executable = "pwsh";
      } else {
        executable = "powershell.exe";
      }
    } else {
      // executable specified
      context.getBuild().getBuildLogger().logMessage(internalize(createTextMessage(
              "Default PowerShell executable path inside container (" + EXECUTABLE_VALUE_DEFAULT + ") " +
                      "was overridden with " + executable)));
    }
    return new PowerShellInfo(
            PowerShellBitness.x64,
            new File("."),
            "-1",
            null,
            executable,
            true
    );
  }
}
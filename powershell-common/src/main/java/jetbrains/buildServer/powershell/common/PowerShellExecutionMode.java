

package jetbrains.buildServer.powershell.common;

import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 01.05.11 16:14
 */
public enum PowerShellExecutionMode {
  STDIN,
  PS1,
  ;

  @NotNull
  public String getValue() {
    return this.toString();
  }

  @Nullable
  public static PowerShellExecutionMode fromString(@Nullable String sMode) {
    if (StringUtil.isEmptyOrSpaces(sMode)) return STDIN;

    for (PowerShellExecutionMode mode : values()) {
      if (mode.getValue().equals(sMode)) {
        return mode;
      }
    }
    return null;
  }
}


package jetbrains.buildServer.powershell.common;

import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public enum PowerShellEdition {
  CORE("Core", "Core"),
  DESKTOP("Desktop", "Desktop");

  private final String myValue;
  private final String myDisplayName;

  PowerShellEdition(String value, String displayName) {
    myValue = value;
    myDisplayName = displayName;
  }

  public String getValue() {
    return myValue;
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  @Nullable
  public static PowerShellEdition fromString(@Nullable final String edition) {
    for (PowerShellEdition e: values()) {
      if (e.getValue().equals(edition)) {
        return e;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return myDisplayName;
  }
}
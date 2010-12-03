package jetbrains.buildServer.powershell.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 13:53
 */
public enum PowerShellVersion {
  V_1_0("1.0"),
  V_2_0("2.0")
  ;

  private final String myVersion;

  PowerShellVersion(final String version) {
    myVersion = version;
  }

  @NotNull
  public String getVersion() {
    return myVersion;
  }

  @Nullable
  public static PowerShellVersion fromString(@Nullable String version) {
    if (version == null) return null;

    version = version.trim();
    if (version.length() == 0) return null;

    for (PowerShellVersion ver : values()) {
      if (ver.getVersion().equals(version)) {
        return ver;
      }
    }
    return null;
  }
}

package jetbrains.buildServer.powershell.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 13:53
 */
public enum PowerShellVersion {
  V_1_0("1.0", 1),
  V_2_0("2.0", 2),
  V_3_0("3.0", 3),
  V_4_0("4.0", 4),
  ;

  private final String myVersion;
  private final int myOrder;

  PowerShellVersion(final String version, int order) {
    myVersion = version;
    myOrder = order;
  }

  @NotNull
  public String getVersion() {
    return myVersion;
  }

  @NotNull
  public String getVersionRegex() {
    return myVersion.replace(".", "\\.");
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

  @NotNull
  public static Collection<PowerShellVersion> getThisOrNewer(@NotNull final PowerShellVersion version) {
    final List<PowerShellVersion> result = new ArrayList<PowerShellVersion>();

    for (PowerShellVersion v : values()) {
      if (v.myOrder >= version.myOrder) result.add(v);
    }

    return result;
  }
}

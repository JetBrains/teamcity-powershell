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
package jetbrains.buildServer.powershell.common;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 13:53
 */
public enum PowerShellVersion {
  V_1_0("1.0", 1),
  V_2_0("2.0", 2),
  V_3_0("3.0", 3),
  V_4_0("4.0", 4),
  V_5_0("5.0", 5);

  /**
   * Regexp to match powershell version declaration.
   * Matches version format {@code Major.Minor.Build.Revision}
   * {@code Build} and {@code Revision} parts are optional (since Powershell v5)
   */
  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d\\.\\d)(\\.\\d+)?(\\.\\d+)?");

  /**
   * Suffix to extend short version with.
   * Matches optional {@code Build} and {@code Revision} parts
   */
  private static final String VERSION_EXTENSION_SUFFIX = "(.\\d+)?(.\\d+)?";

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
    return myVersion.replace(".", "\\.") + VERSION_EXTENSION_SUFFIX;
  }

  @Nullable
  public static PowerShellVersion fromString(@Nullable String version) {
    PowerShellVersion result = null;
    if (!StringUtil.isEmptyOrSpaces(version)) {
      version = version.trim();
      final Matcher m = VERSION_PATTERN.matcher(version);
      if (m.matches()) {
        String shortVersion = m.group(1);
        for (PowerShellVersion ver : values()) {
          if (ver.getVersion().equals(shortVersion)) {
            result = ver;
            break;
          }
        }
      }
    }
    return result;
  }

  @NotNull
  public static Collection<PowerShellVersion> getThisOrNewer(@NotNull final PowerShellVersion version) {
    final List<PowerShellVersion> result = new ArrayList<PowerShellVersion>();
    for (PowerShellVersion v: values()) {
      if (v.myOrder >= version.myOrder) {
        result.add(v);
      }
    }
    return result;
  }
}

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

import jetbrains.buildServer.util.Bitness;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 16:50
 */
public enum PowerShellBitness {
  x86 ("x86", "32 bit", Bitness.BIT32),
  x64 ("x64", "64 bit", Bitness.BIT64);

  private final String myValue;
  private final String myDisplayName;
  private final Bitness myBitness;

  PowerShellBitness(String value, String displayName, Bitness bitness) {
    myValue = value;
    myDisplayName = displayName;
    myBitness = bitness;
  }

  @NotNull
  public String getVersionKey() {
    return "powershell_" + myValue;
  }

  @NotNull
  public String getValue() {
    return myValue;
  }

  @NotNull
  public String getDescription() {
    return myDisplayName;
  }

  @NotNull
  public String getPathKey() {
    return getVersionKey() + "_Path";
  }

  @Nullable
  public static PowerShellBitness fromString(@Nullable String bit) {
    for (PowerShellBitness b : values()) {
      if (b.getValue().equals(bit)) {
        return b;
      }
    }
    return null;
  }

  @NotNull
  public Bitness toBitness() {
    return myBitness;
  }

  @Override
  public String toString() {
    return myDisplayName;
  }
}

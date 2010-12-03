/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
  x86,
  x64
  ;

  @NotNull
  public String getVersionKey() {
    return "powershell_" + this;
  }

  @NotNull
  public String getPathKey() {
    return getVersionKey() + "_Path";
  }

  @Nullable
  public static PowerShellBitness fromString(@Nullable String bit) {
    for (PowerShellBitness b : values()) {
      if (b.toString().equals(bit)) {
        return b;
      }
    }
    return null;
  }

  @NotNull
  public Bitness toBitness() {
    switch (this) {
      case x64: return Bitness.BIT64;
      case x86: return Bitness.BIT32;
      default: throw new IllegalArgumentException("Bitness: " + this);
    }
  }
}

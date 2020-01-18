/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

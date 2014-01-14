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

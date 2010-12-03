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

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 15:53
 */
public class PowerShellConstants {
  public static final String RUN_TYPE = "jetbrains.powershell";

  public static final String CONFIG_KEEP_GENERATED = "powershell.keep.generated";

  public static final String RUNNER_BITNESS = "jetbrains.powershell.bitness";
  public static final String RUNNER_CUSTOM_ARGUMENTS = "jetbrains.powershell.additionalArguments";
  public static final String RUNNER_SCRIPT_CODE = "jetbrains.powershell.script.code";
  public static final String RUNNER_SCRIPT_FILE = "jetbrains.powershell.script.file";
  public static final String RUNNER_SCRIPT_MODE = "jetbrains.powershell.script.mode";
}

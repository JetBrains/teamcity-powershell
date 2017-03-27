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

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 15:53
 */
public class PowerShellConstants {

  public static final String PLUGIN_NAME = "powershell-runner";

  public static final String RUN_TYPE = "jetbrains_powershell";

  public static final String CONFIG_KEEP_GENERATED = "powershell.keep.generated";

  public static final String RUNNER_BITNESS          = "jetbrains_powershell_bitness";
  public static final String RUNNER_EDITION          = "jetbrains_powershell_edition";
  public static final String RUNNER_CUSTOM_ARGUMENTS = "jetbrains_powershell_additionalArguments";
  public static final String RUNNER_SCRIPT_ARGUMENTS = "jetbrains_powershell_scriptArguments";
  public static final String RUNNER_LOG_ERR_TO_ERROR = "jetbrains_powershell_errorToError";
  public static final String RUNNER_MIN_VERSION      = "jetbrains_powershell_minVersion";
  public static final String RUNNER_NO_PROFILE       = "jetbrains_powershell_noprofile";

  public static final String RUNNER_SCRIPT_CODE = "jetbrains_powershell_script_code";
  public static final String RUNNER_SCRIPT_FILE = "jetbrains_powershell_script_file";
  public static final String RUNNER_SCRIPT_MODE = "jetbrains_powershell_script_mode";

  public static final String RUNNER_EXECUTION_MODE = "jetbrains_powershell_execution";

  /**
   * Any bitness of runner is enough to run the build
   */
  public static final String PARAM_VALUE_BITNESS_AUTO = "<Auto>";

  /**
   * Any edition of PowerShell is suitable
   */
  public static final String PARAM_VALUE_EDITION_ANY = "<Any>";
}

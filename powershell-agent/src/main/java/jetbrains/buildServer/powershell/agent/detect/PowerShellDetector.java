/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.powershell.agent.detect;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface PowerShellDetector {

  /**
   * Detects PowerShells installed on the agent
   *
   * @param detectionContext context of detection (agent parameters, etc.)
   * @return map of detected PowerShells in the format of {@code install_path -> PowerShellInfo}
   * @see PowerShellInfo
   */
  @NotNull
  Map<String, PowerShellInfo> findShells(@NotNull final DetectionContext detectionContext);

}

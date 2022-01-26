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

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class DetectionContextImpl implements DetectionContext {

  @NotNull
  private static final String PARAM_SEARCH_PATHS = "teamcity.powershell.detector.search.paths";

  @NotNull
  private final List<String> mySearchPaths;

  public DetectionContextImpl(@NotNull final BuildAgentConfiguration configuration) {
    mySearchPaths = loadSearchPaths(configuration.getConfigurationParameters().get(PARAM_SEARCH_PATHS));
  }

  private List<String> loadSearchPaths(@Nullable final String paramValue) {
    return StringUtil.isEmptyOrSpaces(paramValue) ? Collections.emptyList() : StringUtil.split(paramValue, ";");
  }

  @Override
  @NotNull
  public List<String> getSearchPaths() {
    return mySearchPaths;
  }
}

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
package jetbrains.buildServer.powershell.server;

import com.intellij.openapi.util.io.FileUtil;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.serverSide.BuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.discovery.BreadthFirstRunnerDiscoveryExtension;
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject;
import jetbrains.buildServer.util.browser.Browser;
import jetbrains.buildServer.util.browser.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class PowerShellRunnerDiscoverer extends BreadthFirstRunnerDiscoveryExtension {

  /** Default PowerShell script file extension */
  private static final String PS_EXT = "ps1";

  @NotNull
  @Override
  protected List<DiscoveredObject> discoverRunnersInDirectory(@NotNull final Element dir, @NotNull final List<Element> filesAndDirs) {
    final List<DiscoveredObject> runners = new ArrayList<DiscoveredObject>();
    for (Element e: filesAndDirs) {
      if (e.isLeaf() && PS_EXT.equals(FileUtil.getExtension(e.getName()))) {
        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PowerShellConstants.RUNNER_SCRIPT_FILE, e.getFullName());
        parameters.put(PowerShellConstants.RUNNER_BITNESS, PowerShellBitness.values()[0].getValue());
        parameters.put(PowerShellConstants.RUNNER_EXECUTION_MODE, PowerShellExecutionMode.STDIN.getValue());
        parameters.put(PowerShellConstants.RUNNER_SCRIPT_MODE, PowerShellScriptMode.FILE.getValue());
        runners.add(new DiscoveredObject(PowerShellConstants.RUN_TYPE, parameters));
      }
    }
    return runners;
  }

  @NotNull
  @Override
  protected List<DiscoveredObject> postProcessDiscoveredObjects(@NotNull BuildTypeSettings settings,
                                                                @NotNull Browser browser,
                                                                @NotNull List<DiscoveredObject> discovered) {
    if (discovered.isEmpty() || settings.getBuildRunners().isEmpty()) {
      return discovered;
    }
    final Set<String> alreadyUsedFiles = getAlreadyUsedFiles(settings);
    if (alreadyUsedFiles.isEmpty()) {
      return discovered;
    }
    final Iterator<DiscoveredObject> it = discovered.iterator();
    while (it.hasNext()) {
      DiscoveredObject o = it.next();
      if (alreadyUsedFiles.contains(o.getParameters().get(PowerShellConstants.RUNNER_SCRIPT_FILE))) {
        it.remove();
      }
    }
    return discovered;
  }

  private Set<String> getAlreadyUsedFiles(@NotNull final BuildTypeSettings settings) {
    final Set<String> result = new HashSet<String>();
    for (BuildRunnerDescriptor runner: settings.getBuildRunners()) {
      if (PowerShellConstants.RUN_TYPE.equals(runner.getType())) {
        final Map<String, String> params = runner.getParameters();
        if (PowerShellScriptMode.FILE.getValue().equals(params.get(PowerShellConstants.RUNNER_SCRIPT_MODE))) {
          result.add(runner.getParameters().get(PowerShellConstants.RUNNER_SCRIPT_FILE));
        }
      }
    }
    return result;
  }
}

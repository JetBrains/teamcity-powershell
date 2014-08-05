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

package jetbrains.buildServer.powershell.server.reports;

import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellExecutionMode;
import jetbrains.buildServer.powershell.common.PowerShellVersion;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.healthStatus.*;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Class {@code CommandModeHealthReport}
 *
 * Inspects PowerShell steps. Creates health issue, when step has
 * {@code -Command -} execution mode and uses PowerShell of version greater than 1.0
 *
 * Using {@code -Command -} mode can cause issues with remoting, sqlcmd, and does not always work with scripts
 * that have complicated formatting (stream gets corrupted).
 *
 * It is recommended to use external file as a container of PowerShell script.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CommandModeHealthReport extends HealthStatusReport {

  @NotNull
  private static final String CATEGORY_ID = "powershell";

  @NotNull
  private static final String CATEGORY_NAME = "PowerShell";

  @NotNull
  private final ItemCategory myCategory;

  public CommandModeHealthReport(@NotNull final PluginDescriptor pluginDescriptor,
                                 @NotNull final PagePlaces pagePlaces,
                                 @NotNull final WebLinks webLinks) {
    myCategory = new ItemCategory(CATEGORY_ID, CATEGORY_NAME, ItemSeverity.WARN, null, webLinks.getHelp("PowerShell", null));
    final HealthStatusItemPageExtension myPEx = new HealthStatusItemPageExtension(CATEGORY_ID, pagePlaces);
    myPEx.setIncludeUrl(pluginDescriptor.getPluginResourcesPath("commandModeHealthReport.jsp"));
    myPEx.setVisibleOutsideAdminArea(true);
    myPEx.register();
  }

  @NotNull
  @Override
  public String getType() {
    return CATEGORY_ID;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return CATEGORY_NAME;
  }

  @Override
  public boolean canReportItemsFor(@NotNull final HealthStatusScope scope) {
    return scope.isItemWithSeverityAccepted(myCategory.getSeverity());
  }

  @NotNull
  @Override
  public Collection<ItemCategory> getCategories() {
    return Collections.singletonList(myCategory);
  }

  @Override
  public void report(@NotNull final HealthStatusScope scope,
                     @NotNull final HealthStatusItemConsumer resultConsumer) {
    for (final SBuildType type: scope.getBuildTypes()) {
      final List<String> runnerIds = new ArrayList<String>();
      final List<SBuildRunnerDescriptor> steps = type.getBuildRunners();
      for (final SBuildRunnerDescriptor step: steps) {
        if (PowerShellConstants.RUN_TYPE.equals(step.getType())) {
          final Map<String, String> parameters = step.getParameters();
          final PowerShellExecutionMode mode = PowerShellExecutionMode.fromString(parameters.get(PowerShellConstants.RUNNER_EXECUTION_MODE));
          final PowerShellVersion version = PowerShellVersion.fromString(parameters.get(PowerShellConstants.RUNNER_MIN_VERSION));
          if (PowerShellExecutionMode.STDIN == mode && PowerShellVersion.V_1_0 != version) { // we accept explicit v 1.0 and command mode
            runnerIds.add(step.getId());
          }
        }
      }
      if (!runnerIds.isEmpty()) {
        final HashMap<String, Object> additionalData = new HashMap<String, Object>() {{
          put("steps", runnerIds);
          put("build_type", type);
        }};
        final HealthStatusItem item = new HealthStatusItem("powershell_command_mode_invalid_locks_" + type.getExternalId(), myCategory, additionalData);
        resultConsumer.consumeForBuildType(type, item);
      }
    }
  }
}

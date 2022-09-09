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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 23:24
 */
public class PowerShellInfoTest extends BaseTestCase {

  @Test(dataProvider = "editionProvider")
  public void testSaveLoad(@NotNull final PowerShellEdition edition) throws IOException {
    PowerShellInfo info = new PowerShellInfo(PowerShellBitness.x64, createTempDir(), "1.0", edition, "powershell.exe");

    final Mockery m = new Mockery();
    final BuildAgentConfiguration conf = m.mock(BuildAgentConfiguration.class);

    final Map<String, String> confParams = new HashMap<>();
    m.checking(new Expectations(){{
      allowing(conf).getConfigurationParameters(); will(returnValue(Collections.unmodifiableMap(confParams)));
    }});

    final PowerShellEdition e = info.getEdition();
    assertNotNull(e);
    final String propertyName = "powershell_" + e.getValue() + "_" + info.getVersion() + "_" + info.getBitness().getValue();
    info.saveInfo(confParams);
    assertEquals(info.getVersion(), confParams.get(propertyName));
    assertEquals(info.getHome().getAbsolutePath(), confParams.get(propertyName + "_Path"));
  }

  @DataProvider(name = "editionProvider")
  public Object[][] editionProvider() {
    Object[][] result = new Object[2][];
    result[0] = new Object[] {PowerShellEdition.CORE};
    result[1] = new Object[] {PowerShellEdition.DESKTOP};
    return result;
  }
}

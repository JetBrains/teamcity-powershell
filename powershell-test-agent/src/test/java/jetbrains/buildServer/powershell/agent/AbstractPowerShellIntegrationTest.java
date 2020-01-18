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

package jetbrains.buildServer.powershell.agent;

import com.jetbrains.launcher.SystemInfo;
import jetbrains.buildServer.RunnerTest2Base;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

/**
 * Base class for PowerShell integration tests
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public abstract class AbstractPowerShellIntegrationTest extends RunnerTest2Base {

  /**
   * Provides list of supported architectures
   *
   * @return x86 is supported only on windows, on other platforms only x64 is used
   */
  private PowerShellBitness[] getSupportedBitness() {
    if (SystemInfo.isWindows) {
      return PowerShellBitness.values();
    } else {
      return new PowerShellBitness[] { PowerShellBitness.x64 };
    }
  }
  
  @BeforeMethod
  @Override
  protected void setUp1() throws Throwable {
    super.setUp1();
    setPartialMessagesCheckerEx();
  }

  @NotNull
  @Override
  protected String getRunnerType() {
    return PowerShellConstants.RUN_TYPE;
  }

  @Override
  protected String getTestDataSuffixPath() {
    return "";
  }

  @DataProvider(name = "supportedBitnessProvider")
  public Object[][] getSupportedBitnessValues() {
    final PowerShellBitness[] supported = getSupportedBitness();
    final Object[][] result = new Object[supported.length][];
    for (int i = 0; i < supported.length; i++) {
      result[i] = new Object[] {supported[i]};
    }
    return result;
  }
}

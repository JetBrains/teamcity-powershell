/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
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

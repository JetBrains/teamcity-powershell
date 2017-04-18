/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.detect;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import org.hamcrest.Description;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 23:24
 */
public class PowerShellInfoTest extends BaseTestCase {

  @Test(dataProvider = "editionProvider")
  public void testSaveLoad(@NotNull final PowerShellEdition edition) throws IOException {
    PowerShellInfo info = new PowerShellInfo(PowerShellBitness.x64, createTempDir(), "1.0", edition);

    final Mockery m = new Mockery();
    final BuildAgentConfiguration conf = m.mock(BuildAgentConfiguration.class);

    final Map<String, String> confParams = new HashMap<String, String>();
    m.checking(new Expectations(){{
      allowing(conf).getConfigurationParameters(); will(returnValue(Collections.unmodifiableMap(confParams)));
      allowing(conf).addConfigurationParameter(with(any(String.class)), with(any(String.class)));
      will(new Action() {
        public Object invoke(final Invocation invocation) throws Throwable {
          final String key = (String) invocation.getParameter(0);
          final String value = (String) invocation.getParameter(1);
          Assert.assertNotNull(key);
          Assert.assertNotNull(value);
          confParams.put(key, value);
          return null;
        }

        public void describeTo(final Description description) {
          description.appendText("add Parameters");
        }
      });
    }});

    info.saveInfo(conf);

    PowerShellInfo i = PowerShellInfo.loadInfo(conf, PowerShellBitness.x64);

    Assert.assertNotNull(i);
    Assert.assertEquals(i.getBitness(), info.getBitness());
    Assert.assertEquals(i.getHome(), info.getHome());
    Assert.assertEquals(i.getExecutablePath(), info.getExecutablePath());
    Assert.assertEquals(i.getVersion(), info.getVersion());
    Assert.assertEquals(i.getEdition(), info.getEdition());
  }

  @DataProvider(name = "editionProvider")
  public Object[][] editionProvider() {
    Object[][] result = new Object[2][];
    result[0] = new Object[] {PowerShellEdition.CORE};
    result[1] = new Object[] {PowerShellEdition.DESKTOP};
    return result;
  }
}

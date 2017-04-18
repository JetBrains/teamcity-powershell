/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.detect;

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 14:06
 */
public class PowerShellInfo {

  @NotNull
  private final PowerShellBitness myBitness;

  @NotNull
  private final File myHome;

  @NotNull
  private final String myVersion;

  @NotNull
  private final PowerShellEdition myEdition;

  public PowerShellInfo(@NotNull final PowerShellBitness bitness,
                        @NotNull final File home,
                        @NotNull final String version,
                        @NotNull final PowerShellEdition edition) {
    myBitness = bitness;
    myHome = home;
    myVersion = version;
    myEdition = edition;
  }

  @NotNull
  public PowerShellBitness getBitness() {
    return myBitness;
  }

  @NotNull
  public File getHome() {
    return myHome;
  }

  @NotNull
  public String getVersion() {
    return myVersion;
  }

  @Override
  public String toString() {
    return "PowerShell " + myEdition + " Edition v" + myVersion + " " + myBitness + "(" + getHome() + ")";
  }

  @NotNull
  public PowerShellEdition getEdition() {
    return myEdition;
  }

  @Nullable
  public static PowerShellInfo loadInfo(@NotNull final BuildAgentConfiguration config,
                                        @Nullable final PowerShellBitness bitness) {
    if (bitness == null) {
      return null;
    }

    final Map<String, String> ps = config.getConfigurationParameters();
    final String ver = ps.get(bitness.getVersionKey());
    final String path = ps.get(bitness.getPathKey());
    final PowerShellEdition edition = PowerShellEdition.fromString(ps.get(bitness.getEditionKey()));

    if (path != null && ver != null && edition != null) {
      return new PowerShellInfo(bitness, new File(path), ver, edition);
    }
    return null;
  }

  public void saveInfo(@NotNull final BuildAgentConfiguration config) {
    config.addConfigurationParameter(myBitness.getVersionKey(), myVersion);
    config.addConfigurationParameter(myBitness.getPathKey(), myHome.toString());
    config.addConfigurationParameter(myBitness.getEditionKey(), myEdition.getValue());
  }

  @NotNull
  public String getExecutablePath() {
    return FileUtil.getCanonicalFile(new File(myHome, "powershell" + (SystemInfo.isWindows ? ".exe" : ""))).getPath();
  }
}

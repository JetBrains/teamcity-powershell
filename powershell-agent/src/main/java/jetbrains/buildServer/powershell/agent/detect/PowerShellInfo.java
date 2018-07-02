/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.detect;

import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static jetbrains.buildServer.powershell.common.PowerShellConstants.PATH_SUFFIX;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.generateFullKey;

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

  @Nullable
  private final PowerShellEdition myEdition;

  @NotNull
  private final String myExecutable;

  private final boolean myVirtual;
  
  public PowerShellInfo(@NotNull final PowerShellBitness bitness,
                        @NotNull final File home,
                        @NotNull final String version,
                        @Nullable final PowerShellEdition edition,
                        @NotNull final String executable) {
    this(bitness, home, version, edition, executable, false);
  }

  public PowerShellInfo(@NotNull final PowerShellBitness bitness,
                        @NotNull final File home,
                        @NotNull final String version,
                        @Nullable final PowerShellEdition edition,
                        @NotNull final String executable,
                        boolean isVirtual) {
    myBitness = bitness;
    myHome = home;
    myVersion = version;
    myEdition = edition;
    myExecutable = executable;
    myVirtual = isVirtual;
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
    return (myVirtual ? "(virtual) " : "") + "PowerShell "
        + myEdition + " Edition v"
        + myVersion + " "
        + myBitness
        + "(" + (myVirtual ? "" : ( getHome() + "/")) + myExecutable + ")";
  }

  @NotNull
  public String getExecutable() {
    return myExecutable;
  }

  @Nullable
  public PowerShellEdition getEdition() {
    return myEdition;
  }

  public void saveInfo(@NotNull final BuildAgentConfiguration config) {
    final String key = generateFullKey(myEdition, myBitness, myVersion);
    if (!myVirtual) {
      config.addConfigurationParameter(key, myVersion);
      config.addConfigurationParameter(key + PATH_SUFFIX, myHome.toString());
    }
  }

  @NotNull
  public String getExecutablePath() {
    if (myVirtual) {
      return myExecutable;
    } else {
      return FileUtil.getCanonicalFile(new File(myHome, myExecutable)).getPath();
    }
  }

  public boolean isVirtual() {
    return myVirtual;
  }
}

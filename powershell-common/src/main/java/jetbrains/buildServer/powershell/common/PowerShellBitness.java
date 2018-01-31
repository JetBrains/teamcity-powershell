/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.common;

import jetbrains.buildServer.util.Bitness;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 16:50
 */
public enum PowerShellBitness {
  x86 ("x86", "32 bit", Bitness.BIT32),
  x64 ("x64", "64 bit", Bitness.BIT64);

  private final String myValue;
  private final String myDisplayName;
  private final Bitness myBitness;

  PowerShellBitness(String value, String displayName, Bitness bitness) {
    myValue = value;
    myDisplayName = displayName;
    myBitness = bitness;
  }

  @NotNull
  public String getVersionKey() {
    return "powershell_" + myValue;
  }

  @NotNull
  public String getValue() {
    return myValue;
  }

  @NotNull
  public String getDisplayName() {
    return myDisplayName;
  }

  @NotNull
  public String getPathKey() {
    return getVersionKey() + "_Path";
  }

  public String getExecutableKey() {
    return getVersionKey() + "_Executable";
  }

  public String getEditionKey() {
    return getVersionKey() + "_Edition";
  }

  @Nullable
  public static PowerShellBitness fromString(@Nullable String bit) {
    for (PowerShellBitness b : values()) {
      if (b.getValue().equals(bit)) {
        return b;
      }
    }
    return null;
  }

  @NotNull
  public Bitness toBitness() {
    return myBitness;
  }

  @Override
  public String toString() {
    return myDisplayName;
  }
}

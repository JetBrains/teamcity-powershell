/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.system;

import com.intellij.openapi.util.SystemInfo;

/**
 * Wrapper for SystemInfo constants access.
 * Provided for ease of testing
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SystemBitness {
  
  public boolean is32bit() {
    return SystemInfo.is32Bit;
  }

  public boolean is64bit() {
    return SystemInfo.is64Bit;
  }
}

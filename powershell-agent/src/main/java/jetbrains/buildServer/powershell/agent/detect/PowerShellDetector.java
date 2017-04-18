/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.detect;

import jetbrains.buildServer.powershell.common.PowerShellBitness;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface PowerShellDetector {

  @NotNull
  Map<PowerShellBitness, PowerShellInfo> findPowerShells(@NotNull final DetectionContext detectionContext);
  
}

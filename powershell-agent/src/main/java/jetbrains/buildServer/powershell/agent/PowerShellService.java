/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.BuildServiceAdapter;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellScriptMode;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static jetbrains.buildServer.powershell.common.PowerShellBitness.fromString;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.*;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 16:47
 */
public class PowerShellService extends BuildServiceAdapter {
  private final PowerShellInfoProvider myProvider;
  private File myFileToRemove = null;

  public PowerShellService(final PowerShellInfoProvider provider) {
    myProvider = provider;
  }

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    return createProgramCommandline(
            selectTool().getExecutablePath(),
            getArguments()
    );
  }

  private List<String> getArguments() throws RunBuildException {
    List<String> args = new ArrayList<String>();

    args.add("-File");
    args.add(getOrCreateScriptFile().getPath());

    final String custom = getRunnerParameters().get(RUNNER_CUSTOM_ARGUMENTS);
    if (!StringUtil.isEmptyOrSpaces(custom)) {
      for (String _line : custom.split("[\\r\\n]+")) {
        String line = _line.trim();
        if (StringUtil.isEmptyOrSpaces(line)) continue;
        args.addAll(StringUtil.splitHonorQuotes(line));
      }
    }

    return args;
  }

  @Override
  public void afterProcessFinished() throws RunBuildException {
    super.afterProcessFinished();

    if (myFileToRemove != null && !getConfigParameters().containsKey(CONFIG_KEEP_GENERATED)) {
      FileUtil.delete(myFileToRemove);
      myFileToRemove = null;
    }
  }

  private File getOrCreateScriptFile() throws RunBuildException {
    PowerShellScriptMode mode = PowerShellScriptMode.fromString(getRunnerParameters().get(RUNNER_SCRIPT_MODE));
    if (mode == null) {
      throw new RunBuildException("PowerShell script mode was not defined.");
    }

    //TODO: copy this file to ensure '.ps1' estension
    if (mode == PowerShellScriptMode.FILE) {
      return FileUtil.resolvePath(getCheckoutDirectory(), getRunnerParameters().get(RUNNER_SCRIPT_FILE));
    }

    if (mode != PowerShellScriptMode.CODE) {
      throw new IllegalArgumentException("Unknown powershell mode: " + mode);
    }

    Closeable handle = null;
    try {
      String text = getRunnerParameters().get(RUNNER_SCRIPT_CODE);
      if (StringUtil.isEmptyOrSpaces(text)) {
        throw new RunBuildException("Emptry build script");
      }
      text = StringUtil.convertLineSeparators(text, "\r\n");

      final File code = FileUtil.createTempFile(getBuildTempDirectory(), "powershell", ".ps1", true);
      OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(code), "utf-8");
      handle = w;
      w.write(text);

      return code;
    } catch (IOException e) {
      throw new RunBuildException("Failed to generate temporary file at " + getBuildTempDirectory(), e);
    } finally {
      FileUtil.close(handle);
    }
  }

  private PowerShellInfo selectTool() throws RunBuildException {
    final PowerShellBitness bit = fromString(getRunnerParameters().get(RUNNER_BITNESS));
    if (bit == null) throw new RunBuildException("Failed to read: " + RUNNER_BITNESS);

    for (PowerShellInfo info : myProvider.getPowerShells()) {
      if (info.getBitness() == bit) {
        return info;
      }
    }

    throw new RunBuildException("PowerShell " + bit + " was not found");
  }
}

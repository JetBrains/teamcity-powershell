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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.*;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

import static jetbrains.buildServer.powershell.common.PowerShellBitness.fromString;
import static jetbrains.buildServer.powershell.common.PowerShellConstants.*;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 16:47
 */
public class PowerShellService extends BuildServiceAdapter {
  private static final Logger LOG = Logger.getInstance(PowerShellService.class.getName());

  private final PowerShellInfoProvider myProvider;
  private final Collection<File> myFilesToRemove = new ArrayList<File>();

  public PowerShellService(final PowerShellInfoProvider provider) {
    myProvider = provider;
  }

  @NotNull
  @Override
  public List<ProcessListener> getListeners() {
    final boolean logToError = !StringUtil.isEmptyOrSpaces(getRunnerParameters().get(PowerShellConstants.RUNNER_LOG_ERR_TO_ERROR));
    final BuildProgressLogger logger = getLogger();
    return Arrays.<ProcessListener>asList(new ProcessListenerAdapter(){
      private final org.apache.log4j.Logger OUT_LOG = org.apache.log4j.Logger.getLogger("teamcity.out");
      @Override
      public void onStandardOutput(@NotNull final String text) {
        logger.message(text);
        OUT_LOG.info(text);
      }

      @Override
      public void onErrorOutput(@NotNull final String text) {
        if (logToError) {
          logger.error(text);
        } else {
          logger.warning(text);
        }
        OUT_LOG.warn(text);
      }
    });
  }

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    final PowerShellInfo info = selectTool();
    return new SimpleProgramCommandLine(
            getActualEnvironmentVariables(info),
            getWorkingDirectory().getPath(),
            selectCmd(info),
            getCmdArguments(info)
    );
  }

  private boolean isInternalPropertySetExecutionPolicy(@NotNull final String name, boolean def) {
    final String prop = getConfigParameters().get("teamcity.powershell." + name);
    if (StringUtil.isEmptyOrSpaces(prop)) return def;

    return "true".equalsIgnoreCase(prop);
  }

  @NotNull
  private Map<String, String> getActualEnvironmentVariables(@NotNull PowerShellInfo info) {
    Map<String, String> map = getEnvironmentVariables();
    //check internal property
    if (!isInternalPropertySetExecutionPolicy("set.executionPolicyEnv", info.getVersion() == PowerShellVersion.V_1_0)) return map;

    final String env = "PSExecutionPolicyPreference";

    //check if user had overridden the value
    if (map.containsKey(env)) {
      LOG.info(env + " environment variable was specified explicitly");
      return map;
    }

    map = new TreeMap<String, String>(getEnvironmentVariables());
    map.put(env, "ByPass");
    return map;
  }

  private void addExecutionPolicyPreference(@NotNull final List<String> list, @NotNull final PowerShellInfo info) {
    if (!isInternalPropertySetExecutionPolicy("set.executionPolicyArg", info.getVersion() != PowerShellVersion.V_1_0)) return;

    final String cmdArg = "-ExecutionPolicy";
    for (String arg : list) {
      if (arg.trim().toLowerCase().contains(cmdArg.toLowerCase())) {
        LOG.info(cmdArg  + " was specified explicitly");
        return;
      }
    }

    list.add(cmdArg);
    list.add("ByPass");
  }

  private List<String> getCmdArguments(@NotNull final PowerShellInfo info) throws RunBuildException {
    List<String> list = new ArrayList<String>();

    list.add("/c");
    list.add(info.getExecutablePath());
    if (!StringUtil.isEmptyOrSpaces(getRunnerParameters().get(RUNNER_NO_PROFILE))) {
      list.add("-NoProfile");
    }
    list.add("-NonInteractive");

    addCustomArguments(list, RUNNER_CUSTOM_ARGUMENTS);
    addExecutionPolicyPreference(list, info);

    PowerShellExecutionMode mod = PowerShellExecutionMode.fromString(getRunnerParameters().get(RUNNER_EXECUTION_MODE));
    if (mod == null) {
      throw new RunBuildException("'" + RUNNER_EXECUTION_MODE + "' runner parameters is not defined");
    }

    final File script = getOrCreateScriptFile();
    switch (mod) {
      case STDIN:
        list.add("-Command");
        list.add("-");
        list.add("<");
        list.add(script.getPath());
        break;
      case PS1:
        if (!script.getPath().toLowerCase().endsWith(".ps1")) {
          throw new RunBuildException("PowerShell script should have '.ps1' extension");
        }
        list.add("-File");
        list.add(script.getPath());
        addCustomArguments(list, PowerShellConstants.RUNNER_SCRIPT_ARGUMENTS);
        break;
      default:
        throw new RunBuildException("Unknown ExecutionMode: " + mod);
    }

    return list;
  }

  private void addCustomArguments(@NotNull final List<String> args,
                                  @NotNull final String runnerParametersKey) {
    final String custom = getRunnerParameters().get(runnerParametersKey);
    if (!StringUtil.isEmptyOrSpaces(custom)) {
      for (String _line : custom.split("[\\r\\n]+")) {
        String line = _line.trim();
        if (StringUtil.isEmptyOrSpaces(line)) continue;
        args.addAll(StringUtil.splitHonorQuotes(line));
      }
    }
  }

  @Override
  public void afterProcessFinished() throws RunBuildException {
    super.afterProcessFinished();

    if (getConfigParameters().containsKey(CONFIG_KEEP_GENERATED)) return;

    for (File file : myFilesToRemove) {
      FileUtil.delete(file);
    }
    myFilesToRemove.clear();
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
        throw new RunBuildException("Empty build script");
      }
      //some newlines are necessary to workaround -Command - issues, like TW-19771
      text = "  \r\n  \r\n  \r\n" + StringUtil.convertLineSeparators(text, "\r\n") + "\r\n  \r\n   \r\n   ";

      final File code = FileUtil.createTempFile(getBuildTempDirectory(), "powershell", ".ps1", true);
      myFilesToRemove.add(code);
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

  @NotNull
  private String selectCmd(PowerShellInfo info) {
    final String windir = getEnvironmentVariables().get("windir");
    if (StringUtil.isEmptyOrSpaces(windir)) {
      LOG.warn("Failed to find %windir%");
      return "cmd.exe";
    }

    switch (info.getBitness()) {
      case x64:
        if (SystemInfo.is32Bit) {
          return windir + "\\sysnative\\cmd.exe";
        }
        return windir + "\\system32\\cmd.exe";
      case x86:
        if (SystemInfo.is64Bit) {
          return windir + "\\syswow64\\cmd.exe";
        }
        return windir + "\\system32\\cmd.exe";
    }
    return "cmd.exe";
  }
}

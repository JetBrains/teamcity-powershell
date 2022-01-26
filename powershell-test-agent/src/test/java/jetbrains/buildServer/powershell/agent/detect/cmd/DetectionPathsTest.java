/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.powershell.agent.detect.cmd;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.agent.detect.DetectionContext;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DetectionPathsTest extends BaseTestCase {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testPredefinedPathsChildren() throws Exception {
    final File root = createTempDir();
    final File child = new File(root, "child");
    child.mkdir();
    final DetectionContext context = createContext(root.getAbsolutePath());
    List<String> paths = new DetectionPaths().getPaths(context);
    assertTrue(paths.size() >= 2);
    assertEquals(root.getAbsolutePath(), paths.get(0));
    assertEquals(child.getAbsolutePath(), paths.get(1));
  }

  private DetectionContext createContext(final String... paths) {
    return () -> Arrays.asList(paths);
  }
}

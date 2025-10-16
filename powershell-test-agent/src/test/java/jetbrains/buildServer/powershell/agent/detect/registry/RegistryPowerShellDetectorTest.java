package jetbrains.buildServer.powershell.agent.detect.registry;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.util.Bitness;
import jetbrains.buildServer.util.Win32RegistryAccessor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static jetbrains.buildServer.util.Bitness.BIT32;
import static jetbrains.buildServer.util.Bitness.BIT64;
import static jetbrains.buildServer.util.Win32RegistryAccessor.Hive.LOCAL_MACHINE;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 14:57
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class RegistryPowerShellDetectorTest extends BaseTestCase {

  private Mockery m;

  private Win32RegistryAccessor acc;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    m.setThreadingPolicy(new Synchroniser());
    acc = m.mock(Win32RegistryAccessor.class);
  }

  @Test
  public void should_return_empty_result_when_not_installed() {
    // arrange
    givenNoDesktopEdition();
    givenNoCoreEdition();

    // act
    Map<String, PowerShellInfo> shells = new RegistryPowerShellDetector(acc).findShells();

    // assert
    assertEquals(0, shells.size());
  }

  @Test
  public void should_find_powershell_desktop_edition_1() throws IOException {
    // arrange
    final File home = createTempDir();
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install", null);
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install", "1");

    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion", null);
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion", "1.0");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase", null);
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase", home.getPath());

    givenNoDesktopEditionForBit64();
    givenNoCoreEdition();

    // act
    Map<String, PowerShellInfo> shells = new RegistryPowerShellDetector(acc).findShells();

    // assert
    assertEquals(1, shells.size());
    Map.Entry<String, PowerShellInfo> shell = shells.entrySet().stream().findFirst().get();
    assertEquals("1.0", shell.getValue().getVersion());
    assertEquals(home, shell.getValue().getHome());
  }

  @Test
  public void should_find_powershell_desktop_edition_3() throws IOException {
    // arrange
    final File home = createTempDir();
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install", "1");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install", null);

    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion", "5.0");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase", home.getPath());

    givenNoDesktopEditionForBit64();
    givenNoCoreEdition();

    // act
    Map<String, PowerShellInfo> shells = new RegistryPowerShellDetector(acc).findShells();

    // assert
    assertEquals(1, shells.size());
    Map.Entry<String, PowerShellInfo> shell = shells.entrySet().stream().findFirst().get();
    assertEquals("5.0", shell.getValue().getVersion());
    assertEquals(home, shell.getValue().getHome());
  }

  @Test
  public void should_find_powershell_desktop_editions_for_32_and_64_bits() throws IOException {
    // arrange
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install", "1");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install", null);

    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion", "3.0");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase", createTempDir().getPath());

    given(BIT64, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion", "5.0");
    given(BIT64, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase", createTempDir().getPath());

    givenNoCoreEdition();

    // act, assert
    assertEquals(2, new RegistryPowerShellDetector(acc).findShells().size());
  }

  @Test
  public void should_prefer_powershell_desktop_edition_3_over_1() throws IOException {
    // arrange
    final File homeV1 = createTempDir();
    final File homeV3 = createTempDir();
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install", "1");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install", "1");

    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion", "5.0");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion", "1.0");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase", homeV3.getPath());
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase", homeV1.getPath());

    givenNoDesktopEditionForBit64();
    givenNoCoreEdition();

    // act
    Map<String, PowerShellInfo> shells = new RegistryPowerShellDetector(acc).findShells();

    // assert
    assertEquals(1, shells.size());
    Map.Entry<String, PowerShellInfo> shell = shells.entrySet().stream().findFirst().get();
    assertEquals("5.0", shell.getValue().getVersion());
    assertEquals(homeV3, shell.getValue().getHome());
  }

  @Test
  public void should_check_powershell_home_exists_for_desktop_edition() {
    // arrange
    final File home = new File("fake");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install", "1");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install", null);

    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion", "2.0");
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase", home.getPath());

    givenNoDesktopEditionForBit64();
    givenNoCoreEdition();

    // act, assert
    assertEquals(0, new RegistryPowerShellDetector(acc).findShells().size());
  }

  @Test
  public void should_find_powershell_core_edition() throws IOException {
    // arrange
    final File home = createTempDir();
    assertTrue(new File(home, "pwsh.exe").createNewFile());
    String key = UUID.randomUUID().toString();
    m.checking(new Expectations(){{
      allowing(acc).listSubKeys(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShellCore\\InstalledVersions");
      will(returnValue(new HashSet<>(singletonList(key))));

      allowing(acc).listSubKeys(LOCAL_MACHINE, BIT64, "SOFTWARE\\Microsoft\\PowerShellCore\\InstalledVersions");
      will(returnValue(Collections.emptySet()));
    }});

    given(BIT32, "SOFTWARE\\Microsoft\\PowerShellCore\\InstalledVersions\\" + key, "InstallLocation", home.getPath());
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShellCore\\InstalledVersions\\" + key, "SemanticVersion", "7.5.3");

    givenNoDesktopEdition();

    // act
    Map<String, PowerShellInfo> shells = new RegistryPowerShellDetector(acc).findShells();

    // assert
    assertEquals(1, shells.size());
    Map.Entry<String, PowerShellInfo> shell = shells.entrySet().stream().findFirst().get();
    assertEquals("7.5.3", shell.getValue().getVersion());
    assertEquals(home, shell.getValue().getHome());
  }

  private void given(Bitness bitness, String path, String key, Object value) {
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, bitness, path, key); will(returnValue(value));
    }});
  }

  private void givenNoDesktopEdition() {
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\3", "Install", null);
    given(BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install", null);
  }

  private void givenNoDesktopEditionForBit64() {
    given(BIT64, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "PowerShellVersion", null);
    given(BIT64, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion", null);
    given(BIT64, "SOFTWARE\\Microsoft\\PowerShell\\3\\PowerShellEngine", "ApplicationBase", null);
    given(BIT64, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase", null);
  }

  private void givenNoCoreEdition() {
    m.checking(new Expectations() {{
      allowing(acc).listSubKeys(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShellCore\\InstalledVersions");
      will(returnValue(Collections.emptySet()));

      allowing(acc).listSubKeys(LOCAL_MACHINE, BIT64, "SOFTWARE\\Microsoft\\PowerShellCore\\InstalledVersions");
      will(returnValue(Collections.emptySet()));
    }});
  }
}


package jetbrains.buildServer.powershell.agent;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import org.testng.annotations.DataProvider;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public abstract class BasePowerShellUnitTest extends BaseTestCase {

  @DataProvider(name = "editionProvider")
  public Object[][] editionProvider() {
    Object[][] result = new Object[2][];
    result[0] = new Object[] {PowerShellEdition.CORE};
    result[1] = new Object[] {PowerShellEdition.DESKTOP};
    return result;
  }
}
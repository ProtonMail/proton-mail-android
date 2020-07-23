package ch.protonmail.android.uitests.tests.manageaccounts

import androidx.test.filters.LargeTest
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.menu.MenuRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUserWith2FA
import ch.protonmail.android.uitests.testsHelper.TestData.twoPassUser
import ch.protonmail.android.uitests.testsHelper.TestData.twoPassUserWith2FA
import org.junit.Test

@LargeTest
class MultiuserManagementTests : BaseTest() {

    private val loginRobot = LoginRobot()


    @Test
    fun connectOnePassAccount() {
        loginRobot.loginTwoPasswordUser(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectOnePassAccount(onePassUser)
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(onePassUser.email, 0) }
    }

    @Test
    fun connectTwoPassAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(twoPassUser.email, 0) }
    }

    @Test
    fun connectTwoPassAccountWithTwoFa() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccountWithTwoFa(twoPassUserWith2FA)
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(twoPassUserWith2FA.email, 0) }
    }

    @Test
    fun connectOnePassAccountWithTwoFa() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectOnePassAccountWithTwoFa(onePassUserWith2FA)
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(onePassUserWith2FA.email, 0) }
    }

    @Test
    fun removeAllAccounts() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .removeAllAccounts()
            .verify {
                loginScreenDisplayed()
                //  removingAccountsToastShown()
            }
    }

    @Test
    fun logoutPrimaryAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .logoutAccount(0)
            .menuDrawer()
            .accountsList()
            .verify { accountLoggedOut(twoPassUser.name, 1) }
    }

    @Test
    fun logoutSecondaryAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .logoutAccount(1)
            .menuDrawer()
            .accountsList()
            .verify { accountLoggedOut(onePassUser.name, 1) }
    }

    @Test
    fun logoutOnlyRemainingAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .logoutOnlyRemainingAccount()
            .verify { loginScreenDisplayed() }
    }

    @Test
    fun removePrimaryAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .removeAccount(0)
            .menuDrawer()
            .accountsList()
            .verify { accountRemoved(twoPassUser.name, twoPassUser.email) }
    }

    @Test
    fun removeSecondaryAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .removeAccount(1)
            .menuDrawer()
            .accountsList()
            .verify { accountRemoved(onePassUser.name, onePassUser.email) }
    }

    @Test
    fun removeOnlyRemainingAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .removeOnlyRemainingAccount()
            .verify { loginScreenDisplayed() }
    }

    @Test
    fun logoutOneRemoveAnotherAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .logoutAccount(0)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .removeOnlyRemainingAccount()
            .verify { loginScreenDisplayed() }
    }

    @Test
    fun cancelLoginOnTwoFaPrompt() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .cancelLoginOnTwoFaPrompt(onePassUserWith2FA)
            .menuDrawer()
            .accountsList()
            .verify { accountLoggedOut(onePassUserWith2FA.name, 1) }
    }

    @Test
    fun addTwoFreeAccounts() {
        loginRobot.loginTwoPasswordUserWithTwoFa(twoPassUserWith2FA)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectSecondFreeOnePassAccountWithTwoFa(onePassUserWith2FA)
            .verify { limitReachedDialogDisplayed() }
    }

    @Test
    fun switchAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .switchToAccount(1)
            .verify { switchedToAccount(onePassUser.name) }
    }

}
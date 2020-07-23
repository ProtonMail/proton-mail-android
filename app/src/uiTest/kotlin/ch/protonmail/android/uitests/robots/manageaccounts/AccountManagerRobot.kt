package ch.protonmail.android.uitests.robots.manageaccounts

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [AccountManagerRobot] class contains actions and verifications for Account Manager functionality.
 */
open class AccountManagerRobot {

    fun addAccount(): ConnectAccountRobot {
        UIActions.id.clickViewWithId(R.id.addNewUserAccount)
        return ConnectAccountRobot()
    }

    fun logoutAccount(accountPosition: Int): InboxRobot {
        return accountMoreMenu(accountPosition)
            .logout()
            .confirm()
    }

    fun logoutOnlyRemainingAccount(): LoginRobot {
        accountMoreMenu(0)
            .logout()
            .confirmToLoginScreen()
        return LoginRobot()
    }

    fun removeAccount(accountPosition: Int): InboxRobot {
        return accountMoreMenu(accountPosition)
            .remove()
            .confirm()
    }

    fun removeOnlyRemainingAccount(): LoginRobot {
        accountMoreMenu(0)
            .remove()
            .confirmToLoginScreen()
        return LoginRobot()
    }

    fun removeAllAccounts(): LoginRobot {
        return moreOptions()
            .removeAll()
    }

    /**
     *  Account at [accountPosition] 0 is the current primary account, first in the list.
     */
    private fun accountMoreMenu(accountPosition: Int): AccountManagerRobot {
        UIActions.recyclerView.clickOnObjectWithIdInRecyclerViewRow(accountsRecyclerViewId,
            R.id.accUserMoreMenu, accountPosition)
        return AccountManagerRobot()
    }

    private fun moreOptions(): AccountManagerRobot {
        UIActions.system.clickMoreOptionsButton()
        return AccountManagerRobot()
    }

    private fun logout(): AccountManagerRobot {
        UIActions.text.clickViewWithText(R.string.logout)
        return AccountManagerRobot()
    }

    private fun remove(): AccountManagerRobot {
        UIActions.text.clickViewWithText(R.string.account_manager_remove)
        return AccountManagerRobot()
    }

    private fun removeAll(): LoginRobot {
        UIActions.text.clickViewWithText(R.string.account_manager_remove_all)
        return LoginRobot()
    }

    private fun confirm(): InboxRobot {
        UIActions.system.clickPositiveDialogButton()
        return InboxRobot()
    }

    private fun confirmToLoginScreen(): LoginRobot {
        UIActions.system.clickPositiveDialogButton()
        return LoginRobot()
    }

    /**
     * Contains all the validations that can be performed by [AccountManagerRobot].
     */
    inner class Verify : AccountManagerRobot() {

        fun manageAccountsOpened(): AccountManagerRobot {
            UIActions.check.viewWithIdIsDisplayed(accountsRecyclerViewId)
            return AccountManagerRobot()
        }

        fun accountAddedInAccountManager(emailAddress: String, position: Int) {
            UIActions.check.viewWithIdInRecyclerViewMatchesText(accountsRecyclerViewId, position,
                accUserEmailAddressId, emailAddress)
        }

        fun accountLoggedOutInAccountManager(username: String, position: Int) {
            val userLoggedOut = stringFromResource(R.string.manage_accounts_user_loggedout)
                .replace("%s", username)
            UIActions.check.viewWithIdInRecyclerViewMatchesText(accountsRecyclerViewId, position,
                accUsernameId, userLoggedOut)
        }

        fun accountRemovedFromAccountManager(username: String, emailAddress: String) {
            val userRemoved = stringFromResource(R.string.manage_accounts_user_loggedout)
                .replace("%s", username)
            UIActions.check.viewWithIdAndTextDoesNotExist(accUserEmailAddressId, emailAddress)
            UIActions.check.viewWithIdAndTextDoesNotExist(accUsernameId, userRemoved)
        }

    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as AccountManagerRobot

    companion object {
        const val accountsRecyclerViewId = R.id.accountsRecyclerView
        const val accUserEmailAddressId = R.id.accUserEmailAddress
        const val accUsernameId = R.id.accUserName
    }
}
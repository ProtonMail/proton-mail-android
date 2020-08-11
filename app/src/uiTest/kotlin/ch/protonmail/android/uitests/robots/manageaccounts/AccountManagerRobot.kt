package ch.protonmail.android.uitests.robots.manageaccounts

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.manageaccounts.ManageAccountsMatchers.withPrimaryAccountInAccountManager
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

    fun logoutAccount(email: String): InboxRobot {
        return accountMoreMenu(email)
            .logout()
            .confirm()
    }

    fun logoutLastAccount(email: String): LoginRobot {
        return accountMoreMenu(email)
            .logout()
            .confirmLastAccountLogout()
    }

    fun removeAccount(email: String): InboxRobot {
        return accountMoreMenu(email)
            .remove()
            .confirm()
    }

    fun removeAllAccounts(): LoginRobot {
        return moreOptions()
            .removeAll()
    }

    private fun accountMoreMenu(email: String): AccountManagerRobot {
        UIActions.recyclerView.clickAccountManagerViewItem(
            accountsRecyclerViewId,
            email,
            R.id.accUserMoreMenu
        )
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
        UIActions.system.clickPositiveButtonInDialogRoot()
        return InboxRobot()
    }

    private fun confirmLastAccountLogout(): LoginRobot {
        UIActions.system.clickPositiveButtonInDialogRoot()
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

        fun switchedToAccount(username: String) {
            UIActions
                .recyclerView
                .scrollToRecyclerViewMatchedItem(
                    accountsRecyclerViewId,
                    withPrimaryAccountInAccountManager(
                        stringFromResource(
                            R.string.manage_accounts_user_primary,
                            username
                        )
                    )
                )
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as AccountManagerRobot

    companion object {
        const val accountsRecyclerViewId = R.id.accountsRecyclerView
    }
}

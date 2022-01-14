package ch.protonmail.android.uitests.robots.manageaccounts

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.manageaccounts.ManageAccountsMatchers.withViewAtPosition
import me.proton.core.test.android.instrumented.Robot

/**
 * [AccountPanelRobot] class contains actions and verifications for Account Manager functionality.
 */
open class AccountPanelRobot : Robot {

    fun addAccount(): LoginMailRobot {
        view.withId(R.id.account_action_textview).click()
        return LoginMailRobot()
    }

    fun logoutAccount(email: String): InboxRobot {
        return accountMoreMenu(email)
            .logout()
//            .confirm()
    }

    fun logoutSecondaryAccount(email: String): AccountPanelRobot {
        return accountMoreMenu(email)
            .logoutSecondaryAccount()
//            .confirm()
    }

    fun logoutLastAccount(email: String): LoginMailRobot {
        return accountMoreMenu(email)
            .logoutLastAccount()
//            .confirmLastAccountLogout()
    }

    fun removeAccount(email: String): AccountPanelRobot {
        return accountMoreMenu(email)
            .remove()
//            .confirm()
    }

    fun removeSecondaryAccount(email: String): InboxRobot {
        return accountMoreMenu(email)
            .removeSecondaryAccount()
//            .confirm()
    }

    fun removeLastAccount(email: String): LoginMailRobot {
        return accountMoreMenu(email)
            .removeLastAccount()
//        .confirmLastAccountLogout()
    }

    fun switchToAccount(accountPosition: Int): InboxRobot {
        recyclerView
            .withId(accountsRecyclerViewId)
            .onItemAtPosition(accountPosition)
            .click()
        return InboxRobot()
    }

    private fun logout(): InboxRobot {
        onView(withText("Sign out")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return InboxRobot()
    }

    private fun logoutSecondaryAccount(): AccountPanelRobot {
        onView(withText("Sign out")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return AccountPanelRobot()
    }

    private fun logoutLastAccount(): LoginMailRobot {
        onView(withText("Sign out")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return LoginMailRobot()
    }

    private fun remove(): AccountPanelRobot {
        onView(withText("Remove")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return AccountPanelRobot()
    }

    private fun removeSecondaryAccount(): InboxRobot {
        onView(withText("Remove")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return InboxRobot()
    }

    private fun removeLastAccount(): LoginMailRobot {
        onView(withText("Remove")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return LoginMailRobot()
    }

    private fun accountMoreMenu(email: String): AccountPanelRobot {
        view.withId(R.id.account_more_button).hasSibling(view.withText(email)).click()
        return AccountPanelRobot()
    }

    /**
     * Contains all the validations that can be performed by [AccountPanelRobot].
     */
    inner class Verify : AccountPanelRobot() {

        fun accountsListOpened(): AccountPanelRobot {
            view.withId(accountsRecyclerViewId).checkDisplayed()
            return AccountPanelRobot()
        }

        fun accountAdded(email: String) {
            view.withId(R.id.account_email_textview).withText(email).isEnabled().checkDisplayed()
        }

        fun accountLoggedOut(email: String) {
            view.withId(R.id.account_name_textview).withText(email).isDisabled().checkDisplayed()
        }

        fun accountRemoved(username: String) {
            view.withId(R.id.account_name_textview).withText(username).checkDoesNotExist()
        }

        fun switchedToAccount(username: String) {
            val accountFirstPosition = 0
            onView(withId(accountsRecyclerViewId))
                .check(matches(withViewAtPosition(accountFirstPosition, hasDescendant(withText(username)))))
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as AccountPanelRobot

    companion object {

        const val accountsRecyclerViewId = R.id.account_list_recyclerview
    }
}

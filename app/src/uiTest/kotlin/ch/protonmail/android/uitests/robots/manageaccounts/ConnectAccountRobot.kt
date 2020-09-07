/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.uitests.robots.manageaccounts

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.User
import ch.protonmail.android.uitests.testsHelper.type

/**
 * [ConnectAccountRobot] class contains actions and verifications for Connect Account functionality.
 */
open class ConnectAccountRobot {

    fun connectOnePassAccount(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signIn()
    }

    fun connectOnePassAccountWithTwoFa(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFa()
            .twoFaCode(user.twoFaCode)
            .confirmTwoFa()
    }

    fun connectTwoPassAccount(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFa()
            .mailboxPassword(user.mailboxPassword)
            .signIn()
    }

    fun connectTwoPassAccountWithTwoFa(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFa()
            .twoFaCode(user.twoFaCode)
            .confirmTwoFaAndProvideMailboxPassword()
            .mailboxPassword(user.mailboxPassword)
            .signIn()
    }

    fun connectSecondFreeOnePassAccountWithTwoFa(user: User): ConnectAccountRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFa()
            .twoFaCode(user.twoFaCode)
            .confirmTwoFAAndLimitReached()
    }

    fun cancelLoginOnTwoFaPrompt(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFa()
            .twoFaCode(user.twoFaCode)
            .cancelTwoFaPrompt()
            .navigateBack()
    }

    private fun signIn(): InboxRobot {
        UIActions.text.clickViewWithText(R.string.sign_in)
        return InboxRobot()
    }

    private fun confirmTwoFa(): InboxRobot {
        UIActions.system.clickPositiveDialogButton()
        return InboxRobot()
    }

    private fun cancelTwoFaPrompt(): ConnectAccountRobot {
        UIActions.system.clickNegativeDialogButton()
        return ConnectAccountRobot()
    }

    private fun confirmTwoFAAndLimitReached(): ConnectAccountRobot {
        UIActions.system.clickPositiveDialogButton()
        return ConnectAccountRobot()
    }

    private fun navigateBack(): InboxRobot {
        UIActions.system.clickHamburgerOrUpButton()
        return InboxRobot()
    }

    private fun username(username: String): ConnectAccountRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.username, username)
        return this
    }

    private fun password(password: String): ConnectAccountRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.password, password)
        return this
    }

    private fun mailboxPassword(mailboxPassword: String): ConnectAccountRobot {
        UIActions.wait.forViewWithId(R.id.mailboxPassword).type(mailboxPassword)
        return this
    }

    private fun twoFaCode(twoFaCode: String): ConnectAccountRobot {
        UIActions.wait.forViewWithId(R.id.two_factor_code)
        UIActions.id.insertTextIntoFieldWithId(R.id.two_factor_code, twoFaCode)
        return this
    }

    private fun confirmTwoFaAndProvideMailboxPassword(): ConnectAccountRobot {
        UIActions.system.clickPositiveDialogButton()
        return this
    }

    private fun signInWithMailboxPasswordOrTwoFa(): ConnectAccountRobot {
        UIActions.text.clickViewWithText(R.string.sign_in)
        return this
    }

    /**
     * Contains all the validations that can be performed by [ConnectAccountRobot].
     */
    inner class Verify : ConnectAccountRobot() {

        fun limitReachedDialogDisplayed() {
            UIActions.check.alertDialogWithTextIsDisplayed(R.string.connect_account_limit_title)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as ConnectAccountRobot
}

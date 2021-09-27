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
package ch.protonmail.android.uitests.robots.login

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.uitests.testsHelper.User
import ch.protonmail.android.uitests.testsHelper.uiactions.click
import ch.protonmail.android.uitests.testsHelper.uiactions.insert

/**
 * [LoginRobot] class contains actions and verifications for login functionality.
 */
class LoginRobot {

    fun loginUser(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signIn()
            .refreshMessageList()
    }

    fun loginUserWithTwoFa(user: User): TwoFaRobot {
        return username(user.name)
            .password(user.password)
            .signInWithTwoFa()
    }

    fun loginTwoPasswordUser(user: User): MailboxPasswordRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPassword()
    }

    private fun signIn(): InboxRobot {
        UIActions.allOf.clickViewWithIdAndText(signInButtonId, R.string.sign_in)
        return InboxRobot()
    }

    private fun signInWithMailboxPassword(): MailboxPasswordRobot {
        signIn()
        return MailboxPasswordRobot()
    }

    private fun signInWithTwoFa(): TwoFaRobot {
        signIn()
        return TwoFaRobot()
    }

    private fun username(name: String): LoginRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.username, name)
        return this
    }

    private fun password(password: String): LoginRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.password, password)
        return this
    }

    /**
     * Class represents Message Password dialog.
     */
    inner class TwoFaRobot {

        fun provideTwoFaCode(twoFaCode: String): InboxRobot {
            return twoFaCode(twoFaCode)
                .confirm2Fa()
        }

        fun provideTwoFaCodeMailbox(twoFaCode: String): MailboxPasswordRobot {
            return twoFaCode(twoFaCode)
                .confirm2FaMailbox()
        }

        private fun confirm2Fa(): InboxRobot {
            UIActions.wait.forViewWithId(android.R.id.button1).click()
            return InboxRobot()
        }

        private fun confirm2FaMailbox(): MailboxPasswordRobot {
            UIActions.wait.untilViewWithIdEnabled(android.R.id.button1).click()
            return MailboxPasswordRobot()
        }

        private fun twoFaCode(twoFaCode: String): TwoFaRobot {
            UIActions.wait.forViewWithId(twoFactorEditTextId).insert(twoFaCode)
            return this
        }
    }

    /**
     * Contains all the validations that can be performed by [LoginRobot].
     */
    class Verify {

        fun loginScreenDisplayed(): LoginRobot {
            UIActions.wait.forViewWithId(signInButtonId)
            return LoginRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {
        const val signInButtonId = R.id.sign_in
        const val twoFactorEditTextId = R.id.two_factor_code
    }
}

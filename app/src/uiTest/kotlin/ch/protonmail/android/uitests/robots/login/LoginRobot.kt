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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobot
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.User

/**
 * [LoginRobot] class contains actions and verifications for login functionality.
 */
open class LoginRobot : UIActions() {

    fun loginUser(user: User): MailboxRobot {
        return username(user.name)
            .password(user.password)
            .signIn()
    }

    fun loginUserWithTwoFA(user: User): MailboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFA()
            .twoFACode(user.twoFASecurityKey)
            .confirm2FA()
    }

    fun loginTwoPasswordUser(user: User): MailboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFA()
            .mailboxPassword(user.mailboxPassword)
            .decrypt()
    }

    fun loginTwoPasswordUserWithTwoFA(user: User): MailboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFA()
            .twoFACode(user.twoFASecurityKey)
            .confirm2FAAndProvideMailboxPassword()
            .mailboxPassword(user.mailboxPassword)
            .decrypt()
    }

    private fun username(name: String): LoginRobot {
        insertTextIntoFieldWithId(R.id.username, name)
        return this
    }

    private fun password(password: String?): LoginRobot {
        insertTextIntoFieldWithId(R.id.password, password)
        return this
    }

    private fun signIn(): MailboxRobot {
        clickOnObjectWithIdAndText(R.id.sign_in, R.string.sign_in)
        return MailboxRobot()
    }

    private fun signInWithMailboxPasswordOrTwoFA(): LoginRobot {
        clickOnObjectWithIdAndText(R.id.sign_in, R.string.sign_in)
        return this
    }

    private fun mailboxPassword(password: String?): LoginRobot {
        waitUntilObjectWithIdAppearsInView(R.id.mailbox_password).insertText(password)
        return this
    }

    private fun decrypt(): MailboxRobot {
        clickOnObjectWithIdAndText(R.id.sign_in, R.string.decrypt)
        return MailboxRobot()
    }

    private fun confirm2FA(): MailboxRobot {
        clickOnObjectWithId(android.R.id.button1)
        return MailboxRobot()
    }

    private fun confirm2FAAndProvideMailboxPassword(): LoginRobot {
        clickOnObjectWithId(android.R.id.button1)
        return this
    }

    private fun twoFACode(twoFACode: String?): LoginRobot {
        waitUntilObjectWithIdAppearsInView(R.id.two_factor_code)
        onView(withId(R.id.two_factor_code)).insertText(twoFACode)
        return this
    }

    private fun secondPass(mailboxPassword: String): LoginRobot {
        waitUntilObjectWithIdAppearsInView(R.id.mailbox_password).insertText(mailboxPassword)
        return this
    }

    private fun confirmSecondPass(): LoginRobot {
        clickOnObjectWithIdAndText(R.id.sign_in, R.string.decrypt)
        return this
    }

    /**
     * Contains all the validations that can be performed by [LoginRobot].
     */
    class Verify : ComposerRobot() {

        fun loginSuccessful(): LoginRobot {
            waitUntilObjectWithIdAppearsInView(R.id.compose)
            return LoginRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as ComposerRobot
}

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
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.User
import ch.protonmail.android.uitests.testsHelper.insert

/**
 * [LoginRobot] class contains actions and verifications for login functionality.
 */
class LoginRobot {

    fun loginUser(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signIn()
    }

    fun loginUserWithTwoFa(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFa()
            .twoFaCode(user.twoFaCode)
            .confirm2Fa()
    }

    fun loginTwoPasswordUser(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFa()
            .mailboxPassword(user.mailboxPassword)
            .decrypt()
    }

    fun loginTwoPasswordUserWithTwoFa(user: User): InboxRobot {
        return username(user.name)
            .password(user.password)
            .signInWithMailboxPasswordOrTwoFa()
            .twoFaCode(user.twoFaCode)
            .confirm2FaAndProvideMailboxPassword()
            .mailboxPassword(user.mailboxPassword)
            .decrypt()
    }

    private fun username(name: String): LoginRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.username, name)
        return this
    }

    private fun password(password: String): LoginRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.password, password)
        return this
    }

    private fun signIn(): InboxRobot {
        UIActions.allOf.clickViewWithIdAndText(signInButtonId, R.string.sign_in)
        return InboxRobot()
    }

    private fun signInWithMailboxPasswordOrTwoFa(): LoginRobot {
        signIn()
        return this
    }

    private fun mailboxPassword(password: String): LoginRobot {
        UIActions.wait.forViewWithId(R.id.mailbox_password).insert(password)
        return this
    }

    private fun decrypt(): InboxRobot {
        UIActions.allOf.clickViewWithIdAndText(signInButtonId, R.string.decrypt)
        return InboxRobot()
    }

    private fun confirm2Fa(): InboxRobot {
        UIActions.system.clickPositiveDialogButton()
        return InboxRobot()
    }

    private fun confirm2FaAndProvideMailboxPassword(): LoginRobot {
        UIActions.system.clickPositiveDialogButton()
        return this
    }

    private fun twoFaCode(twoFaCode: String): LoginRobot {
        UIActions.wait.forViewWithId(R.id.two_factor_code)
        onView(withId(R.id.two_factor_code)).insert(twoFaCode)
        return this
    }

    private fun secondPass(mailboxPassword: String): LoginRobot {
        UIActions.wait.forViewWithId(R.id.mailbox_password).insert(mailboxPassword)
        return this
    }

    private fun confirmSecondPass(): LoginRobot {
        UIActions.allOf.clickViewWithIdAndText(signInButtonId, R.string.decrypt)
        return this
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
    }
}

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
package ch.protonmail.android.uitests.actions

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import ch.protonmail.android.uitests.results.LoginResult
import ch.protonmail.android.uitests.testsHelper.TestUser
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [LoginRobot] class contains actions and verifications for login functionality.
 */
class LoginRobot : UIActions() {

    fun loginUser(user: TestUser) {
        username(user.name)
            .password(user.password)
            .signIn()
    }

    fun loginTwoPasswordUser(user: TestUser) {
        username(user.name)
            .password(user.password)
            .signIn()
            .mailboxPassword(user.mailboxPassword)
            .decrypt()
    }

    fun username(name: String): LoginRobot {
        insertTextIntoFieldWithId(R.id.username, name)
        return this
    }

    fun password(password: String?): LoginRobot {
        insertTextIntoFieldWithId(R.id.password, password)
        return this
    }

    fun signIn(): LoginRobot {
        clickOnObjectWithIdAndText(R.id.sign_in, R.string.sign_in)
        return this
    }

    fun mailboxPassword(password: String?): LoginRobot {
        waitUntilObjectWithIdAppearsInView(R.id.mailbox_password).insertText(password)
        return this
    }

    fun decrypt() {
        clickOnObjectWithIdAndText(R.id.sign_in, R.string.decrypt)
    }

    fun confirm2FA(): LoginRobot {
        clickOnObjectWithId(android.R.id.button1)
        return this
    }

    fun twoFACode(twoFACode: String?): LoginRobot {
        waitUntilObjectWithIdAppearsInView(R.id.two_factor_code)
        onView(withId(R.id.two_factor_code)).insertText(twoFACode)
        return this
    }

    fun secondPass(mailboxPassword: String): LoginRobot {
        waitUntilObjectWithIdAppearsInView(R.id.mailbox_password).insertText(mailboxPassword)
        return this
    }

    fun confirmSecondPass(): LoginResult {
        clickOnObjectWithIdAndText(R.id.sign_in, R.string.decrypt)
        return LoginResult()
    }
}

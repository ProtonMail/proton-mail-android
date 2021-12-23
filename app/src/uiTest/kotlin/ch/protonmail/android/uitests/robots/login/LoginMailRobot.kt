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

import android.widget.EditText
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.tests.BaseTest.Companion.users
import me.proton.core.test.android.instrumented.Robot
import me.proton.core.test.android.robots.auth.login.LoginRobot
import me.proton.core.test.android.robots.auth.login.MailboxPasswordRobot
import me.proton.core.test.android.robots.auth.login.TwoFaRobot

class LoginMailRobot : Robot {

    val loginRobot = LoginRobot()

    fun loginOnePassUser(): InboxRobot {
        val onePassUser = users.getUser { it.name == "onePassUser" }
        view.withId(R.id.sign_in).click()
        loginRobot
            .username(onePassUser.name)
            .password(onePassUser.password)
            .signIn<InboxRobot>()
        return InboxRobot()
    }

    fun loginTwoPassUser(): InboxRobot {
        val twoPassUser = users.getUser { it.name == "twoPassUser" }
        view.withId(R.id.sign_in).click()
        loginRobot
            .username(twoPassUser.name)
            .password(twoPassUser.password)
            .signIn<MailboxPasswordRobot>()
            .unlockMailbox<InboxRobot>(twoPassUser)
        return InboxRobot()
    }

    fun loginOnePassUserWith2FA(): InboxRobot {
        val onePassUserWith2FA = users.getUser { it.name == "onePassUserWith2FA" }
        view.withId(R.id.sign_in).click()
        loginRobot
            .username(onePassUserWith2FA.name)
            .password(onePassUserWith2FA.password)
            .signIn<TwoFaRobot>()
            .setSecondFactorInput(onePassUserWith2FA.twoFa)
            .authenticate<InboxRobot>()
        return InboxRobot()
    }

    fun loginTwoPassUserWith2FA(): InboxRobot {
        val twoPassUserWith2FA = users.getUser { it.name == "twoPassUserWith2FA" }
        view.withId(R.id.sign_in).click()
        loginRobot
            .username(twoPassUserWith2FA.name)
            .password(twoPassUserWith2FA.password)
            .signIn<TwoFaRobot>()
            .setSecondFactorInput(twoPassUserWith2FA.twoFa)
            .authenticate<MailboxPasswordRobot>()
            .unlockMailbox<InboxRobot>(twoPassUserWith2FA)
        return InboxRobot()
    }

    fun loginAutoAttachPublicKeyUser(): InboxRobot {
        val autoAttachPublicKey = users.getUser { it.name == "autoAttachPublicKey" }
        view.withId(R.id.sign_in).click()
        loginRobot
            .username(autoAttachPublicKey.name)
            .password(autoAttachPublicKey.password)
            .signIn<InboxRobot>()
        return InboxRobot()
    }

    fun addFreeAccount(): LoginMailRobot {
        val onePassUserWith2FA = users.getUser { it.name == "onePassUserWith2FA" }
        loginRobot
            .username(onePassUserWith2FA.name)
            .password(onePassUserWith2FA.password)
            .signIn<TwoFaRobot>()
            .setSecondFactorInput(onePassUserWith2FA.twoFa)
            .authenticate<InboxRobot>()
        return LoginMailRobot()
    }

    fun cancelLoginOnTwoFaPrompt(): InboxRobot {
        val onePassUserWith2FA = users.getUser { it.name == "onePassUserWith2FA" }
        loginRobot
            .username(onePassUserWith2FA.name)
            .password(onePassUserWith2FA.password)
            .signIn<TwoFaRobot>()
            .switchTwoFactorMode()
//            .clickCancelButton()
        /**
         * TODO Create cancel method in [TwoFaRobot]
         **/
        view.withId(R.id.closeButton).click()
        return InboxRobot()
    }

    fun addOnePassUser(): InboxRobot {
        val onePassUser = users.getUser { it.name == "onePassUser" }
        loginRobot
            .username(onePassUser.name)
            .password(onePassUser.password)
            .signIn<InboxRobot>()
        return InboxRobot()
    }

    fun addTwoPassUser(): InboxRobot {
        val twoPassUser = users.getUser { it.name == "twoPassUser" }
        loginRobot
            .username(twoPassUser.name)
            .password(twoPassUser.password)
            .signIn<MailboxPasswordRobot>()
            .unlockMailbox<InboxRobot>(twoPassUser)
        return InboxRobot()
    }

    fun addOnePassUserWith2FA(): InboxRobot {
        val onePassUserWith2FA = users.getUser { it.name == "onePassUserWith2FA" }
        loginRobot
            .username(onePassUserWith2FA.name)
            .password(onePassUserWith2FA.password)
            .signIn<TwoFaRobot>()
            .setSecondFactorInput(onePassUserWith2FA.twoFa)
            .authenticate<InboxRobot>()
        return InboxRobot()
    }

    fun addTwoPassUserWith2FA(): InboxRobot {
        val twoPassUserWith2FA = users.getUser { it.name == "twoPassUserWith2FA" }
        loginRobot
            .username(twoPassUserWith2FA.name)
            .password(twoPassUserWith2FA.password)
            .signIn<TwoFaRobot>()
            .setSecondFactorInput(twoPassUserWith2FA.twoFa)
            .authenticate<MailboxPasswordRobot>()
            .unlockMailbox<InboxRobot>(twoPassUserWith2FA)
        return InboxRobot()
    }

    class Verify : Robot {

        //    TODO move verification methods to LoginRobot class
        fun loginScreenDisplayed() {
            view.withId(R.id.usernameInput).instanceOf(EditText::class.java).checkDisplayed()
        }

        fun limitReachedToastDisplayed() {
            view.withId(R.id.snackbar_text).withText("Only one free Proton account is allowed.").checkDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}

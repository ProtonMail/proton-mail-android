/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.uitests.testsHelper

import ch.protonmail.android.uitests.tests.BaseTest

object TestUser {

    private const val emailStub = "email_stub"
    private const val nameStub = "name_stub"
    private const val pwdStub = "pwd_stub"
    private const val mailPwdStub = "mail_pwd_stub"
    private const val twoFAStub = "2fa_stub"

    /** Test Users **/
    var onePassUser =
        User(
            email = emailStub, name = nameStub, password = pwdStub, mailboxPassword = mailPwdStub,
            twoFASecurityKey = twoFAStub
        )
    var twoPassUser =
        User(
            email = emailStub, name = nameStub, password = pwdStub, mailboxPassword = mailPwdStub,
            twoFASecurityKey = twoFAStub
        )
    var onePassUserWith2FA =
        User(
            email = emailStub, name = nameStub, password = pwdStub, mailboxPassword = mailPwdStub,
            twoFASecurityKey = twoFAStub
        )
    var twoPassUserWith2FA =
        User(
            email = emailStub, name = nameStub, password = pwdStub, mailboxPassword = mailPwdStub,
            twoFASecurityKey = twoFAStub
        )
    var autoAttachPublicKeyUser =
        User(
            email = emailStub, name = nameStub, password = pwdStub, mailboxPassword = mailPwdStub,
            twoFASecurityKey = twoFAStub
        )

    /** Message recipients **/
    var internalEmailTrustedKeys =
        User(
            email = emailStub, name = nameStub, password = pwdStub, mailboxPassword = mailPwdStub,
            twoFASecurityKey = twoFAStub
        )
    var internalEmailNotTrustedKeys =
        User(
            email = emailStub, name = nameStub, password = pwdStub, mailboxPassword = mailPwdStub,
            twoFASecurityKey = twoFAStub
        )
    var externalGmailPGPEncrypted =
        User(
            email = emailStub, name = nameStub, password = pwdStub, mailboxPassword = mailPwdStub,
            twoFASecurityKey = twoFAStub
        )
    var externalOutlookPGPSigned =
        User(
            email = emailStub, name = nameStub, password = pwdStub, mailboxPassword = mailPwdStub,
            twoFASecurityKey = twoFAStub
        )

    fun populateUsers() {
        val onePass = BaseTest.users.getUser { it.name == "onePassUser" }
        val twoPass = BaseTest.users.getUsers(false) { it.name == "twoPassAccount" }[0]
        val onePassWith2FA = BaseTest.users.getUser { it.name == "onePassUserWith2FA" }
        val twoPassWith2FA = BaseTest.users.getUsers(false)  { it.name == "twoPassUserWith2FA" }[0]
        val autoAttachPublicKey = BaseTest.users.getUser { it.name == "autoAttachPublicKeyUser" }
        val internalTrustedKeys = BaseTest.users.getUser { it.name == "internalTrustedKeys" }
        val internalUntrustedKeys = BaseTest.users.getUser { it.name == "internalUntrustedKeys" }
        val externalGmail = BaseTest.users.getUser { it.name == "gmail" }
        val externalOutlook = BaseTest.users.getUser { it.name == "outlook" }

        onePassUser =
            User(onePass.email, onePass.name, onePass.password, onePass.passphrase, onePass.twoFa)
        twoPassUser =
            User(twoPass.email, twoPass.name, twoPass.password, twoPass.passphrase, twoPass.twoFa)
        onePassUserWith2FA =
            User(
                onePassWith2FA.email,
                onePassWith2FA.name,
                onePassWith2FA.password,
                onePassWith2FA.passphrase,
                onePassWith2FA.twoFa
            )
        twoPassUserWith2FA =
            User(
                twoPassWith2FA.email,
                twoPassWith2FA.name,
                twoPassWith2FA.password,
                twoPassWith2FA.passphrase,
                twoPassWith2FA.twoFa
            )
        autoAttachPublicKeyUser =
            User(
                autoAttachPublicKey.email,
                autoAttachPublicKey.name,
                autoAttachPublicKey.password,
                autoAttachPublicKey.passphrase,
                autoAttachPublicKey.twoFa
            )
        internalEmailTrustedKeys =
            User(
                internalTrustedKeys.email,
                internalTrustedKeys.name,
                internalTrustedKeys.password,
                internalTrustedKeys.passphrase,
                internalTrustedKeys.twoFa
            )
        internalEmailNotTrustedKeys =
            User(
                internalUntrustedKeys.email,
                internalUntrustedKeys.name,
                internalUntrustedKeys.password,
                internalUntrustedKeys.passphrase,
                internalUntrustedKeys.twoFa
            )
        externalGmailPGPEncrypted =
            User(
                externalGmail.email,
                externalGmail.name,
                externalGmail.password,
                externalGmail.passphrase,
                externalGmail.twoFa
            )
        externalOutlookPGPSigned =
            User(
                externalOutlook.email,
                externalOutlook.name,
                externalOutlook.password,
                externalOutlook.passphrase,
                externalOutlook.twoFa
            )
    }
}

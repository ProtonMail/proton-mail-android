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
package ch.protonmail.android.uitests.testsHelper

/**
 * Contains users and data used in UI test runs.
 */
object TestData {

    private const val emailStub = "email_stub"
    private const val pwdStub = "pwd_stub"
    private const val mailPwdStub = "mail_pwd_stub"
    private const val twoFAStub = "2fa_stub"

    /** Test Users **/
    var onePassUser =
        User(email = emailStub, password = pwdStub, mailboxPassword = mailPwdStub, twoFASecurityKey = twoFAStub)
    var twoPassUser =
        User(email = emailStub, password = pwdStub, mailboxPassword = mailPwdStub, twoFASecurityKey = twoFAStub)
    var onePassUserWith2FA =
        User(email = emailStub, password = pwdStub, mailboxPassword = mailPwdStub, twoFASecurityKey = twoFAStub)
    var twoPassUserWith2FA =
        User(email = emailStub, password = pwdStub, mailboxPassword = mailPwdStub, twoFASecurityKey = twoFAStub)

    /** Message recipients **/
    var internalEmailTrustedKeys =
        User(email = emailStub, password = pwdStub, mailboxPassword = mailPwdStub, twoFASecurityKey = twoFAStub)
    var internalEmailNotTrustedKeys =
        User(email = emailStub, password = pwdStub, mailboxPassword = mailPwdStub, twoFASecurityKey = twoFAStub)
    var externalGmailPGPEncrypted =
        User(email = emailStub, password = pwdStub, mailboxPassword = mailPwdStub, twoFASecurityKey = twoFAStub)
    var externalOutlookPGPSigned =
        User(email = emailStub, password = pwdStub, mailboxPassword = mailPwdStub, twoFASecurityKey = twoFAStub)


    fun reSubject(subject: String): String = "Re: $subject"

    // Forwarded subject
    fun fwSubject(subject: String): String = "Fw: $subject"

    // SEARCH MESSAGE
    const val searchMessageSubject = "Random Subject"
    const val searchMessageSubjectNotFound = "MessageNotFound :O"

    // CONTACT DATA
    const val newContactName = "A new contact"
    val editContactName = "Edited on ${System.currentTimeMillis()}"
    const val editEmailAddress = "test@pm.test"

    // GROUP DATA
    val newGroupName = "A New group #${System.currentTimeMillis()}"
    val editGroupName = "Group edited on ${System.currentTimeMillis()}"
    val messageSubject = "Random Subject: ${System.currentTimeMillis()}"
    val messageBody = "\n\nHello ProtonMail!\nRandom body:\n\n${System.currentTimeMillis()}"

    const val editedPassword = "123"
    const val editedPasswordHint = "ProtonMail"
}

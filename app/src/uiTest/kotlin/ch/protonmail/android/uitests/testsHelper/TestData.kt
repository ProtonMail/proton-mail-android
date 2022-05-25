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

import ch.protonmail.android.uitests.testsHelper.StringUtils.getAlphaNumericStringWithSpecialCharacters
import ch.protonmail.android.uitests.testsHelper.StringUtils.getEmailString

/**
 * Contains users and data used in UI test runs.
 */
object TestData {

    // SEARCH MESSAGE
    const val searchMessageSubject = "Random Subject"
    const val searchMessageSubjectNotFound = "MessageNotFound :O"

    // CONTACT DATA
    val newContactName = getAlphaNumericStringWithSpecialCharacters()
    val editContactName = getAlphaNumericStringWithSpecialCharacters()
    val newEmailAddress = "${getEmailString()}@pm.me"
    val editEmailAddress = "${getEmailString()}@pm.me"

    // GROUP DATA
    val newGroupName = "A New group #${System.currentTimeMillis()}"
    val editGroupName = "Group edited on ${System.currentTimeMillis()}"

    // COMPOSER DATA
    val messageSubject = "${System.currentTimeMillis()}"
    val messageBody = "ProtonMail!\n${System.currentTimeMillis()}"
    const val pgpEncryptedText = "-----BEGIN PGP MESSAGE-----\n" +
        "Version: Mailvelope v4.3.2\n" +
        "Comment: https://www.mailvelope.com\n\n" +
        "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\n" +
        "bqbqhzuzrE0=\n" +
        "=k1Nt\n" +
        "-----END PGP MESSAGE-----"

    const val pgpEncryptedTextDecrypted = "PGP Encrypted Message From Gmail"

    const val pgpSignedText = "-----BEGIN PGP SIGNED MESSAGE-----\n" +
        "Hash: SHA256\n\n" +
        "PGP Signed Message from Outlook\n" +
        "-----BEGIN PGP SIGNATURE-----\n" +
        "Version: Mailvelope v4.3.2\n" +
        "Comment: https://www.mailvelope.com\n\n" +
        "wsFcBAEBCAAGBQJfYfjzAAoJEC+3JKDX3w9xmUoP/1GoOxRVSB+cK4pCYgPm\n" +
        "XzsCfmRqL8D0HVOCcVYah1oNsa/Rf1eWJqGRPcxJWj8qhL2SWvRBBwD7TsBC\n" +
        "HOpICo8OWiCt92PhDrcl0tADClNrKoE3oMsR4wzeSMEK1DudT8sL8X96vh9q\n" +
        "p2k1lxt3dEfsYbpatzcflFxP2EAxvKkI5xvOBk36k4w4l7bOjU763MT3voTJ\n" +
        "zzroG/2M+RN5xFgoxaP3+9wVUgJ9bEFBpHPmMG3WS/Bmer6NhvWj9UYgCesD\n" +
        "j6g2xwYhLVF6OaNmC4rTyDG+vaJEPByncd6/5ZcUh0MGPY+RKAoDsjY8MEYY\n" +
        "25mPpO1T3Gg+FkPDpenLM/IC+M5LzOlchMs0UzCvrhz2Z0sGQpU47mMo6t46\n" +
        "59t3NpetVOzqMQ/R8lyhs5qN9ickQnBdgxKItxW88RQgJe/qRW2Pnqn9Etf2\n" +
        "U27KFeijIO/RcaltqPGyoWQlpL0CMTFGKGFRAe5e8E4l0FRROXxaJocxzp5r\n" +
        "Mt1fGFlrWAMP2BBoLvM7XKLHVhMjlO89ClpVXKOGcpG1lesWGEAKKfqKfLdn\n" +
        "acyDEMBZFgYyeBE2/vmTOoglVeZf8Ca3GnIuCyZmH2IRS9haYPFgatN2Fzue\n" +
        "DMBiQzLXz4I6baquH7/85mAou4RkDwd3eBl5WMEqtUt5kySSqspWju2fiJsV\n" +
        "LSp0\n" +
        "=xmfD\n" +
        "-----END PGP SIGNATURE-----"

    const val pgpSignedTextDecrypted = "PGP Signed Message from Outlook"
    const val editedPassword = "1234"
    const val editedPasswordHint = "ProtonMail"

    // Share intent file names
    const val pdfFile = "lorem_ipsum.pdf"
    const val zipFile = "lorem_ipsum.zip"
    const val pngFile = "lorem_ipsum.png"
    const val jpegFile = "lorem_ipsum.jpeg"
    const val docxFile = "lorem_ipsum.docx"

    // Forwarded subject
    fun fwSubject(subject: String): String = "Fw: $subject"
    // Replied subject
    fun reSubject(subject: String): String = "Re: $subject"
    // Updated subject
    fun updatedSubject(subject: String): String = "Updated: $subject"
}

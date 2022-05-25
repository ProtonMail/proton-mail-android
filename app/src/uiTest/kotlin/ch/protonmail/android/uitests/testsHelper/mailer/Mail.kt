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

package ch.protonmail.android.uitests.testsHelper.mailer

import ch.protonmail.android.uitests.testsHelper.User
import org.junit.Assert.fail
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Api to send emails from 3rd party email providers.
 */
object Mail {

    val gmail = fromGmail()
    private fun fromGmail(): MailData {
        val props = Properties()
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.port"] = "587"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.ssl.enable"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.auth.mechanisms"] = "XOAUTH2"
        return MailData(props)
    }

    val outlook = fromOutlook()
    private fun fromOutlook(): MailData {
        val props = Properties()
        props["mail.smtp.host"] = "smtp.office365.com"
        props["mail.smtp.port"] = "587"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        return MailData(props)
    }

    class MailData(private var props: Properties) {
        private lateinit var from: User
        private lateinit var to: User
        private var cc: User? = null
        private var bcc: User? = null
        private lateinit var subject: String
        private lateinit var body: String

        fun from(from: User) = apply {
            this.from = from
        }

        fun to(to: User) = apply {
            this.to = to
        }

        fun cc(cc: User) = apply {
            this.cc = cc
        }

        fun bcc(bcc: User) = apply {
            this.bcc = bcc
        }

        fun withSubject(subject: String) = apply {
            this.subject = subject
        }

        fun withBody(body: String) = apply {
            this.body = body
        }

        fun send() {
            val session = Session.getInstance(
                props,
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return if (!from.email.contains("gmail")) {
                            PasswordAuthentication(from.email, from.password)
                        } else {
                            PasswordAuthentication(from.email, from.password)
                        }
                    }
                }
            )
            try {
                val message: Message = MimeMessage(session)
                message.setFrom(InternetAddress(from.email))
                message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to.email)
                )
                if (cc != null) {
                    message.setRecipients(
                        Message.RecipientType.CC,
                        InternetAddress.parse(cc?.email)
                    )
                }
                if (bcc != null) {
                    message.setRecipients(
                        Message.RecipientType.BCC,
                        InternetAddress.parse(bcc!!.email)
                    )
                }
                message.subject = subject
                message.setText(body)
                Transport.send(message)
            } catch (e: MessagingException) {
                fail("Failed to send email from external account: ${from.email}.\nError message: ${e.message}")
            }
        }

        fun sendToPmMe() {
            val session = Session.getInstance(
                props,
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(from.email, from.password)
                    }
                }
            )
            try {
                val message: Message = MimeMessage(session)
                message.setFrom(InternetAddress(from.email))
                message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to.pmMe)
                )
                if (cc != null) {
                    message.setRecipients(
                        Message.RecipientType.CC,
                        InternetAddress.parse(cc?.email)
                    )
                }
                if (bcc != null) {
                    message.setRecipients(
                        Message.RecipientType.BCC,
                        InternetAddress.parse(bcc!!.email)
                    )
                }
                message.subject = subject
                message.setText(body)
                Transport.send(message)
            } catch (e: MessagingException) {
                fail("Failed to send email from external account: ${from.email}.\nError message: ${e.message}")
            }
        }
    }
}

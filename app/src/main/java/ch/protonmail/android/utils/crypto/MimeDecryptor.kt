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
package ch.protonmail.android.utils.crypto

import com.proton.gopenpgp.crypto.KeyRing
import com.proton.gopenpgp.crypto.MIMECallbacks
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.mail.internet.InternetHeaders

class MimeDecryptor(
    private val mimeMessage: String,
    private val openPGP: OpenPGP,
    private val addressKeyRing: KeyRing
) {

    private var current: Thread? = null
    private val callbacks = Callbacks()
    private val verificationKeys = ByteArrayOutputStream()
    private var time = 0L

    fun start() {
        if (current != null) {
            throw IllegalStateException("Decryption already started")
        }
        val thread = Thread(
            null,
            {
                openPGP.decryptMIMEMessage(
                    mimeMessage,
                    verificationKeys.toByteArray(),
                    addressKeyRing,
                    callbacks,
                    time
                )
            },
            "MIMEDecryptor")
        current = thread
        thread.start()
    }

    fun withVerificationKey(verificationKey: ByteArray) {
        verificationKeys.write(verificationKey)
    }

    fun withMessageTime(messageTime: Long) {
        time = messageTime
    }

    var onVerified = { _: Boolean, _: Boolean -> }
    var onAttachment = { _: InternetHeaders, _: ByteArray -> }
    var onError = { _: Exception -> }
    var onEncryptedHeaders = { _: InternetHeaders -> }
    var onBody: (String, String) -> Unit = { _: String, _: String -> }

    fun await() {
        current?.join()
    }

    private inner class Callbacks : MIMECallbacks {

        override fun onVerified(verified: Long) {
            this@MimeDecryptor.onVerified(verified != 1L, verified == 0L)
        }

        override fun onAttachment(headers: String?, data: ByteArray?) {
            val ih = InternetHeaders()
            ih.load(ByteArrayInputStream(headers!!.toByteArray()))
            this@MimeDecryptor.onAttachment(ih, data!!)
        }

        override fun onError(err: Exception?) {
            this@MimeDecryptor.onError(err!!)
        }

        override fun onEncryptedHeaders(headers: String?) {
            val ih = InternetHeaders()
            ih.load(ByteArrayInputStream(headers!!.toByteArray()))
            this@MimeDecryptor.onEncryptedHeaders(ih)
        }

        override fun onBody(body: String, mimetype: String) {
            this@MimeDecryptor.onBody(body, mimetype)
        }
    }
}

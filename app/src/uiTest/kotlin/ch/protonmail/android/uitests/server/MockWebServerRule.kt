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

package ch.protonmail.android.uitests.server

import ch.protonmail.android.uitests.testsHelper.StringUtils.fileContentAsString
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.rules.ExternalResource

class MockWebServerRule : ExternalResource() {

    val server = MockWebServer()

    override fun before() {
        // Expires in 01/2032
        val localhostCertificate =
            HeldCertificate.decode(fileContentAsString("ssl/instrumentation_cert.pem"))

        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(localhostCertificate)
            .build()

        with(server) {
            useHttps(serverCertificates.sslSocketFactory(), tunnelProxy = false)
            start(8080)
        }
    }

    override fun after() {
        server.shutdown()
    }
}

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

package ch.protonmail.android.api.interceptors

import ch.protonmail.android.api.segments.HEADER_AUTH
import ch.protonmail.android.api.segments.HEADER_UID
import me.proton.core.network.domain.session.Session
import okhttp3.Request

fun Session.getUid() = sessionId.id
fun Session.getAuthorization() = "Bearer $accessToken"

fun Request.Builder.setSessionHeaders(session: Session) {
    header(HEADER_UID, session.getUid())
    header(HEADER_AUTH, session.getAuthorization())
}

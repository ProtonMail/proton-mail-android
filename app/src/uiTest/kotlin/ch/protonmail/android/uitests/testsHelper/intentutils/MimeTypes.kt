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

package ch.protonmail.android.uitests.testsHelper.intentutils

object MimeTypes {

    val application = Application
    val text = Text
    val image = Image
    val video = Video

    object Application {
        val pdf = "application/pdf"
        val zip = "application/zip"
        val docx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }

    object Text {
        val plain = "text/plain"
        val rtf = "text/rtf"
        val html = "text/html"
        val json = "text/json"
    }

    object Image {
        val png = "image/png"
        val jpeg = "image/jpeg"
        val gif = "image/gif"
    }

    object Video {
        val mp4 = "video/mp4"
        val jp3 = "video/3gp"
    }
}

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

package ch.protonmail.android.domain.entity

import me.proton.core.util.kotlin.equalsNoCase

enum class FileType(val extensions: Set<String>) {

    Generic(emptySet()),

    Archive(setOf("7zip", "rar", "zip")),
    Audio(setOf("mp3", "wav")),
    Doc(setOf("doc", "docx")),
    Image(setOf("jpg", "jpeg", "png")),
    Keynote(setOf("keynote")),
    Numbers(setOf("numbers")),
    Pages(setOf("pages")),
    Pdf(setOf("pdf")),
    Presentation(setOf("pot", "ppt", "pptx")),
    Video(setOf("3gp", "mkv", "mp4")),
    Xls(setOf("xls", "xlsx")),
    Xml(setOf("xml", "xliff"));

    companion object {

        fun byExtension(string: String): FileType =
            values().find { fileType ->
                fileType.extensions.any { it equalsNoCase string }
            } ?: Generic
    }
}

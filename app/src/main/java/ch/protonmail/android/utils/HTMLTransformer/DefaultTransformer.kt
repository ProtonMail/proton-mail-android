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
package ch.protonmail.android.utils.HTMLTransformer

import org.jsoup.nodes.Document

class DefaultTransformer : AbstractTransformer() {

    private val blacklist = arrayOf(
        "meta", "audio", "video", "iframe", "object",
        "picture", "form", "map", "area", "button", "input", "embed", "script", "style"
    )

    override fun transform(doc: Document): Document {
        for (blackElement in blacklist) {
            val blockedElements = doc.select(blackElement)
            if (blockedElements.`is`("form")) {
                blockedElements.unwrap()
            } else {
                blockedElements.remove()
            }
        }
        val aHrefElements = doc.select("a[ping]")
        aHrefElements.removeAttr("ping")
        val linkElements = doc.select("link")
        if (linkElements != null) {
            linkElements.select("link[rel=prefetch]").remove()
            linkElements.select("link[rel=stylesheet]").remove()
            linkElements.select("link[rel=preload]").remove()
            linkElements.select("link[rel=alternate stylesheet]").remove()
        }
        return doc
    }
}

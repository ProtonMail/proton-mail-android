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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ViewportTransformer(
    renderWidth: Int,
    css: String,
    private val darkMoreCss: String,
) : AbstractTransformer() {

    private val mRenderWidth: Int = renderWidth
    private val mCss: String = css

    override fun transform(html: Document): Document {

        val meta = "<meta name=\"viewport\" content=\"width=$mRenderWidth, maximum-scale=2\">\n"
        val messageString = StringBuilder(" <style>")
        messageString.append(mCss)
        messageString.append(darkMoreCss)
        messageString.append("</style>")
        messageString.append(meta)
        messageString.append("<div id='pm-body' class='inbox-body'>")
        messageString.append(html.toString())
        messageString.append("</div>")
        val doc = Jsoup.parse(messageString.toString())

        doc.outputSettings(html.outputSettings())
        return doc
    }
}

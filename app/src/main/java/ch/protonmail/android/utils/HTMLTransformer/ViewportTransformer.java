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
package ch.protonmail.android.utils.HTMLTransformer;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ViewportTransformer extends AbstractTransformer {

    private int mRenderWidth;
    private String mCss;

    public ViewportTransformer(int renderWidth, @NonNull String css) {
        mRenderWidth = renderWidth;
        mCss = css;
    }

    @Override
    public Document transform(Document html) {
        String meta = "<meta name=\"viewport\" content=\"width=" + mRenderWidth + ", maximum-scale=2\">\n";
        StringBuilder messageString = new StringBuilder(" <style>");
        messageString.append(mCss);
        messageString.append("</style>");
        messageString.append(meta);
        messageString.append("<div id='pm-body' class='inbox-body'>");
        messageString.append(html.toString());
        messageString.append("</div>");
        Document doc = Jsoup.parse(messageString.toString());
        doc.outputSettings(html.outputSettings());
        return doc;
    }
}

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
package ch.protonmail.android.utils;

import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;

import java.util.Collections;

import javax.inject.Inject;

import timber.log.Timber;

public class HTMLToMDConverter {

    private Remark remark;

    @Inject
    public HTMLToMDConverter() {
        Options protonMailFlavoredOptions = new Options() {{
            abbreviations = false;
            autoLinks = true;
            headerIds = false;
            fencedCodeBlocks = FencedCodeBlocks.ENABLED_BACKTICK;
            fencedCodeBlocksWidth = 3;
            ignoredHtmlElements = Collections.emptySet();
            fixPegdownStrongEmphasisInLinks = false;
            tables = Tables.MULTI_MARKDOWN;
            inWordEmphasis = InWordEmphasis.REMOVE_EMPHASIS;
        }};

        remark = new Remark(protonMailFlavoredOptions);
    }


    public String convert(String html) {
        // remove trailing spaces we do not need them.
        try {
            return remark.convert(html).replaceAll(" *\n", "\n");
        } catch (StackOverflowError exception) {
            Timber.d(exception);
        }
        try {
            // the workaround below is for slow performance devices which Remark throws stack overflow exception
            Parser parser = Parser.builder().build();
            TextContentRenderer renderer = TextContentRenderer.builder().build();
            return renderer.render(parser.parse(html));
        } catch (Exception exception) {
            Timber.d(exception);
        }
        return "";
    }
}

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
package ch.protonmail.tokenautocomplete;

import android.text.Layout;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

import java.util.Locale;

/**
 * Span that displays +[x]
 *
 * Created on 2/3/15.
 * @author mgod
 */
class CountSpan extends CharacterStyle {
    private String countText;

    CountSpan() {
        super();
        countText = "";
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        //Do nothing, we are using this span as a location marker
    }

    void setCount(int c) {
        if (c > 0) {
            countText = String.format(Locale.getDefault(), " +%d", c);
        } else {
            countText = "";
        }
    }

    String getCountText() {
        return countText;
    }

    float getCountTextWidthForPaint(TextPaint paint) {
        return Layout.getDesiredWidth(countText, 0, countText.length(), paint);
    }
}

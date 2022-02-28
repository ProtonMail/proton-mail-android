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
package ch.protonmail.android.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Button;

import ch.protonmail.android.R;

import static ch.protonmail.android.core.Constants.FONTS_FOLDER;

@Deprecated // use ProtonButton or normal Button instead
public class CustomFontButton extends Button {

    public CustomFontButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);

    }

    public CustomFontButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);

    }

    private void init(Context context, AttributeSet attrs) {
        if (!isInEditMode()) {
            setCustomFont(context, attrs);
        }
    }


    private void setCustomFont(Context context, AttributeSet attrs) {
        if (!isInEditMode()) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomFont);
            String assetPath = array.getString(R.styleable.CustomFont_fontName);

            array.recycle();

            if (!TextUtils.isEmpty(assetPath)) {
                Typeface tf = FontCache.get(FONTS_FOLDER + assetPath, getContext());
                if (tf != null) {
                    setTypeface(tf);
                }
            }
        }
    }
}


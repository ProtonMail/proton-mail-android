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
import android.widget.TextView;

import ch.protonmail.android.R;

import static ch.protonmail.android.core.Constants.FONTS_FOLDER;

@Deprecated
public class CustomFontTextView extends TextView {

    /**
     * Constructor.
     * @param context the context
     * @param attrs the attributes
     */
    public CustomFontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomFont(context, attrs);
    }

    /**
     * Constructor.
     * @param context the context
     * @param attrs the attributes
     * @param defStyle the style
     */
    public CustomFontTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomFont(context, attrs);
    }

    /**
     * Sets custom font to the view.
     * @param context the context
     * @param attrs the attributes
     */
    private void setCustomFont(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomFont);
        String assetPath = array.getString(R.styleable.CustomFont_fontName);

        array.recycle();

        if(!isInEditMode()) {
            if (!TextUtils.isEmpty(assetPath)) {
                Typeface tf = FontCache.get(FONTS_FOLDER + assetPath, getContext());
                if (tf != null) {
                    setTypeface(tf);
                }
            }
        }
    }
}

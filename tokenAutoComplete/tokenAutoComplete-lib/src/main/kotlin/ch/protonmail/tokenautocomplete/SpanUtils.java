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
package ch.protonmail.tokenautocomplete;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class SpanUtils {

    private static class EllipsizeCallback implements TextUtils.EllipsizeCallback {
        int start = 0;
        int end = 0;

        @Override
        public void ellipsized(int ellipsedStart, int ellipsedEnd) {
            start = ellipsedStart;
            end = ellipsedEnd;
        }
    }

    @Nullable
    static Spanned ellipsizeWithSpans(@Nullable CountSpan countSpan,
                                      int tokenCount, @NonNull TextPaint paint,
                                      @NonNull CharSequence originalText, float maxWidth) {

        float countWidth = 0;
        if (countSpan != null) {
            //Assume the largest possible number of items for measurement
            countSpan.setCount(tokenCount);
            countWidth = countSpan.getCountTextWidthForPaint(paint);
        }

        EllipsizeCallback ellipsizeCallback = new EllipsizeCallback();
        CharSequence tempEllipsized = TextUtils.ellipsize(originalText, paint, maxWidth - countWidth,
                TextUtils.TruncateAt.END, false, ellipsizeCallback);
        SpannableStringBuilder ellipsized = new SpannableStringBuilder(tempEllipsized);
        if (tempEllipsized instanceof Spanned) {
            TextUtils.copySpansFrom((Spanned)tempEllipsized, 0, tempEllipsized.length(), Object.class, ellipsized, 0);
        }

        if (ellipsizeCallback.start != ellipsizeCallback.end) {

            if (countSpan != null) {
                int visibleCount = ellipsized.getSpans(0, ellipsized.length(), TokenCompleteTextView.TokenImageSpan.class).length;
                countSpan.setCount(tokenCount - visibleCount);
                ellipsized.replace(ellipsizeCallback.start, ellipsized.length(), countSpan.getCountText());
                ellipsized.setSpan(countSpan, ellipsizeCallback.start, ellipsized.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return ellipsized;
        }
        //No ellipses necessary
        return null;
    }
}

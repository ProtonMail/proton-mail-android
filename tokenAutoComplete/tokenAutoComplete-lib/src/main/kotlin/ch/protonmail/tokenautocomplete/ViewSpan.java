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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Span that holds a view it draws when rendering
 * <p>
 * Created on 2/3/15.
 *
 * @author mgod
 */
public class ViewSpan extends ReplacementSpan {
    protected View view;
    private ViewSpan.Layout layout;
    private int cachedMaxWidth = -1;

    public ViewSpan(View view, ViewSpan.Layout layout) {
        super();
        this.layout = layout;
        this.view = view;
        this.view.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
    }

    private void prepView() {
        if (layout.getMaxViewSpanWidth() != cachedMaxWidth) {
            cachedMaxWidth = layout.getMaxViewSpanWidth();

            int spec = View.MeasureSpec.AT_MOST;
            if (cachedMaxWidth == 0) {
                //If the width is 0, allow the view to choose it's own content size
                spec = View.MeasureSpec.UNSPECIFIED;
            }
            int widthSpec = View.MeasureSpec.makeMeasureSpec(cachedMaxWidth, spec);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            view.measure(widthSpec, heightSpec);
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }
    }

    @Override
    public void draw(
            @NonNull Canvas canvas, CharSequence text,
            @IntRange(from = 0) int start,
            @IntRange(from = 0) int end,
            float x, int top, int y, int bottom, @NonNull Paint paint
    ) {
        prepView();

        canvas.save();
        canvas.translate(x, top);
        view.draw(canvas);
        canvas.restore();
    }

    @Override
    public int getSize(
            @NonNull Paint paint,
            CharSequence charSequence,
            @IntRange(from = 0) int start,
            @IntRange(from = 0) int end,
            @Nullable Paint.FontMetricsInt fontMetricsInt
    ) {
        prepView();

        if (fontMetricsInt != null) {
            //We need to make sure the layout allots enough space for the view
            int height = view.getMeasuredHeight();

            int adjustedBaseline = view.getBaseline();
            //-1 means the view doesn't support baseline alignment, so align bottom to font baseline
            if (adjustedBaseline == -1) {
                adjustedBaseline = height;
            }
            fontMetricsInt.ascent = fontMetricsInt.top = -adjustedBaseline;
            fontMetricsInt.descent = fontMetricsInt.bottom = height - adjustedBaseline;
        }

        return view.getRight();
    }

    public interface Layout {
        int getMaxViewSpanWidth();
    }
}

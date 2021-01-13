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
package ch.protonmail.tokenautocomplete.example;

import android.content.Context;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;

import ch.protonmail.tokenautocomplete.ViewSpan;
import kotlin.test.Test;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ViewSpanTest {

    private TestLayout layout;
    private Context context;

    private static class TestLayout implements ViewSpan.Layout {
        int width = 100;

        @Override
        public int getMaxViewSpanWidth() {
            return width;
        }
    }

    @Rule
    public ActivityTestRule<TokenActivity> activityRule = new ActivityTestRule<>(
            TokenActivity.class);

    @BeforeTest
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        layout = new TestLayout();
    }

    @Test
    public void correctLineHeightWithBaseline() {
        TextView textView = new TextView(context);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                            ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setText("A person's name");

        ViewSpan span = new ViewSpan(textView, layout);
        Paint paint = new Paint();
        Paint.FontMetricsInt fontMetricsInt = paint.getFontMetricsInt();
        int width = span.getSize(paint, "", 0, 0, fontMetricsInt);
        assertEquals(width, textView.getRight());
        assertEquals(textView.getHeight() - textView.getBaseline(), fontMetricsInt.bottom);
        assertEquals(-textView.getBaseline(), fontMetricsInt.top);
    }

    @Test
    public void correctLineHeightWithoutBaseline() {
        View view = new View(context);
        view.setMinimumHeight(1000);
        view.setMinimumWidth(1000);

        ViewSpan span = new ViewSpan(view, layout);
        Paint paint = new Paint();
        Paint.FontMetricsInt fontMetricsInt = paint.getFontMetricsInt();
        int width = span.getSize(paint, "", 0, 0, fontMetricsInt);
        assertEquals(100, width);
        assertEquals(0, fontMetricsInt.bottom);
        assertEquals(-view.getHeight(), fontMetricsInt.top);
    }

    @Test
    public void usesIntrisicLayoutParametersWhenAllowedZeroWidth() {
        View view = new View(context);
        view.setMinimumHeight(1000);
        view.setMinimumWidth(1000);

        layout.width = 0;
        ViewSpan span = new ViewSpan(view, layout);
        Paint paint = new Paint();
        Paint.FontMetricsInt fontMetricsInt = paint.getFontMetricsInt();
        int width = span.getSize(paint, "", 0, 0, fontMetricsInt);
        assertEquals(1000, width);
        assertEquals(0, fontMetricsInt.bottom);
        assertEquals(-view.getHeight(), fontMetricsInt.top);
    }
}

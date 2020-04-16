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
package ch.protonmail.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by dkadrikj on 12/3/16.
 */

public class VCardLinearLayout extends LinearLayout {

    private List<Integer> mChildIds;

    public VCardLinearLayout(Context context) {
        super(context);
        init();
    }

    public VCardLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VCardLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mChildIds = new ArrayList<>();
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        mChildIds.add(child.getId());
    }

    @Override
    public void addView(View child, int width, int height) {
        super.addView(child, width, height);
        mChildIds.add(child.getId());
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        int viewId = view.getId();
        Iterator<Integer> iterator = mChildIds.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == viewId) {
                iterator.remove();
            }
        }
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        mChildIds = new ArrayList<>();
    }

    public List<Integer> getChildIds() {
        return mChildIds;
    }

    public int getChildCount() {
        return mChildIds.size();
    }
}

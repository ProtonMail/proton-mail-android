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
package ch.protonmail.android.views.contactDetails;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import ch.protonmail.android.R;


public class SquareFloatingButtonView extends androidx.appcompat.widget.AppCompatTextView {

    public static final int FAB_TYPE_SQUARE = 1;
    public static final int FAB_TYPE_ROUNDED_SQUARE = 2;

    public static final int FAB_SIZE_NORMAL = 10;
    public static final int FAB_SIZE_MINI = 11;


    private int fabType;
    private int fabSize;
    private float fabElevation;
    private int fabColor;
    private Drawable fabIcon;
    private int fabIconColor;

    private boolean isCreated;

    public SquareFloatingButtonView(Context context) {
        this(context, null, 0);
    }

    public SquareFloatingButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SquareFloatingButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initTypedArray(attrs);
    }


    public void hide() {
        this.setVisibility(GONE);
    }

    public void show() {
        this.setVisibility(VISIBLE);
    }


    private void initTypedArray(AttributeSet attrs) {
        TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.SquareFloatingButtonView, 0, 0);

        fabType = ta.getInt(R.styleable.SquareFloatingButtonView_fabType, FAB_TYPE_SQUARE);
        fabSize = ta.getInt(R.styleable.SquareFloatingButtonView_fabSizes, FAB_SIZE_NORMAL);
        fabElevation = ta.getDimension(R.styleable.SquareFloatingButtonView_fabElevation, getResources().getDimension(R.dimen.fab_default_elevation));
        fabColor = ta.getColor(R.styleable.SquareFloatingButtonView_fabColor, ContextCompat.getColor(getContext(), R.color.new_purple));
        fabIcon = ta.getDrawable(R.styleable.SquareFloatingButtonView_fabIcon);
        fabIconColor = ta.getColor(R.styleable.SquareFloatingButtonView_fabIconColor, -1);

        ta.recycle();
    }

    private void buildView() {
        isCreated = true;
        setGravity(Gravity.CENTER);
        initFabBackground();


        initFabIconColor();
        initFabShadow();
        createSize();
        initFabPadding();
    }

    private void initFabBackground() {
        Drawable backgroundDrawable;
        switch (fabType) {
            case FAB_TYPE_ROUNDED_SQUARE:
                backgroundDrawable = ContextCompat.getDrawable(getContext(), R.drawable.fab_rounded_square_bg);
                break;
            default:
                backgroundDrawable = ContextCompat.getDrawable(getContext(), R.drawable.fab_square_bg);
                break;
        }

        if (backgroundDrawable != null)
        backgroundDrawable.mutate().setColorFilter(fabColor, PorterDuff.Mode.SRC_IN);

        Drawable selectableDrawable;
        selectableDrawable = new RippleDrawable(ColorStateList.valueOf(Color.argb(150, 255, 255, 255)),
                null, backgroundDrawable);

        LayerDrawable backgroundLayers = new LayerDrawable(new Drawable[]{
                backgroundDrawable,
                selectableDrawable});

        setBackground(backgroundLayers);
    }

    private void initFabIconColor() {
        if (fabIcon != null && fabIconColor != -1) {
            fabIcon.mutate().setColorFilter(fabIconColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private void initFabPadding() {


        int h = fabIcon.getIntrinsicHeight();
        int w = fabIcon.getIntrinsicWidth();
        fabIcon.setBounds(0, 0, w, h);
        setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.fab_text_horizontal_margin_mini));
        setCompoundDrawables(fabIcon, null, null, null);


        int iconWidth = fabIcon != null ? fabIcon.getIntrinsicWidth() : 0;
        int iconHeight = fabIcon != null ? fabIcon.getIntrinsicHeight() : 0;

        int paddingSize = fabSize == FAB_SIZE_MINI
                ? getResources().getDimensionPixelSize(R.dimen.fab_text_horizontal_margin_mini)
                : getResources().getDimensionPixelSize(R.dimen.fab_text_horizontal_margin_normal);

        int normalSize = getResources().getDimensionPixelSize(R.dimen.fab_size_normal);
        int miniSize = getResources().getDimensionPixelSize(R.dimen.fab_size_mini);

        int horizontalPadding = iconWidth == 0 ? paddingSize
                : (fabSize == FAB_SIZE_MINI ? (miniSize - iconWidth) / 2 : (normalSize - iconWidth) / 2);
        int verticalPadding = iconHeight == 0 ? paddingSize
                : (fabSize == FAB_SIZE_MINI ? (miniSize - iconHeight) / 2 : (normalSize - iconHeight) / 2);


        setPaddingRelative(horizontalPadding, verticalPadding, horizontalPadding * 2, verticalPadding);
    }

    private void initFabShadow() {
        ViewCompat.setElevation(this, fabElevation);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        createSize();
    }

    private void createSize() {
        ViewGroup.LayoutParams thisParams = getLayoutParams();

        if (fabSize == FAB_SIZE_MINI) {
            setMinHeight(getResources().getDimensionPixelSize(R.dimen.fab_size_mini));
            setMinWidth(getResources().getDimensionPixelSize(R.dimen.fab_size_mini));

            thisParams.width = getResources().getDimensionPixelSize(R.dimen.fab_size_mini);
            thisParams.height = getResources().getDimensionPixelSize(R.dimen.fab_size_mini);
        } else {
            setMinHeight(getResources().getDimensionPixelSize(R.dimen.fab_size_normal));
            setMinWidth(getResources().getDimensionPixelSize(R.dimen.fab_size_normal));

            thisParams.width = getResources().getDimensionPixelSize(R.dimen.fab_size_normal);
            thisParams.height = getResources().getDimensionPixelSize(R.dimen.fab_size_normal);

        }

        this.setLayoutParams(thisParams);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isCreated) {
            buildView();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!isCreated) {
            buildView();
        }
    }

    public int getFabType() {
        return fabType;
    }

    public void setFabType(int fabType) {
        this.fabType = fabType;
        buildView();
    }

    public int getFabSize() {
        return fabSize;
    }

    public void setFabSize(int fabSize) {
        this.fabSize = fabSize;
        buildView();
    }

    public float getFabElevation() {
        return fabElevation;
    }

    public void setFabElevation(float fabElevation) {
        this.fabElevation = fabElevation;
        buildView();
    }

    public int getFabColor() {
        return fabColor;
    }

    public void setFabColor(int fabColor) {
        this.fabColor = fabColor;
        buildView();
    }

    public Drawable getFabIcon() {
        return fabIcon;
    }

    public void setFabIcon(Drawable fabIcon) {
        this.fabIcon = fabIcon;
        buildView();
    }

    public int getFabIconColor() {
        return fabIconColor;
    }

    public void setFabIconColor(int fabIconColor) {
        this.fabIconColor = fabIconColor;
        buildView();
    }

}

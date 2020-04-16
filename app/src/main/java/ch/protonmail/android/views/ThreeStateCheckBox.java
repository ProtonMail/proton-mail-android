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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import ch.protonmail.android.R;

/**
 * Created by dkadrikj on 26.8.15.
 */
public class ThreeStateCheckBox extends LinearLayout {

    private ThreeStateButton box;
    private TextView label;

    private String labelText;
    private int state;
    private LayoutInflater inflater;

    /**
     * @param context
     */
    public ThreeStateCheckBox(Context context) {
        super(context);
        initConfig();

        inflate(getContext(), R.layout.three_state_checkbox, this);
        //inflate(context, R.layout.three_state_checkbox, this);
        //((Activity)getContext()).getLayoutInflater().inflate(R.layout.three_state_checkbox, this);

        // get views and set their values
        box = findViewById(R.id.box);
        setState(state);
        label = findViewById(R.id.label);
        //label.setTextColor(Color.WHITE);
        //label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        setText(labelText);
    }

    /**
     * @param context
     * @param attrs
     */
    public ThreeStateCheckBox(final Context context, AttributeSet attrs) {
        super(context, attrs);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        initConfig();

        // get attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ThreeStateCheckBox, 0, 0);
        labelText = a.getString(R.styleable.ThreeStateCheckBox_label);
        if (labelText == null)
            labelText = "";
        state = a.getInt(R.styleable.ThreeStateCheckBox_state, ThreeStateButton.STATE_UNPRESSED);
        // free TypedArray
        a.recycle();

    }

    private void initConfig() {
        this.setOrientation(HORIZONTAL);
        // set attribute defaults
        labelText = "";
        state = 0;
    }

    public ThreeStateButton getButton() {
        return box;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        inflater.inflate(R.layout.three_state_checkbox, this);

        // get views and set their values
        box = (ThreeStateButton) findViewById(R.id.box);
        setState(state);
        label = (TextView) findViewById(R.id.label);
        setText(labelText);
    }

    public boolean isStatePressed() {
        return box.isPressed();
    }

    public boolean isStateChecked() {
        return box.isChecked();
    }

    public int getState() {
        return box.getState();
    }

    public void setState(int state) {
        box.setState(state);
    }

    //@Override
    public CharSequence getText() {
        return label.getText();
    }

    //@Override
    public final void setText(CharSequence text) {
        label.setText(text);
    }

    public TextView getLabel() {
        return label;
    }

    public void setLabel(TextView label) {
        this.label = label;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        box.setOnClickListener(listener);
    }

    public void setOnStateChangedListener(View.OnClickListener listener) {
        box.setOnStateChangedListener(listener);
    }
}

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
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatButton;

import ch.protonmail.android.R;

public class ThreeStateButton extends AppCompatButton {

    public static final int STATE_UNPRESSED = 0;
    public static final int STATE_CHECKED = 1;
    public static final int STATE_PRESSED = 2;
    public int numberOfStates = 3;

    private int state;

    private View.OnClickListener onStateChangedListener = null;

    /**
     * @param context
     */
    public ThreeStateButton(Context context) {
        super(context);
        initConfig();
    }

    /**
     * @param context
     * @param attrs
     */
    public ThreeStateButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initConfig();
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public ThreeStateButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initConfig();
    }

    private void nextState() {
        state++;
        state = state % numberOfStates;
        //this.setPressed(false);
        // forces to redraw the view
        invalidate();
        if (onStateChangedListener != null) {
            onStateChangedListener.onClick(this);
        }
    }

    private void setButtonBackground(@DrawableRes int backgroundDrawableId) {
        setBackground(getResources().getDrawable(backgroundDrawableId));
    }

    private void initConfig() {
        // initialize variables
        state = ThreeStateButton.STATE_UNPRESSED;
        setButtonBackground(R.drawable.mail_check);
        // listeners
        setOnClickListener(v -> nextState());
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER) && (event.getAction() == KeyEvent.ACTION_UP)) {
            nextState();
            this.setPressed(false);
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (state) {
            case 0:
                setButtonBackground(R.drawable.mail_check);
                break;
            case 1:
                setButtonBackground(R.drawable.mail_check_active);
                break;
            case 2:
                // draw a tick
                setButtonBackground(R.drawable.mail_check_neutral);
                break;
            default:
                break;
        }
    }

    public void setOnStateChangedListener(View.OnClickListener listener) {
        onStateChangedListener = listener;
    }

    public boolean isUnPressed() {
        return (state == ThreeStateButton.STATE_UNPRESSED);
    }

    public boolean isPressed() {
        return (state == ThreeStateButton.STATE_PRESSED);
    }

    public boolean isChecked() {
        return (state == ThreeStateButton.STATE_CHECKED);
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        // forces to redraw the view
        invalidate();
    }

}


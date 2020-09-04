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
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Vibrator;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.settings.pin.PinAction;

/**
 * Created by dkadrikj on 3/27/16.
 */
public class SecureEditText extends LinearLayout {

    @BindView(R.id.title)
    TextView mTitle;
    @BindView(R.id.pin_input)
    EditText mInputField;
    @BindView(R.id.button_delete)
    ImageButton mDeleteButton;
    @BindView(R.id.attempts)
    TextView mAttempts;

    private PinAction mActionType = PinAction.VALIDATE;
    private ISecurePINListener mListener;

    public SecureEditText(Context context) {
        this(context, null, 0);
    }

    public SecureEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SecureEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.secure_edit_view, this, true);
        ButterKnife.bind(this);
        disableSoftInputFromAppearing(mInputField);
        if (context instanceof ISecurePINListener) {
            mListener = (ISecurePINListener) context;
        } else if (context instanceof ContextWrapper) {
            if (((ContextWrapper) context).getBaseContext() instanceof ISecurePINListener) {
                mListener = (ISecurePINListener) ((ContextWrapper) context).getBaseContext();
            }
        }
        handlePinErrorUI(ProtonMailApplication.getApplication().getUserManager());
    }

    public static void disableSoftInputFromAppearing(EditText editText) {
        editText.setRawInputType(InputType.TYPE_CLASS_TEXT);
        editText.setTextIsSelectable(true);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = true;
        boolean withinDelete = isWithinDeleteButtonBounds((int)ev.getRawX(), (int)ev.getRawY());
        if (withinDelete) {
            handled = false;
        }
        if (!handled) {
            return super.dispatchTouchEvent(ev);
        }
        return true;
    }

    public String getPin() {
        return mInputField.getText().toString();
    }

    public boolean isValid(String wantedPin) {
        String pin = mInputField.getText().toString();
        boolean isValid = false;
        if (!TextUtils.isEmpty(pin) && pin.length() >= 4) {
            isValid = true;
        }

        return isValid && pin.equals(wantedPin);
    }

    public boolean isValid() {
        String pin = mInputField.getText().toString();
        boolean isValid = false;
        if (!TextUtils.isEmpty(pin) && pin.length() >= 4) {
            isValid = true;
        }
        return isValid;
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setActionType(PinAction actionType) {
        mActionType = actionType;
    }

    public void enterKey(String keyValue) {
        StringBuilder currentValue = new StringBuilder(mInputField.getText().toString());
        if (!TextUtils.isEmpty(currentValue) && currentValue.length() >= 4) {
            Vibrator mVibrator = (Vibrator) ProtonMailApplication.getApplication().getSystemService(Context.VIBRATOR_SERVICE);
            mVibrator.vibrate(150);
            return;
        }
        currentValue.append(keyValue);
        mInputField.setText(currentValue.toString());
        mInputField.setSelection(currentValue.length());
        if(currentValue.length() >= 4) {
            switch (mActionType) {
                case CREATE:
                    if (!TextUtils.isEmpty(currentValue) && currentValue.length() >= 4) {
                        mListener.onPinMaxDigitReached();
                    }
                    break;
                case CONFIRM:

                    break;
                case VALIDATE:
                    String pin = ProtonMailApplication.getApplication().getUserManager().getMailboxPin();
                     if (pin != null && pin.equals(currentValue.toString())) {
                        if (mListener != null) {
                            mListener.onPinSuccess();
                            ProtonMailApplication.getApplication().getUserManager().resetPinAttempts();
                        }
                    } else if (currentValue.length() == 4) {
                        if (mListener != null) {
                            UserManager userManager = ProtonMailApplication.getApplication().getUserManager();
                            userManager.increaseIncorrectPinAttempt();
                            handlePinErrorUI(userManager);
                            mListener.onPinError();
                        }
                        Vibrator mVibrator = (Vibrator) ProtonMailApplication.getApplication().getSystemService(Context.VIBRATOR_SERVICE);
                        mVibrator.vibrate(450);
                        mEditTextHandler.postDelayed(new ClearTextRunnable(this), 350);
                    }
                    break;
            }
        }
    }

    private void handlePinErrorUI(UserManager userManager) {
        int currentAttempts = userManager.getIncorrectPinAttempts();
        int remainingAttempts = Constants.MAX_INCORRECT_PIN_ATTEMPTS - currentAttempts;
        if (remainingAttempts == Constants.MAX_INCORRECT_PIN_ATTEMPTS) {
            return;
        }

        mInputField.setTextColor(getResources().getColor(R.color.red));
        mInputField.setBackground(getResources().getDrawable(R.drawable.edittext_red));

        if (currentAttempts >= Constants.MAX_INCORRECT_PIN_ATTEMPTS - 3) {
            mAttempts.setTextColor(getResources().getColor(R.color.white));
            mAttempts.setBackgroundColor(getResources().getColor(R.color.red));
            mAttempts.setText(getResources().getQuantityString(R.plurals.incorrect_pin_remaining_attempts_wipe, remainingAttempts, remainingAttempts));
        } else {
            mAttempts.setText(getResources().getQuantityString(R.plurals.incorrect_pin_remaining_attempts, remainingAttempts, remainingAttempts));
        }
    }

    private void resetContent() {
        mInputField.setText("");
    }

    @OnClick(R.id.button_delete)
    public void onDelClicked() {
        StringBuilder currentValue = new StringBuilder(mInputField.getText().toString());
        if (currentValue.length() > 0) {
            currentValue.deleteCharAt(currentValue.length() - 1);
        }
        mInputField.setText(currentValue.toString());
        mInputField.setSelection(currentValue.length());
    }

    private boolean isWithinDeleteButtonBounds(int xPoint, int yPoint) {
        int[] l = new int[2];
        mDeleteButton.getLocationOnScreen(l);
        int x = l[0];
        int y = l[1];
        int w = mDeleteButton.getWidth();
        int h = mDeleteButton.getHeight();

        return xPoint >= x && xPoint <= x + w && yPoint >= y && yPoint <= y + h;
    }

    public interface ISecurePINListener {
        void onPinSuccess();
        void onPinError();
        void onPinMaxDigitReached();
    }

    private static class EditTextHandler extends Handler {
        // non leaky handler
    }

    private final EditTextHandler mEditTextHandler = new EditTextHandler();

    private static class ClearTextRunnable implements Runnable {
        // non leaky runnable
        private final WeakReference<SecureEditText> secureEditTextWeakReference;

        ClearTextRunnable(SecureEditText secureEditText) {
            secureEditTextWeakReference = new WeakReference<>(secureEditText);
        }

        @Override
        public void run() {
            SecureEditText secureEditText = secureEditTextWeakReference.get();
            if (secureEditText != null) {
                secureEditText.resetContent();
            }
        }
    }
}

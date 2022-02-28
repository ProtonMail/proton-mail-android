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
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.utils.extensions.TextExtensions;

public class MessagePasswordView extends LinearLayout {

    public enum Stage {DEFINE_PASSWORD, CONFIRM_PASSWORD, DEFINE_HINT}

    @BindView(R.id.define_password)
    EditText mDefinePassword;
    @BindView(R.id.confirm_password)
    EditText mConfirmPassword;
    @BindView(R.id.define_hint)
    EditText mDefineHint;

    private Stage mStage;
    private String mMessagePassword;
    private String mPasswordHint;
    private boolean mIsPasswordSet;
    private boolean mIsActive;
    private InputMethodManager imm;
    private final OnMessagePasswordChangedListener mListener;

    public MessagePasswordView(Context context) {
        this(context, null, 0);
    }

    public MessagePasswordView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessagePasswordView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater.from(context).inflate(R.layout.message_password_view, this, true);
        ButterKnife.bind(this);
        mListener = (OnMessagePasswordChangedListener) context;

        TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                    onNext();
                    return true;
                }

                return false;
            }
        };

        mDefinePassword.setOnEditorActionListener(onEditorActionListener);
        mConfirmPassword.setOnEditorActionListener(onEditorActionListener);
        mDefineHint.setOnEditorActionListener(onEditorActionListener);
    }

    public Stage getStage() {
        return mStage;
    }

    public void show(InputMethodManager imm) {
        mStage = Stage.DEFINE_PASSWORD;
        mIsPasswordSet = false;
        mMessagePassword = "";
        mPasswordHint = "";

        mDefinePassword.setText("");
        mConfirmPassword.setText("");
        mDefineHint.setText("");

        mIsActive = true;
        setVisibility(VISIBLE);
        setViewVisibilities();
        mDefinePassword.requestFocus();
        this.imm = imm;
        imm.showSoftInput(mDefinePassword, InputMethodManager.SHOW_IMPLICIT);
    }

    public boolean isPasswordSet() {
        return mIsPasswordSet;
    }

    public String getMessagePassword() {
        return mMessagePassword;
    }

    public String getPasswordHint() {
        return mPasswordHint;
    }

    private void setViewVisibilities() {
        mDefinePassword.setVisibility(mStage == Stage.DEFINE_PASSWORD ? VISIBLE : GONE);
        mConfirmPassword.setVisibility(mStage == Stage.CONFIRM_PASSWORD ? VISIBLE : GONE);
        mDefineHint.setVisibility(mStage == Stage.DEFINE_HINT ? VISIBLE : GONE);
    }

    @OnClick(R.id.next)
    void onNext() {
        switch (mStage) {
            case DEFINE_PASSWORD:
                if (TextUtils.isEmpty(mDefinePassword.getText())) {
                    return;
                }
                mMessagePassword = mDefinePassword.getText().toString();
                mStage = Stage.CONFIRM_PASSWORD;
                setViewVisibilities();
                mConfirmPassword.requestFocus();
                break;
            case CONFIRM_PASSWORD:
                if (TextUtils.isEmpty(mConfirmPassword.getText())) {
                    return;
                }
                if (!mMessagePassword.equals(mConfirmPassword.getText().toString())) {
                    TextExtensions.showToast(getContext(), R.string.eo_passwords_do_not_match, Toast.LENGTH_LONG, Gravity.CENTER);
                    return;
                }
                mStage = Stage.DEFINE_HINT;
                mIsPasswordSet = true;
                setViewVisibilities();
                mDefineHint.requestFocus();
                break;
            case DEFINE_HINT:
                mPasswordHint = mDefineHint.getText().toString();
                mIsPasswordSet = true;
                onHideView();
                break;
        }
    }

    @OnClick(R.id.hide_view)
    public void onHideView() {
        mIsActive = false;
        setVisibility(GONE);
        mListener.onMessagePasswordChanged();
        if (imm == null) {
            imm = (InputMethodManager) ProtonMailApplication.getApplication().getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(mDefineHint.getWindowToken(), 0);
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public boolean isValid() {
        if (mStage == Stage.DEFINE_PASSWORD || mStage == Stage.CONFIRM_PASSWORD) {
            return false;
        }
        return true;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, mStage, mMessagePassword, mPasswordHint, mIsPasswordSet, mIsActive);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mStage = savedState.getStage();
        mMessagePassword = savedState.getMessagePassword();
        mPasswordHint = savedState.getPasswordHint();
        mIsPasswordSet = savedState.isPasswordSet();
        mIsActive = savedState.isActive();

        setVisibility(mIsActive ? VISIBLE : GONE);
        setViewVisibilities();
    }

    public interface OnMessagePasswordChangedListener {
        void onMessagePasswordChanged();
    }

    protected static class SavedState extends BaseSavedState {

        private final Stage stage;
        private final String messagePassword;
        private final String passwordHint;
        private final boolean isPasswordSet;
        private final boolean isActive;

        private SavedState(Parcelable superState, Stage stage, String messagePassword, String passwordHint, boolean isPasswordSet, boolean isActive) {
            super(superState);
            this.stage = stage;
            this.messagePassword = messagePassword;
            this.passwordHint = passwordHint;
            this.isPasswordSet = isPasswordSet;
            this.isActive = isActive;
        }

        private SavedState(Parcel in) {
            super(in);
            this.stage = (Stage) in.readSerializable();
            this.messagePassword = in.readString();
            this.passwordHint = in.readString();
            this.isPasswordSet = (in.readInt() == 1);
            this.isActive = (in.readInt() == 1);
        }

        public Stage getStage() {
            return stage;
        }

        public String getMessagePassword() {
            return messagePassword;
        }

        public String getPasswordHint() {
            return passwordHint;
        }

        public boolean isPasswordSet() {
            return isPasswordSet;
        }

        public boolean isActive() {
            return isActive;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeSerializable(this.stage);
            out.writeString(this.messagePassword);
            out.writeString(this.passwordHint);
            out.writeInt(this.isPasswordSet ? 1 : 0);
            out.writeInt(this.isActive ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}

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
package ch.protonmail.android.activities.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import ch.protonmail.android.R;

/**
 * Created by dkadrikj on 19.7.15.
 */
public abstract class AbstractDialogFragment extends DialogFragment {

    private Unbinder unbinder;

    /**
     * Return the id of the fragment layout resource file.
     *
     * @return the layout id of the fragment UI.
     */
    protected abstract int getLayoutResourceId();

    /**
     * Initialize the UI here in this method.
     *
     * @param rootView the root View of the fragment UI
     */
    protected abstract void initUi(View rootView);

    protected abstract int getStyleResource();

    public abstract String getFragmentKey();

    public void onBackPressed() {
        // noop
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, getStyleResource());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // create the root view
        View rootView = inflater.inflate(getLayoutResourceId(), container, false);
        // inject all other views via ButterKnife
        unbinder = ButterKnife.bind(this, rootView);
        // init other UI related stuff (although this will be very rear case)
        initUi(rootView);

        // return view
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().setOnKeyListener((dialog, keyCode, event) -> {
            if ((keyCode == KeyEvent.KEYCODE_BACK)) {
                //This is the filter
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    onBackPressed();
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
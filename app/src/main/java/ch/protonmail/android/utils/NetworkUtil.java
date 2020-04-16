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
package ch.protonmail.android.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

import ch.protonmail.android.R;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;

public class NetworkUtil {

    public static Snackbar setNoConnectionSnackLayout(View snackBarLayout, Context context, View.OnClickListener listener) {
        return setNoConnectionSnackLayout(snackBarLayout, context, listener, false);
    }

    public static Snackbar setNoConnectionSnackLayout(View snackBarLayout, Context context, View
            .OnClickListener listener, boolean top, @StringRes int message) {
        Snackbar connectivitySnackBar = Snackbar.make(snackBarLayout, message, Snackbar.LENGTH_INDEFINITE);
        View view = connectivitySnackBar.getView();
        if (top) {
            setTopGravity(view);
        }
        view.setBackgroundColor(context.getResources().getColor(R.color.red));
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        connectivitySnackBar.setAction(context.getString(R.string.retry), listener);
        connectivitySnackBar.setActionTextColor(context.getResources().getColor(R.color.white));
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnClickListener(v -> {
            View troubleshootMessageView = LayoutInflater.from(context).inflate(R.layout.dialog_message_troubleshoot, null);
            TextView troubleshootMessage = troubleshootMessageView.findViewById(R.id.troubleshoot_message);
            troubleshootMessage.setMovementMethod(LinkMovementMethod.getInstance());
            DialogUtils.Companion.showInfoDialogWithCustomView(context,
                    R.string.troubleshoot_title,
                    troubleshootMessageView,
                    unit -> unit);
        });
        return connectivitySnackBar;
    }

    private static Snackbar setNoConnectionSnackLayout(View snackBarLayout, Context context, View.OnClickListener listener, boolean top) {
        return setNoConnectionSnackLayout(snackBarLayout, context, listener, top, R.string.no_connectivity_detected);
    }

    public static Snackbar setCheckingConnectionSnackLayout(View snackBarLayout, Context context) {
        return setCheckingConnectionSnackLayout(snackBarLayout, context, false);
    }

    private static Snackbar setCheckingConnectionSnackLayout(View snackBarLayout, Context context,
                                                             boolean top) {
        Snackbar connectivitySnackBar = Snackbar.make(snackBarLayout,
                context.getString(R.string.connectivity_checking),
                Snackbar.LENGTH_LONG);
        View view = connectivitySnackBar.getView();
        if (top) {
            setTopGravity(view);
        }
        view.setBackgroundColor(context.getResources().getColor(R.color.blue));
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);

        return connectivitySnackBar;
    }

    private static void setTopGravity(View view) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
    }
}

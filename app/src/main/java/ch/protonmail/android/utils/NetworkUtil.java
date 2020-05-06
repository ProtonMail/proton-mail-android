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

import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.snackbar.Snackbar;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import ch.protonmail.android.R;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;

public class NetworkUtil {

    public static Snackbar setNoConnectionSnackLayout(View snackBarLayout, Context context,
                                                      View.OnClickListener listener,
                                                      User user, INetworkConfiguratorCallback callback) {
        return setNoConnectionSnackLayout(snackBarLayout, context, listener, false, user, callback);
    }

    private static void showNoConnectionTroubleshootDialog(Context context, User user, INetworkConfiguratorCallback callback) {
        View troubleshootMessageView = LayoutInflater.from(context).inflate(R.layout.dialog_message_troubleshoot, null);
        TextView troubleshootMessageTextView = troubleshootMessageView.findViewById(R.id.troubleshoot_message);
        troubleshootMessageTextView.setText(Html.fromHtml(context.getString(R.string.troubleshoot_dialog_message)));
        troubleshootMessageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        TextView troubleshootDescription = troubleshootMessageView.findViewById(R.id.troubleshoot_switch_description);
        troubleshootDescription.setMovementMethod(LinkMovementMethod.getInstance());
        SwitchCompat troubleshootSwitch = troubleshootMessageView.findViewById(R.id.troubleshoot_switch);
        troubleshootSwitch.setChecked(user.getAllowSecureConnectionsViaThirdParties());
        troubleshootSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // update the uri from here
            // callback.onApiProvidersChanged();
            user.setAllowSecureConnectionsViaThirdParties(isChecked);
        });
        DialogUtils.Companion.showInfoDialogWithCustomView(context,
                context.getString(R.string.troubleshoot_dialog_title),
                troubleshootMessageView,
                unit -> {
                    callback.onApiProvidersChanged();
                    return unit;
                });
    }

    public static Snackbar setNoConnectionSnackLayout(View snackBarLayout, Context context, View
            .OnClickListener listener, boolean top, @StringRes int message, User user,
                                                      INetworkConfiguratorCallback callback) {
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
            showNoConnectionTroubleshootDialog(context, user, callback);
        });
        return connectivitySnackBar;
    }

    private static Snackbar setNoConnectionSnackLayout(View snackBarLayout, Context context,
                                                       View.OnClickListener listener,
                                                       boolean top, User user, INetworkConfiguratorCallback callback) {
        return setNoConnectionSnackLayout(snackBarLayout, context, listener, top,
                R.string.no_connectivity_detected_troubleshoot, user, callback);
    }

    public static Snackbar setCheckingConnectionSnackLayout(View snackBarLayout, Context context) {
        return setCheckingConnectionSnackLayout(snackBarLayout, context, false);
    }

    private static Snackbar setCheckingConnectionSnackLayout(View snackBarLayout, Context context,
                                                             boolean top) {
        Snackbar connectivitySnackBar = Snackbar.make(snackBarLayout,
                context.getString(R.string.connectivity_checking),
                Snackbar.LENGTH_INDEFINITE); // Dimitar, maybe return this to length long?
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

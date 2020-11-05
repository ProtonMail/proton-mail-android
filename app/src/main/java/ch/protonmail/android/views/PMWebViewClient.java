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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import ch.protonmail.android.R;
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.MessageUtils;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;

import static ch.protonmail.android.activities.composeMessage.ComposeMessageActivity.EXTRA_MAIL_TO;
import static ch.protonmail.android.activities.composeMessage.ComposeMessageActivity.EXTRA_MESSAGE_BODY;
import static ch.protonmail.android.activities.composeMessage.ComposeMessageActivity.EXTRA_MESSAGE_TITLE;

public class PMWebViewClient extends WebViewClient {

    private final UserManager mUserManager;
    private final Activity mActivity;
    private boolean mLoadRemote;
    private int mBlockedImages;

    private final List<String> hyperlinkConfirmationWhitelistedHosts = Arrays.asList(
            "protonmail.com",
            "protonmail.ch",
            "protonvpn.com",
            "protonstatus.com",
            "gdpr.eu",
            "protonvpn.net",
            "pm.me",
            "mail.protonmail.com",
            "account.protonvpn.com",
            "protonirockerxow.onion"
    );

    public PMWebViewClient(
            @NonNull UserManager userManager,
            @NonNull Activity activity,
            boolean loadRemote
    ) {
        mUserManager = userManager;
        mActivity = activity;
        mLoadRemote = loadRemote;
        mBlockedImages = 0;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url1) {
        String url = url1.replaceFirst(Constants.DUMMY_URL_PREFIX, "");

        if (url.startsWith("mailto:")) {
            composeMessageWithMailToData(url, view.getContext().getApplicationContext());
            return true;
        }

        if (url.startsWith("tel:")) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse(url));
            if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
                mActivity.startActivity(intent);
            } else {
                TextExtensions.showToast(mActivity, R.string.no_application_found);
            }
        } else {
            if (!TextUtils.isEmpty(url)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
                    if (!showHyperlinkConfirmation(url)) {
                        mActivity.startActivity(intent);
                    }
                } else {
                    TextExtensions.showToast(mActivity, R.string.no_application_found_or_link_invalid);
                }
            }
        }
        return true;
    }

    private void composeMessageWithMailToData(String url, Context context) {
        Intent intent = AppUtil.decorInAppIntent(
                new Intent(context, ComposeMessageActivity.class)
        );
        MailTo mt = MailTo.parse(url);

        User user = mUserManager.getUser();
        MessageUtils.INSTANCE.addRecipientsToIntent(
                intent,
                ComposeMessageActivity.EXTRA_TO_RECIPIENTS,
                mt.getTo(),
                Constants.MessageActionType.FROM_URL,
                user.getAddresses()
        );
        MessageUtils.INSTANCE.addRecipientsToIntent(intent,
                ComposeMessageActivity.EXTRA_CC_RECIPIENTS,
                mt.getCc(),
                Constants.MessageActionType.FROM_URL,
                user.getAddresses()
        );
        intent.putExtra(EXTRA_MAIL_TO, true);
        intent.putExtra(EXTRA_MESSAGE_TITLE, mt.getSubject());
        intent.putExtra(EXTRA_MESSAGE_BODY, mt.getBody());
        mActivity.startActivity(intent);
    }

    /**
     *
     * @return true if confirmation was shown
     */
    private boolean showHyperlinkConfirmation(@NonNull String url) {

        if (!Constants.FeatureFlags.HYPERLINK_CONFIRMATION) return false;
        try {
            URL parsedUrl = new URL(url);

            // only handle http/s protocols
            if (!"http".equals(parsedUrl.getProtocol().toLowerCase()) && !"https".equals(parsedUrl.getProtocol().toLowerCase())) return false;

            // whitelist domains
            for (String host: hyperlinkConfirmationWhitelistedHosts) {
                if (parsedUrl.getHost().endsWith(host)) return false;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean(Constants.Prefs.PREF_HYPERLINK_CONFIRM, true)) {

            String hyperlink = url; // truncate if too long
            if (hyperlink.length() > 100) {
                hyperlink = hyperlink.substring(0, 60) + "..." + hyperlink.substring(hyperlink.length() - 40);
            }
            Spanned message = Html.fromHtml(String.format(mActivity.getString(R.string.hyperlink_confirmation_dialog_text_html), hyperlink));

            DialogUtils.Companion.showInfoDialogWithTwoButtonsAndCheckbox(mActivity, "", message, mActivity.getString(R.string.cancel), mActivity.getString(R.string.cont), mActivity.getString(R.string.dont_ask_again), unit -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                mActivity.startActivity(intent);
                return null;
            }, isChecked -> {
                PreferenceManager.getDefaultSharedPreferences(mActivity).edit().putBoolean(Constants.Prefs.PREF_HYPERLINK_CONFIRM, !isChecked).apply();
                return null;
            }, true);

            return true;
        } else {
            return false;
        }
    }

    protected int amountOfRemoteResourcesBlocked() {
        return mBlockedImages;
    }

    public void blockRemoteResources(boolean block) {
        mBlockedImages = 0;
        mLoadRemote = !block;
    }

    public void loadRemoteResources() {
        blockRemoteResources(false);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (mLoadRemote) {
            return super.shouldInterceptRequest(view, url);
        }
        Uri uri = Uri.parse(url);
        if (uri.getScheme().equalsIgnoreCase("cid") || uri.getScheme().equalsIgnoreCase("data")) {
            return super.shouldInterceptRequest(view, url);
        }
        if (url.toLowerCase().contains("/favicon.ico")) {
            return super.shouldInterceptRequest(view, url);
        }
        mBlockedImages++;
        return new WebResourceResponse("text/plain" , "utf-8", new ByteArrayInputStream(new byte[0]));
    }
}

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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.protonmail.android.R;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;

public class UiUtil {

    public static final Pattern PROTON_URL_PATTERN = Pattern.compile(
            "((ht|f)tp(s?):\\/\\/|www\\.)(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private UiUtil() {

    }

    public static void copy(Context context, CharSequence text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
    }


    public static void setStatusBarColor(@NonNull Activity activity, int color) {
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(color);
    }

    public static void hideKeyboard(Context context, EditText editText) {
        if (editText != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    public static void hideKeyboard(@NonNull Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            final View view = activity.getCurrentFocus();
            if (view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static void toggleKeyboard(@NonNull Activity activity, EditText editText) {
        if (editText != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        }
    }

    public static void showKeyboard(@NonNull Activity activity, EditText editText) {
        if (editText != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, 0);
            }
        }
    }

    /**
     * If the supplied color is in #ABC format insted od #AABBCC it will normalize it, since it
     * is not supported in the Android methods.
     *
     * @param color color
     * @return normalized color
     */
    public static String normalizeColor(String color) {
        if (color == null || color.length() == 0) {
            return "";
        }
        if (color.length() == 7) {
            // no normalization needed
            return color;
        }
        StringBuilder labelColor = new StringBuilder("#");
        int colorLength = color.length();
        if (colorLength == 4) {
            // normalize the color, since 3 digit colors are not acceptable
            for (int i = 1; i < colorLength; i++) {
                labelColor.append(color.charAt(i));
                labelColor.append(color.charAt(i));
            }
        }
        return labelColor.toString();
    }

    public static int scaleColor(int color, float factor, boolean scaleAlpha) {
        return Color.argb(scaleAlpha ? (Math.round(Color.alpha(color) * factor)) : Color.alpha(color),
                Math.round(Color.red(color) * factor), Math.round(Color.green(color) * factor),
                Math.round(Color.blue(color) * factor));
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        if (context == null) {
            return result;
        }
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getToolbarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{R.attr.actionBarSize});
        int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        return toolbarHeight;
    }

    public static Snackbar showInfoSnack(View snackBarLayout, Context context, @StringRes int message) {
        Snackbar infoSnack = Snackbar.make(snackBarLayout, context.getString(message), Snackbar.LENGTH_LONG);
        View view = infoSnack.getView();
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        return infoSnack;
    }

    public static int generateViewId(View view) {
        int id = View.generateViewId();
        view.setId(id);
        return id;
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String text) {
        String textResult = text;
        if (TextUtils.isEmpty(text) && !"".equals(text)) {
            textResult = "";
        }
        if (TextUtils.isEmpty(textResult)) {
            return SpannedString.valueOf(textResult);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(textResult, Html.FROM_HTML_MODE_COMPACT);
        } else {
            return Html.fromHtml(textResult);
        }
    }

    public static String toHtml(String text) {
        return TextUtils.htmlEncode(text);
    }

    public static String createLinksSending(String input) {
        Matcher matcher = PROTON_URL_PATTERN.matcher(input);

        while (matcher.find()) {
            String match = matcher.group();
            String securedMatch;

            Pattern protocolPattern = Pattern.compile("((ht|f)tp(s?):\\/\\/)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            Matcher protocolMatcher = protocolPattern.matcher(match);

            if (protocolMatcher.find()) {
                securedMatch = match;
            } else {
                securedMatch = "http://" + match;
            }
            if (!input.contains("<a href=\"" + securedMatch + "\">" + match + "</a>")) {
                input = input.replace(match, "<a href=\"" + securedMatch + "\">" + match + "</a>");
            }
        }

        return input;
    }

    public static String createLinks(String input) {
        try {
            Spannable sp = new SpannableString(Html.fromHtml(input));
            Linkify.addLinks(sp, Linkify.WEB_URLS);
            return Html.toHtml(sp);
        } catch (Exception e) {
            return input;
        }
    }

    public static int getRenderWidth(WindowManager windowManager) {
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        float dens = dm.density;
        return (int) (width / dens);
    }

    public static void buildExpirationTimeErrorDialog(Context context, List<String> recipientsMissingPassword, List<String> recipientsDisablePgp, final View.OnClickListener okClickListener) {
        if (!(context instanceof Activity)) {
            return;
        }
        final Activity activity = (Activity) context;
        LayoutInflater inflater = activity.getLayoutInflater();
        final AlertDialog dialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = inflater.inflate(R.layout.dialog_recipient_expiration_unsupported, null);

        final LinearLayout recipientsMissingPasswordLayout = dialogView.findViewById(R.id.recipients_missing_password);
        final LinearLayout recipientsDisablePgpLayout = dialogView.findViewById(R.id.recipients_disable_pgp);
        TextView learnMore = dialogView.findViewById(R.id.learn_more_button);
        Button cancelButton = dialogView.findViewById(R.id.dialog_expiration_unsupported_cancel_button);
        Button sendAnywayButton = dialogView.findViewById(R.id.dialog_expiration_unsupported_send_anyway_button);

        ImageButton recipientsMissingPasswordSwitch = dialogView.findViewById(R.id.recipients_missing_password_switch);
        ImageButton recipientsDisablePgpSwitch = dialogView.findViewById(R.id.recipients_disable_pgp_switch);

        View recipientsMissingPasswordHeader = dialogView.findViewById(R.id.recipients_missing_password_header);
        View recipientsDisablePgpHeader = dialogView.findViewById(R.id.recipients_disable_pgp_header);

        recipientsMissingPasswordSwitch.setOnClickListener(view -> {
            boolean visible = recipientsMissingPasswordLayout.getVisibility() == View.VISIBLE;
            recipientsMissingPasswordLayout.setVisibility(visible ? View.GONE : View.VISIBLE);
        });

        recipientsDisablePgpSwitch.setOnClickListener(view -> {
            boolean visible = recipientsDisablePgpLayout.getVisibility() == View.VISIBLE;
            recipientsDisablePgpLayout.setVisibility(visible ? View.GONE : View.VISIBLE);
        });

        Linkify.addLinks(learnMore, Linkify.ALL);
        learnMore.setMovementMethod(LinkMovementMethod.getInstance());
        if (recipientsMissingPassword == null || recipientsMissingPassword.size() == 0) {
            recipientsMissingPasswordHeader.setVisibility(View.GONE);
            recipientsMissingPasswordLayout.setVisibility(View.GONE);
        } else {
            for (String email : recipientsMissingPassword) {
                View emailRowView = inflater.inflate(R.layout.item_email_expiration_error_dialog, recipientsMissingPasswordLayout, false);
                TextView emailTextView = emailRowView.findViewById(R.id.email);
                emailTextView.setText(email);
                recipientsMissingPasswordLayout.addView(emailRowView);
            }
        }

        if (recipientsDisablePgp == null || recipientsDisablePgp.size() == 0) {
            recipientsDisablePgpHeader.setVisibility(View.GONE);
            recipientsDisablePgpLayout.setVisibility(View.GONE);
        } else {
            for (String email : recipientsDisablePgp) {
                View emailRowView = inflater.inflate(R.layout.item_email_expiration_error_dialog, recipientsDisablePgpLayout, false);
                TextView emailTextView = emailRowView.findViewById(R.id.email);
                emailTextView.setText(email);
                recipientsDisablePgpLayout.addView(emailRowView);
            }
        }

        builder.setView(dialogView);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        cancelButton.setOnClickListener(v -> dialog.cancel());

        sendAnywayButton.setOnClickListener(view -> {
            okClickListener.onClick(view);
            dialog.cancel();
        });
        if (!activity.isFinishing()) {
            dialog.show();
        }
    }

    public static AlertDialog buildForceUpgradeDialog(Context context, String message) {
        if (!(context instanceof Activity)) {
            return null;
        }
        final Activity activity = (Activity) context;
        return DialogUtils.Companion.showInfoDialogWithTwoButtons(activity, context.getString(R.string.update_app_title),
                TextUtils.isEmpty(message) ? context.getString(R.string.update_app) : message,
                context.getString(R.string.upgrade),
                context.getString(R.string.learn_more),
                unit -> {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=ch.protonmail.android")));
                    return unit;
                },
                unit -> {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://protonmail.com/support/knowledge-base/update-required/")));
                    return unit;
                }, false, false, false);
    }

    public static void setTextViewDrawableColor(Context context, TextView textView, @ColorRes int color) {
        for (Drawable drawable : textView.getCompoundDrawables()) {
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(context, color), PorterDuff.Mode.SRC_IN));
            }
        }
    }

    public static String extractInitials(String name){
        String initials = "";
        String[] nameSplit = name.split(" ");
        for (int i = 0; i < nameSplit.length; i++) {
            String s = nameSplit[i];
            String str = s.replaceAll("[^\\p{L}\\d]", "");
            if (i > 0) {
                str = s.replaceAll("[^\\p{L}]", "");
            }
            if (!str.isEmpty()) {
                initials += str.charAt(0);
            }
        }
        return initials.toUpperCase();
    }

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.0").format(size/Math.pow(1024, digitGroups)) + " " +
                units[digitGroups];
    }
}

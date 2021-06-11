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
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentTransaction;

import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.protonmail.android.R;
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity;
import ch.protonmail.android.api.models.MessageRecipient;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.compose.recipients.GroupRecipientsDialogFragment;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.utils.extensions.CommonExtensionsKt;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.locks.ComposerLockIcon;

public class MessageRecipientView extends TokenCompleteTextView<MessageRecipient> {

    private Map<String, String> mPublicKeysMap;
    private Typeface mTypefacePgp, mTypefaceGroups;
    private Map<String, View> mMapView;
    private Map<String, SendPreference> sendPreferenceMap;
    private boolean mEOEnabled;
    private Constants.RecipientLocationType location;

    public void setSendPreferenceMap(Map<String, SendPreference> sendPreferenceMap, boolean mEOEnabled) {
        this.sendPreferenceMap = sendPreferenceMap;
        this.mEOEnabled = mEOEnabled;
    }

    public MessageRecipientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLongClickable(true);
        mPublicKeysMap = new HashMap<>();
        mTypefacePgp = Typeface.createFromAsset(context.getAssets(), "pgp-icons-android.ttf");
        mTypefaceGroups = Typeface.createFromAsset(context.getAssets(), "fonts/contacts_icons.ttf");
        mMapView = new HashMap<>();
    }

    public void setLocation(Constants.RecipientLocationType location) {
        this.location = location;
    }

    @Override
    protected View getViewForObject(MessageRecipient messageRecipient) {
        removeToken(messageRecipient.getEmailAddress());
        LinearLayout view = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.layout_recipient_chip, (ViewGroup) getParent(), false);

        // Text
        TextView tokenView = view.findViewById(R.id.recipient_text_text_view);
        String name = messageRecipient.getEmailAddress();
        if (!TextUtils.isEmpty(messageRecipient.getGroup())) {
            name = messageRecipient.getName();
        }
        tokenView.setText(name);
        tokenView.measure(0, 0);
        if (tokenView.getMeasuredWidth() > maxTextWidth()) {
            tokenView.setWidth(getWidth() - 100);
        }

        // Icon
        TextView tokenPgpView = view.findViewById(R.id.recipient_pgp_text_view);
        int icon = messageRecipient.getIcon();
        int groupIcon = messageRecipient.getGroupIcon();
        int color = messageRecipient.getIconColor();
        int groupColor = messageRecipient.getGroupColor();
        boolean isGroup = messageRecipient.getGroupRecipients() != null;

        if (isGroup) {
            tokenPgpView.setTypeface(mTypefaceGroups);
            if (groupIcon != 0) {
                tokenPgpView.setVisibility(VISIBLE);
                tokenPgpView.setText(getContext().getString(groupIcon));
                tokenPgpView.setTextColor(groupColor);
            }
            if (groupColor != 0) {
                // tokenPgpView.setTextColor(messageRecipient.getGroupColor());
            }
        } else {
            tokenPgpView.setTypeface(mTypefacePgp);
            if (color != 0) {
                // tokenPgpView.setTextColor(getContext().getResources().getColor(messageRecipient.getIconColor()));
            }
        }

        if (icon != 0) {
            tokenPgpView.setVisibility(VISIBLE);
            tokenPgpView.setText(getContext().getString(messageRecipient.getIcon()));
        }
        mMapView.put(messageRecipient.getEmailAddress(), view);
        return view;
    }

    public boolean containsPGPRecipient() {
        List<MessageRecipient> messageRecipients = getObjects();
        for (MessageRecipient messageRecipient : messageRecipients) {
            if (messageRecipient.isPGP()) {
                return true;
            }
        }
        return false;
    }

    public List<String> getPGPRecipients() {
        List<String> pgpRecipients = new ArrayList<>();
        List<MessageRecipient> messageRecipients = getObjects();
        for (MessageRecipient messageRecipient : messageRecipients) {
            if (messageRecipient.isPGP()) {
                pgpRecipients.add(messageRecipient.getEmailAddress());
            }
        }
        return pgpRecipients;
    }

    public void setIconAndDescription(String email, int icon, int color, int description, boolean isPgp) {
        List<MessageRecipient> messageRecipients = getObjects();
        for (MessageRecipient messageRecipient : messageRecipients) {
            if (messageRecipient.getEmailAddress().equals(email)) {
                messageRecipient.setDescription(description);
                messageRecipient.setIcon(icon);
                messageRecipient.setIconColor(color);
                messageRecipient.setIsPGP(isPgp);

                for (Map.Entry<String, View> entry : mMapView.entrySet()) {
                    if (entry.getKey().equals(email)) {
                        View view = entry.getValue();
                        TextView tokenPgpView = view.findViewById(R.id.recipient_pgp_text_view);
                        if (icon != 0) {
                            tokenPgpView.setVisibility(VISIBLE);
                            tokenPgpView.setText(getContext().getString(messageRecipient.getIcon()));
                        } else {
                            tokenPgpView.setVisibility(GONE);
                        }
                        if (color != 0) {
                            // tokenPgpView.setTextColor(getContext().getResources().getColor(messageRecipient.getIconColor()));
                        }
                        tokenPgpView.setTypeface(mTypefacePgp);
                        view.invalidate();
                        invalidate();
                        postInvalidate();
                    }
                }
            }
        }
    }

    public void removeToken(String email) {
        mMapView.remove(email);
    }

    public void removeObjectForKey(String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        List<MessageRecipient> objects = getObjects();
        if (objects.size() == 0) {
            return;
        }
        MessageRecipient recipient = null;
        for (MessageRecipient messageRecipient : objects) {
            if (key.equals(messageRecipient.getEmailAddress())) {
                recipient = messageRecipient;
                break;
            }
        }
        if (recipient != null) {
            removeObject(recipient);
        }
    }

    @Override
    protected MessageRecipient defaultObject(String completionText) {
        int index = completionText.indexOf('@');

        if (index == -1) {
            return new MessageRecipient(completionText, completionText);
        } else {
            return new MessageRecipient(completionText.substring(0, index), completionText);
        }
    }

    public List<MessageRecipient> getMessageRecipients() {
        Object[] objects = getObjects().toArray();
        if (objects.length == 0) return Collections.emptyList();
        List<MessageRecipient> allRecipients = Arrays.asList(Arrays.copyOf(objects, objects.length, MessageRecipient[].class));
        List<MessageRecipient> recipients = new ArrayList<>();
        for (MessageRecipient messageRecipient : allRecipients) {
            if (!TextUtils.isEmpty(messageRecipient.getGroup())) {
                recipients.addAll(messageRecipient.getGroupRecipients());
            } else {
                recipients.add(messageRecipient);
            }
        }
        return recipients;
    }

    public int getRecipientCount() {
        return getObjects().size();
    }

    public String findInvalidRecipient() {
        List<MessageRecipient> messageRecipients = getMessageRecipients();
        if (messageRecipients == null) return null;
        for (MessageRecipient messageRecipient : messageRecipients) {
            String emailAddress = messageRecipient.getEmailAddress();
            if (TextUtils.isEmpty(emailAddress)) {
                continue;
            }
            if (!CommonExtensionsKt.isValidEmail(emailAddress)) {
                return emailAddress;
            }
        }

        return null;
    }

    public List<String> addressesWithMissingKeys() {
        List<MessageRecipient> messageRecipients = getMessageRecipients();
        List<String> missingEmailRecipients = new ArrayList<>();
        if (messageRecipients == null) {
            return missingEmailRecipients;
        }
        for (MessageRecipient messageRecipient : messageRecipients) {
            String emailAddress = messageRecipient.getEmailAddress();
            if (TextUtils.isEmpty(emailAddress)) {
                continue;
            }
            String key = mPublicKeysMap.get(emailAddress);
            if (key == null) {
                missingEmailRecipients.add(emailAddress);
            }
        }
        return missingEmailRecipients;
    }

    public boolean includesNonProtonMailRecipient() {
        List<MessageRecipient> messageRecipients = getMessageRecipients();
        if (messageRecipients == null) return false;
        for (MessageRecipient messageRecipient : messageRecipients) {
            String emailAddress = messageRecipient.getEmailAddress();
            String key = mPublicKeysMap.get(emailAddress);
            if (TextUtils.isEmpty(emailAddress)) {
                continue;
            }
            if ("".equals(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean includesNonProtonMailAndNonPGPRecipient() {
        List<MessageRecipient> messageRecipients = getMessageRecipients();
        if (messageRecipients == null) return false;
        for (MessageRecipient messageRecipient : messageRecipients) {
            boolean isPGPRecipient = messageRecipient.isPGP();
            String emailAddress = messageRecipient.getEmailAddress();
            String key = mPublicKeysMap.get(emailAddress);
            if (TextUtils.isEmpty(emailAddress)) {
                continue;
            }
            if ("".equals(key) && !isPGPRecipient) {
                return true;
            }
        }
        return false;
    }

    public List<String> getNonProtonMailAndNonPGPRecipients() {
        List<MessageRecipient> messageRecipients = getMessageRecipients();
        List<String> nonProtonMailRecipients = new ArrayList<>();
        if (messageRecipients == null) {
            return nonProtonMailRecipients;
        }
        for (MessageRecipient messageRecipient : messageRecipients) {
            boolean isPGPRecipient = messageRecipient.isPGP();
            String emailAddress = messageRecipient.getEmailAddress();
            String key = mPublicKeysMap.get(emailAddress);
            if (TextUtils.isEmpty(emailAddress)) {
                continue;
            }
            if ("".equals(key) && !isPGPRecipient) {
                nonProtonMailRecipients.add(emailAddress);
            }
        }
        return nonProtonMailRecipients;
    }

    public void setEmailPublicKey(Map<String, String> key) {
        mPublicKeysMap.putAll(key);
    }

    public void removeKey(String email) {
        mPublicKeysMap.remove(email);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
    }

    @Override
    protected TokenImageSpan buildSpanForObject(MessageRecipient obj) {
        if(obj == null) {
            return null;
        } else {
            View tokenView = this.getViewForObject(obj);
            return new ClickableTokenImageSpan(tokenView, obj, getWidth() - getPaddingLeft() - getPaddingRight());
        }
    }

    public void invalidateRecipients() {
        post(() -> {
            List<MessageRecipient> recipients = getMessageRecipients();
            if (recipients.size() == 0) {
                return;
            }

            for (MessageRecipient recipient : recipients) {
                removeObject(recipient);
            }

            for (MessageRecipient recipient : recipients) {
                addObject(recipient);
            }
        });
    }

//    @Override
//    protected CharSequence tokenToString(MessageRecipient token) {
//        return token.getEmailAddress();
//    }

    protected class ClickableTokenImageSpan extends TokenImageSpan {

        ClickableTokenImageSpan(View d, MessageRecipient token, int maxWidth) {
            super(d, token, maxWidth);
        }

        @Override
        public void onClick() {
            //You should have access to the activity here
            MessageRecipient token = getToken();
            List<MessageRecipient> groupRecipients = token.getGroupRecipients();
            boolean isGroup = groupRecipients != null && groupRecipients.size() > 0;
            int description = token.getDescription();
            if (!isGroup && description != 0) {
                TextExtensions.showToast(getContext(), getContext().getString(description), Toast.LENGTH_SHORT);
            } else if (isGroup) {
                if (sendPreferenceMap != null) {
                    for (MessageRecipient messageRecipient : groupRecipients) {
                        if (sendPreferenceMap.containsKey(messageRecipient.getEmailAddress())) {
                            SendPreference sendPreference = sendPreferenceMap.get(messageRecipient.getEmailAddress());
                            ComposerLockIcon lock = new ComposerLockIcon(sendPreference, mEOEnabled);
                            messageRecipient.setDescription(lock.getTooltip());
                            messageRecipient.setIcon(lock.getIcon());
                            messageRecipient.setIconColor(lock.getColor());
                            messageRecipient.setIsPGP(sendPreference.isPGP());
                        }
                    }
                }
                removeObject(token);
                ArrayList<MessageRecipient> arrayListGroupRecipients = new ArrayList<>(groupRecipients);
                GroupRecipientsDialogFragment groupRecipientsDialogFragment = GroupRecipientsDialogFragment.Companion.newInstance(arrayListGroupRecipients, location);
                FragmentTransaction transaction = ((ComposeMessageActivity) getContext()).getSupportFragmentManager().beginTransaction();
                transaction.add(groupRecipientsDialogFragment, groupRecipientsDialogFragment.getFragmentKey());
                transaction.commitAllowingStateLoss();
            }
        }
    }
}

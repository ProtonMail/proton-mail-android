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
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.JobManager;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import ch.protonmail.android.R;
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.MessageRecipient;
import ch.protonmail.android.api.models.SimpleMessage;
import ch.protonmail.android.api.models.enumerations.MessageEncryption;
import ch.protonmail.android.api.models.enumerations.MessageFlag;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.jobs.MoveToFolderJob;
import ch.protonmail.android.jobs.PostArchiveJob;
import ch.protonmail.android.jobs.PostInboxJob;
import ch.protonmail.android.jobs.PostSpamJob;
import ch.protonmail.android.jobs.PostTrashJobV2;
import ch.protonmail.android.api.segments.event.AlarmReceiver;

/**
 * Created by dkadrikj on 29.9.15.
 */
public class MessageUtils {

    public static final Pattern PROTON_EMAIL_ADDRESS
            = Pattern.compile(
            "[a-zA-Z0-9\\=\\+\\.\\_\\&\\#\\%\\~\\-\\+\\'\\`\\/]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    ")+"
    );

    public static boolean matchesOneOfUserAddresses(List<Address> userAddresses, String recipient) {
        for (Address address : userAddresses) {
            if (address.getEmail().equalsIgnoreCase(recipient)) {
                return true;
            }
        }
        return false;
    }

    public static void addRecipientsToIntent(Intent intent, String extraName, String recipientList, Constants.MessageActionType messageAction, List<Address> userAddresses) {
        if (!TextUtils.isEmpty(recipientList)) {
            String[] recipients = recipientList.split(Constants.EMAIL_DELIMITER);
            ArrayList<String> list = new ArrayList<>();
            int numberOfMatches = 0;
            for (String recipient : recipients) {
                boolean matchesUserAddresses = matchesOneOfUserAddresses(userAddresses, recipient);
                if (matchesUserAddresses) {
                    numberOfMatches++;
                }
                if (!matchesUserAddresses || messageAction == Constants.MessageActionType.REPLY) {
                    list.add(recipient);
                }
            }

            if (list.size() > 0) {
                intent.putExtra(extraName, list.toArray(new String[list.size()]));
            } else if (numberOfMatches == recipients.length && ComposeMessageActivity.EXTRA_TO_RECIPIENTS.equals(extraName)) {
                list.add(recipients[0]);
                intent.putExtra(extraName, list.toArray(new String[list.size()]));
            }
        }
    }

    public static String buildNewMessageTitle(Context context, Constants.MessageActionType messageAction, String messageTitle) {
        String messagePrefix = "";
        String normalizedMessageTitle = normalizeMessageTitle(context, messageTitle);

        switch (messageAction) {
            case REPLY:
            case REPLY_ALL:
                messagePrefix = context.getString(R.string.reply_prefix) + " ";
                break;
            case FORWARD:
                messagePrefix = context.getString(R.string.forward_prefix) + " ";
                break;
        }

        return messagePrefix + normalizedMessageTitle;
    }

    private static String normalizeMessageTitle(Context context, String messageTitle) {
        String[] prefixes = new String[] {
                context.getString(R.string.reply_prefix),
                context.getString(R.string.forward_prefix)};

        for (String prefix : prefixes) {
            if (messageTitle.toLowerCase().startsWith(prefix.toLowerCase())) {
                return messageTitle.substring(prefix.length()).trim();
            }
        }

        return messageTitle;
    }

    public static boolean areAllRead(List<SimpleMessage> messages) {
        boolean result = true;
        for (SimpleMessage message : messages) {
            if (!message.isRead()) {
                result = false;
                break;
            }
        }
        return result;
    }

    public static boolean areAllUnRead(List<SimpleMessage> messages) {
        boolean result = true;
        for (SimpleMessage message : messages) {
            if (message.isRead()) {
                result = false;
                break;
            }
        }
        return result;
    }

    public static boolean containsRealContent(String text) {
        boolean result = true;

        String copy = text.replace("<div>", "").replace("</div>", "").replace("<br />", "");

        if (TextUtils.isEmpty(copy)) {
            result = false;
        }

        return result;
    }

    @Nullable
    public static String getBase64String(String emails) throws UnsupportedEncodingException {
        byte[] data = emails.getBytes("UTF-8");
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public static void moveMessage(Context context, JobManager jobManager, String folderId, List<String> folderIds, List<SimpleMessage> selectedMessages) {
        List<String> messageIds = new ArrayList<>();
        for (SimpleMessage message : selectedMessages) {
            messageIds.add(message.getMessageId());
        }
        Job job;
        if (folderId.equals(String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue()))) {
            job = new PostTrashJobV2(messageIds, folderIds, folderId);
        } else if (folderId.equals(String.valueOf(Constants.MessageLocationType.ARCHIVE.getMessageLocationTypeValue()))) {
            job = new PostArchiveJob(messageIds, folderIds);
        } else if (folderId.equals(String.valueOf(Constants.MessageLocationType.INBOX.getMessageLocationTypeValue()))) {
            job = new PostInboxJob(messageIds, folderIds);
        } else if (folderId.equals(String.valueOf(Constants.MessageLocationType.SPAM.getMessageLocationTypeValue()))) {
            job = new PostSpamJob(messageIds);
        } else {
            job = new MoveToFolderJob(messageIds, folderId);
        }

        jobManager.addJobInBackground(job);

        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.setAlarm(context, true);
    }

    public static boolean isPmMeAddress(String address) {
        return address.endsWith(Constants.MAIL_DOMAIN_PM_ME);
    }

    public static String getListString(List<MessageRecipient> messageRecipients) {
        MessageRecipient[] messageRecipientsArray = new MessageRecipient[messageRecipients.size()];
        return getListString(messageRecipients.toArray(messageRecipientsArray));
    }

    public static String getListStringGroupAware(List<MessageRecipient> messageRecipients) {
        MessageRecipient[] messageRecipientsArray = new MessageRecipient[messageRecipients.size()];
        return getListStringContactGroupAware(messageRecipients.toArray(messageRecipientsArray));
    }

    @NonNull
    private static String getListStringContactGroupAware(MessageRecipient[] messageRecipients) {
        if (messageRecipients == null) return "";
        Set<String> setOfEmailsIncludingGroups = new HashSet<>();
        for (MessageRecipient messageRecipient : messageRecipients) {
            setOfEmailsIncludingGroups.add(!TextUtils.isEmpty(messageRecipient.getGroup()) ?
                messageRecipient.getGroup() : messageRecipient.getEmailAddress());
        }
        StringBuilder builder = new StringBuilder();
        boolean skip = true;

        for (String recipient : setOfEmailsIncludingGroups) {
            if (skip) {
                skip = false;
            } else {
                builder.append(Constants.EMAIL_DELIMITER);
            }

            builder.append(recipient);
        }
        return builder.toString();
    }

    @NonNull
    private static String getListString(MessageRecipient[] messageRecipients) {
        if (messageRecipients == null) return "";
        StringBuilder builder = new StringBuilder();
        boolean skip = true;

        for (MessageRecipient messageRecipient : messageRecipients) {
            if (skip) {
                skip = false;
            } else {
                builder.append(Constants.EMAIL_DELIMITER);
            }

            builder.append(messageRecipient.getEmailAddress());
        }
        return builder.toString();
    }

    public static String getListOfStringsAsString(List<String> messageRecipients) {
        if (messageRecipients == null) return "";
        StringBuilder builder = new StringBuilder();
        boolean firstTime = true;

        for (String messageRecipient : messageRecipients) {
            if (firstTime) {
                firstTime = false;
            } else {
                builder.append(Constants.EMAIL_DELIMITER);
            }

            builder.append(messageRecipient);
        }
        return builder.toString();
    }

    public static boolean isLocalMessageId(String messageId) {
        boolean valid = false;
        try {
            UUID.fromString(messageId);
            valid = true;
        } catch (IllegalArgumentException e) {
            // noop
        }
        return valid;
    }

    public static boolean isLocalAttachmentId(String attachmentId) {
        boolean local = false;
        try {
            long value = Long.valueOf(attachmentId);
            if (value != 0) {
                local = true;
            }
        } catch (NumberFormatException e) {
            // noop
        }
        return local;
    }

    public static Message.MessageType calculateType(Long flags) {
        boolean received = (flags & MessageFlag.RECEIVED.getValue()) == MessageFlag.RECEIVED.getValue();
        boolean sent = (flags & MessageFlag.SENT.getValue()) == MessageFlag.SENT.getValue();

        if (received && sent) {
            return Message.MessageType.INBOX_AND_SENT;
        } else if (received) {
            return Message.MessageType.INBOX;
        } else if (sent) {
            return Message.MessageType.SENT;
        }
        return Message.MessageType.DRAFT;
    }

    public static MessageEncryption calculateEncryption(Long flags) {
        boolean internal = (flags & MessageFlag.INTERNAL.getValue()) == MessageFlag.INTERNAL.getValue();
        boolean e2e = (flags & MessageFlag.E2E.getValue()) == MessageFlag.E2E.getValue();
        boolean received = (flags & MessageFlag.RECEIVED.getValue()) == MessageFlag.RECEIVED.getValue();
        boolean sent = (flags & MessageFlag.SENT.getValue()) == MessageFlag.SENT.getValue();
        boolean auto = (flags & MessageFlag.AUTO.getValue()) == MessageFlag.AUTO.getValue();

        if (internal) {
            if (e2e) {
                if (received && sent) {
                    return MessageEncryption.INTERNAL;
                } else if (received && auto) {
                    return MessageEncryption.AUTO_RESPONSE;
                }
                return MessageEncryption.INTERNAL;
            }
            if (auto) {
                return MessageEncryption.AUTO_RESPONSE;
            }
            return MessageEncryption.INTERNAL;
        } else if (received && e2e) {
            return MessageEncryption.EXTERNAL_PGP;
        } else if (received) {
            return MessageEncryption.EXTERNAL;
        }
        if (e2e) {
            return MessageEncryption.MIME_PGP;
        }
        return MessageEncryption.EXTERNAL;
    }

    public static int calculateFlags(MessageEncryption messageEncryption) {
        int flags = 0;

        if (messageEncryption == MessageEncryption.MIME_PGP) {
            flags |= MessageFlag.E2E.getValue();
        }
        if (messageEncryption == MessageEncryption.AUTO_RESPONSE) {
            flags |= MessageFlag.INTERNAL.getValue();
            flags |= MessageFlag.AUTO.getValue();
        }
        if (messageEncryption == MessageEncryption.INTERNAL) {
            flags |= MessageFlag.INTERNAL.getValue();
            flags |= MessageFlag.RECEIVED.getValue();
        }
        if (messageEncryption == MessageEncryption.EXTERNAL) {
            flags |= MessageFlag.RECEIVED.getValue();
        }

        return flags;
    }
}

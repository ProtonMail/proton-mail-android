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
package ch.protonmail.android.jobs.messages;

import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.protonmail.android.BuildConfig;
import ch.protonmail.android.R;
import ch.protonmail.android.api.interceptors.RetrofitTag;
import ch.protonmail.android.api.models.MailSettings;
import ch.protonmail.android.api.models.NewMessage;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.factories.PackageFactory;
import ch.protonmail.android.api.models.factories.SendPreferencesFactory;
import ch.protonmail.android.api.models.messages.receive.AttachmentFactory;
import ch.protonmail.android.api.models.messages.receive.MessageFactory;
import ch.protonmail.android.api.models.messages.receive.MessageResponse;
import ch.protonmail.android.api.models.messages.receive.MessageSenderFactory;
import ch.protonmail.android.api.models.messages.receive.ServerMessage;
import ch.protonmail.android.api.models.messages.send.MessageSendBody;
import ch.protonmail.android.api.models.messages.send.MessageSendPackage;
import ch.protonmail.android.api.models.messages.send.MessageSendResponse;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.models.room.messages.Attachment;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.messages.MessageSender;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory;
import ch.protonmail.android.api.models.room.pendingActions.PendingSend;
import ch.protonmail.android.api.models.room.sendingFailedNotifications.SendingFailedNotification;
import ch.protonmail.android.api.models.room.sendingFailedNotifications.SendingFailedNotificationsDatabase;
import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.crypto.AddressCrypto;
import ch.protonmail.android.crypto.Crypto;
import ch.protonmail.android.domain.entity.user.Address;
import ch.protonmail.android.domain.entity.user.AddressKeys;
import ch.protonmail.android.events.MessageSentEvent;
import ch.protonmail.android.events.ParentEvent;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.receivers.VerificationOnSendReceiver;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.MessageUtils;
import ch.protonmail.android.utils.ServerTime;
import io.sentry.Sentry;
import io.sentry.event.EventBuilder;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_ERROR_VERIFICATION_NEEDED;
import static ch.protonmail.android.core.Constants.LogTags.SENDING_FAILED_DEVICE_TAG;
import static ch.protonmail.android.core.Constants.LogTags.SENDING_FAILED_REASON_TAG;
import static ch.protonmail.android.core.Constants.LogTags.SENDING_FAILED_SAME_USER_TAG;
import static ch.protonmail.android.core.Constants.LogTags.SENDING_FAILED_TAG;
import static ch.protonmail.android.core.Constants.RESPONSE_CODE_OK;
import static ch.protonmail.android.utils.AppUtil.getAppVersion;
import static ch.protonmail.android.utils.AppUtil.getExceptionStringBuilder;

public class PostMessageJob extends ProtonMailBaseJob {

    private static final int SEND_MSG_RETRY_LIMIT = 10;

    private String mOutsidersPassword;
    private String mOutsidersHint;
    private long expiresIn;
    private String mParentId;
    private Constants.MessageActionType mActionType;
    private final List<String> mNewAttachments;
    private Long mMessageDbId;
    private List<SendPreference> mSendPreferences;
    private final String mOldSenderAddressID;
    private String mUsername;

    public PostMessageJob(@NonNull Long messageDbId, String outsidersPassword, String outsidersHint, long expiresIn,
                          String parentId, Constants.MessageActionType actionType, @NonNull List<String> newAttachments,
                          List<SendPreference> sendPreferences, String oldSenderId, String username) {
        super(new Params(Priority.HIGH).groupBy(Constants.JOB_GROUP_SENDING).requireNetwork().persist());
        mMessageDbId = messageDbId;
        mOutsidersPassword = outsidersPassword;
        mOutsidersHint = outsidersHint;
        this.expiresIn = expiresIn;
        mParentId = parentId;
        mActionType = actionType;
        mNewAttachments = newAttachments;
        mSendPreferences = sendPreferences;
        mOldSenderAddressID = oldSenderId;
        mUsername = username;
    }

    @Override
    protected int getRetryLimit() {
        return SEND_MSG_RETRY_LIMIT;
    }

    //region added and cancelled
    @Override
    protected void onProtonCancel(int cancelReason, @Nullable Throwable throwable) {
        PendingActionsDatabase pendingActionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(getApplicationContext(), mUsername).getDatabase();
        Message message = getMessageDetailsRepository().findMessageByMessageDbId(mMessageDbId);
        if (message != null) {
            if (!BuildConfig.DEBUG) {
                EventBuilder eventBuilder = new EventBuilder()
                        .withTag(SENDING_FAILED_TAG, getAppVersion())
                        .withTag(SENDING_FAILED_REASON_TAG, String.valueOf(cancelReason))
                        .withTag(SENDING_FAILED_DEVICE_TAG, Build.MODEL + " " + Build.VERSION.SDK_INT)
                        .withTag("LOCATION", "CANCEL - NOTNULL")
                        .withTag("DBID", String.valueOf(mMessageDbId))
                        .withTag("EXCEPTION", (throwable != null) ? throwable.getMessage() : "NO EXCEPTION");
                StringBuilder exceptionStringBuilder = getExceptionStringBuilder(throwable);
                Sentry.capture(eventBuilder.withMessage(exceptionStringBuilder.toString()).build());
            }
            message.setLocation(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue());
            message.setLabelIDs(Arrays.asList(String.valueOf(Constants.MessageLocationType.ALL_DRAFT.getMessageLocationTypeValue()), String.valueOf(Constants.MessageLocationType.ALL_MAIL.getMessageLocationTypeValue()), String.valueOf(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue())));
            message.setTime(ServerTime.currentTimeMillis() / 1000);
            getMessageDetailsRepository().saveMessageInDB(message);
            PendingSend pendingSend = pendingActionsDatabase.findPendingSendByMessageId(message.getMessageId());
            if (pendingSend != null) {
                pendingSend.setSent(false);
                pendingActionsDatabase.insertPendingForSend(pendingSend);
            }
        } else {
            if (!BuildConfig.DEBUG) {
                EventBuilder eventBuilder = new EventBuilder()
                        .withTag(SENDING_FAILED_TAG, getAppVersion())
                        .withTag(SENDING_FAILED_REASON_TAG, String.valueOf(cancelReason))
                        .withTag(SENDING_FAILED_DEVICE_TAG, Build.MODEL + " " + Build.VERSION.SDK_INT)
                        .withTag("LOCATION", "CANCEL - NULL")
                        .withTag("EXCEPTION", (throwable != null) ? throwable.getMessage() : "NO EXCEPTION")
                        .withTag("DBID", String.valueOf(mMessageDbId));
                StringBuilder exceptionStringBuilder = getExceptionStringBuilder(throwable);
                Sentry.capture(eventBuilder.withMessage(exceptionStringBuilder.toString()).build());
            }
        }

        sendErrorSending(null);
    }
    //endregion

    //region create draft & upload attachments
    @Override
    public void onRun() throws Throwable {
        getMessageDetailsRepository().reloadDependenciesForUser(mUsername);
        ContactsDatabase contactsDatabase = ContactsDatabaseFactory.Companion.getInstance(getApplicationContext(), mUsername).getDatabase();
        PendingActionsDatabase pendingActionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(getApplicationContext(), mUsername).getDatabase();
        if (mMessageDbId == null) {
            Exception e = new RuntimeException("Message id is null");
            if (!BuildConfig.DEBUG)  {
                EventBuilder eventBuilder = new EventBuilder()
                        .withMessage(getExceptionStringBuilder(e).toString());
                Sentry.capture(eventBuilder.build());
            }
            throw e;
        }
        Message message = getMessageDetailsRepository().findMessageByMessageDbId(mMessageDbId);
        if (!BuildConfig.DEBUG && message == null) {
            EventBuilder eventBuilder = new EventBuilder()
                    .withTag(SENDING_FAILED_TAG, getAppVersion())
                    .withTag(SENDING_FAILED_DEVICE_TAG, Build.MODEL + " " + Build.VERSION.SDK_INT)
                    .withTag("LOCATION", "ONRUN - NULL")
                    .withTag("EXCEPTION", "MESSAGE NULL")
                    .withTag("DBID", String.valueOf(mMessageDbId));
            Sentry.capture(eventBuilder.withMessage("username same with primary: " + (mUsername.equals(getUserManager().getUsername()))).build());
        }
        String messageBody = message.getMessageBody();
        AddressCrypto crypto = Crypto.forAddress(getUserManager(), mUsername, message.getAddressID());
        AttachmentFactory attachmentFactory = new AttachmentFactory();
        MessageSenderFactory messageSenderFactory = new MessageSenderFactory();
        MessageFactory messageFactory = new MessageFactory(attachmentFactory, messageSenderFactory);
        final ServerMessage serverMessage = messageFactory.createServerMessage(message);
        final NewMessage newMessage = new NewMessage(serverMessage);

        newMessage.addMessageBody(Fields.Message.SELF, messageBody);
        if (mParentId != null) {
            newMessage.setParentID(mParentId);
            newMessage.setAction(mActionType.getMessageActionTypeValue());
        }

        User user = getUserManager().getUser(mUsername);
        String addressId = message.getAddressID();
        Address senderAddress1 = user.getAddressById(addressId).toNewAddress();
        String displayName = senderAddress1.getDisplayName() == null ? null : senderAddress1.getDisplayName().getS();
        newMessage.setSender(new MessageSender(displayName, senderAddress1.getEmail().getS()));

        if (MessageUtils.INSTANCE.isLocalMessageId(message.getMessageId())) {
            // create the draft if there was no connectivity previously for execution the create and post draft job
            // this however should not happen, because the jobs with the same ID are executed serial,
            // but just in case that there is no any bug on the JobQueue library
            final MessageResponse draftResponse = getApi().createDraft(newMessage);
            message.setMessageId(draftResponse.getMessageId());
        }
        message.setTime(ServerTime.currentTimeMillis() / 1000);
        getMessageDetailsRepository().saveMessageInDB(message);
        MailSettings mailSettings = getUserManager().getMailSettings(mUsername);

        try {
            uploadAttachments(message, crypto, mailSettings);
            fetchMissingSendPreferences(contactsDatabase, message, mailSettings);
        } catch (Exception e) {
            Timber.e(e);
        }

        onRunPostMessage(pendingActionsDatabase, message, crypto);
    }

    /**
     * upload all attachments, if one of the attachments fails to upload, then the message will not be sent
     * @param message Message
     * @param crypto AddressCrypto
     * @return
     * @throws Exception
     */
    private void uploadAttachments(Message message, AddressCrypto crypto, MailSettings mailSettings) throws Exception {
        List<File> attachmentTempFiles = new ArrayList<>();
        for (String attachmentId : mNewAttachments) {
            Attachment attachment = getMessageDetailsRepository().findAttachmentById(attachmentId);
            if (attachment == null) {
                continue;
            }
            if (attachment.getFilePath() == null) {
                continue;
            }
            if (attachment.isUploaded()) {
                continue;
            }
            final File file = new File(attachment.getFilePath());
            if (!file.exists()) {
                continue;
            }
            attachmentTempFiles.add(file);
            attachment.setMessage(message);

            attachment.uploadAndSave(getMessageDetailsRepository(), getApi(), crypto);
        }
        // upload public key
        if (mailSettings.getAttachPublicKey()) {
            attachPublicKey(message, crypto);
        }

        for (File file : attachmentTempFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private void attachPublicKey(Message message, AddressCrypto crypto) throws Exception {
        Address address =
                getUserManager().getUser(mUsername).getAddressById(message.getAddressID()).toNewAddress();
        AddressKeys keys = address.getKeys();
        String publicKey = crypto.buildArmoredPublicKey(keys.getPrimaryKey().getPrivateKey());

        Attachment attachment = new Attachment();
        attachment.setFileName("publickey - " + address.getEmail() + " - 0x" + crypto.getFingerprint(publicKey).substring(0, 8).toUpperCase() + ".asc");
        attachment.setMimeType("application/pgp-keys");
        attachment.setMessage(message);
        attachment.uploadAndSave(getMessageDetailsRepository(),publicKey.getBytes(), getApi(), crypto);
    }

    private void onRunPostMessage(PendingActionsDatabase pendingActionsDatabase, @NonNull Message message,
                                  AddressCrypto crypto) throws Throwable {
        AttachmentFactory attachmentFactory = new AttachmentFactory();
        MessageSenderFactory messageSenderFactory = new MessageSenderFactory();
        MessageFactory messageFactory = new MessageFactory(attachmentFactory, messageSenderFactory);
        final ServerMessage serverMessage = messageFactory.createServerMessage(message);
        final NewMessage newMessage = new NewMessage(serverMessage);

        newMessage.addMessageBody(Fields.Message.SELF, message.getMessageBody());

        List<Attachment> parentAttachmentList;
        MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(getApplicationContext(), mUsername).getDatabase();
        parentAttachmentList = message.attachments(messagesDatabase);
        User user = getUserManager().getUser(mUsername);
        String addressId = message.getAddressID();
        ch.protonmail.android.api.models.address.Address senderAddress =
                user.getAddressById(addressId);
        updateAttachmentKeyPackets(parentAttachmentList, newMessage, mOldSenderAddressID, senderAddress);
        newMessage.setSender(new MessageSender(senderAddress.getDisplayName(), senderAddress.getEmail()));

        if (message.getSenderEmail().contains("+")) { // it's being sent by alias
            newMessage.setSender(new MessageSender(message.getSenderName(), message.getSenderEmail()));
        }

        final MessageResponse draftResponse = getApi().updateDraft(message.getMessageId(), newMessage, new RetrofitTag(mUsername));
        EventBuilder eventBuilder = new EventBuilder()
                .withTag(SENDING_FAILED_TAG, getAppVersion())
                .withTag(SENDING_FAILED_DEVICE_TAG, Build.MODEL + " " + Build.VERSION.SDK_INT)
                .withTag(SENDING_FAILED_SAME_USER_TAG, String.valueOf(username.equals(getUserManager().getUsername())))
                .withTag("LOC", "ONRUNPOSTMESSAGE");

        if (draftResponse == null) {
            if (!BuildConfig.DEBUG) {
                eventBuilder.withMessage("Draft response is null");
                Sentry.capture(eventBuilder.build());
            }
            return;
        }

        if (draftResponse.getCode() != RESPONSE_CODE_OK) {
            if (!BuildConfig.DEBUG) {
                eventBuilder.withMessage(draftResponse.getCode() + " " + draftResponse.getError());
                Sentry.capture(eventBuilder.build());
                sendErrorSending(draftResponse.getError());
            }
            return;
        }

        Message draftMessage = draftResponse.getMessage();
        draftMessage.decrypt(getUserManager(), mUsername);
        //region create packages to send to API
        PackageFactory packageFactory = new PackageFactory(getApi(), crypto, mOutsidersPassword, mOutsidersHint);
        List<MessageSendPackage> packages = packageFactory.generatePackages(draftMessage, mSendPreferences);

        MessageSendBody Body = new MessageSendBody(packages, expiresIn <= 0 ? null : expiresIn, getUserManager().getMailSettings(mUsername).getAutoSaveContacts());
        //endregion

        //region sending the message
        MessageSendResponse messageSendResponse;
        Call<MessageSendResponse> responseCall = getApi().sendMessage(message.getMessageId(), Body, new RetrofitTag(mUsername));
        Response<MessageSendResponse> response = responseCall.execute();

        StringBuilder builder = new StringBuilder("HTTP CODE " + response.code());
        if (!response.isSuccessful()) {
            builder.append("response NOT SUCCESS \n");
            okhttp3.ResponseBody error = response.errorBody();
            messageSendResponse = new Gson().fromJson(error.charStream(), MessageSendResponse.class);
            String serverError = String.format(getApplicationContext().getString(R.string.message_drafted_error_server), messageSendResponse.getError());
            builder.append(serverError + " \n");
            sendErrorSending(serverError);
        } else {
            messageSendResponse = response.body();
            if (messageSendResponse != null && messageSendResponse.getCode() != RESPONSE_CODE_OK) {
                builder.append("response SUCCESS response code: " + messageSendResponse.getCode() + " \n");
                String errorSending = "";
                if (!TextUtils.isEmpty(messageSendResponse.getError())) {
                    errorSending = String.format(getApplicationContext().getString(R.string.message_drafted_error_server), messageSendResponse.getError());
                    builder.append("response SUCCESS response error NOT EMPTY: " + errorSending + " \n");
                } else {
                    errorSending = getApplicationContext().getString(R.string.message_drafted);
                    builder.append("response SUCCESS response error EMPTY: " + errorSending + " \n");
                }
                if (messageSendResponse.getCode() == RESPONSE_CODE_ERROR_VERIFICATION_NEEDED) {
                    builder.append("response SUCCESS response error ERROR_VERIFICATION_NEEDED \n");
                    Intent verifyIntent = new Intent(getApplicationContext().getString(R.string.notification_action_verify));
                    verifyIntent.putExtra(Constants.ERROR, getApplicationContext().getString(R.string.message_drafted_verification_needed));
                    verifyIntent.putExtra(VerificationOnSendReceiver.EXTRA_MESSAGE_ID, message.getMessageId());
                    verifyIntent.putExtra(VerificationOnSendReceiver.EXTRA_MESSAGE_INLINE, message.isInline());
                    verifyIntent.putExtra(VerificationOnSendReceiver.EXTRA_MESSAGE_ADDRESS_ID, message.getAddressID());
                    ProtonMailApplication.getApplication().sendOrderedBroadcast(verifyIntent, null);
                } else {
                    if (!BuildConfig.DEBUG) {
                        Sentry.capture(eventBuilder.withMessage(builder.toString()).build());
                    }
                    sendErrorSending(errorSending);
                    return;
                }
            } else {
                if (messageSendResponse != null) {
                    messageSendResponse.getSent().writeTo(message);
                }
                // success
                message.setLocation(Constants.MessageLocationType.SENT.getMessageLocationTypeValue());
                message.setLabelIDs(Arrays.asList(String.valueOf(Constants.MessageLocationType.ALL_SENT.getMessageLocationTypeValue()), String.valueOf(Constants.MessageLocationType.ALL_MAIL.getMessageLocationTypeValue()), String.valueOf(Constants.MessageLocationType.SENT.getMessageLocationTypeValue())));
                message.removeLabels(Arrays.asList(String.valueOf(Constants.MessageLocationType.ALL_DRAFT.getMessageLocationTypeValue()), String.valueOf(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue())));
                getMessageDetailsRepository().saveMessageInDB(message);
                pendingActionsDatabase.deletePendingSendByMessageId(message.getMessageId());
            }
        }
        if (!TextUtils.isEmpty(message.getMessageId())) {
            PendingSend pendingForSending = pendingActionsDatabase.findPendingSendByMessageId(message.getMessageId());

            if (pendingForSending != null) {
                pendingForSending.setSent(response.isSuccessful());
                pendingActionsDatabase.insertPendingForSend(pendingForSending);
            }
        }
//        15186 code for recipient not found
        MessageSentEvent event = new MessageSentEvent(messageSendResponse.getCode());
        AppUtil.postEventOnUi(event);
        if (messageSendResponse.getParent() != null){
            AppUtil.postEventOnUi(new ParentEvent(messageSendResponse.getParent().getID(), messageSendResponse.getParent().getIsReplied(), messageSendResponse.getParent().getIsRepliedAll(), messageSendResponse.getParent().getIsForwarded()));
        }
        //endregion
    }

    private void updateAttachmentKeyPackets(List<Attachment> attachmentList, NewMessage newMessage, String oldSenderAddress, ch.protonmail.android.api.models.address.Address newSenderAddress) throws Exception {
        if (!TextUtils.isEmpty(oldSenderAddress)) {
            AddressCrypto oldCrypto = Crypto.forAddress(getUserManager(), mUsername, oldSenderAddress);
            AddressKeys newAddressKeys = newSenderAddress.toNewAddress().getKeys();
            String newPublicKey = oldCrypto.buildArmoredPublicKey(newAddressKeys.getPrimaryKey().getPrivateKey());
            for (Attachment attachment : attachmentList) {
                String AttachmentID = attachment.getAttachmentId();
                String keyPackets = attachment.getKeyPackets();
                if (TextUtils.isEmpty(keyPackets)) {
                    continue;
                }
                try {
                    byte[] keyPackage = Base64.decode(keyPackets, Base64.DEFAULT);
                    byte[] sessionKey = oldCrypto.decryptKeyPacket(keyPackage);
                    byte[] newKeyPackage = oldCrypto.encryptKeyPacket(sessionKey, newPublicKey);
                    String newKeyPackets = Base64.encodeToString(newKeyPackage, Base64.NO_WRAP);
                    if (!TextUtils.isEmpty(keyPackets)) {
                        newMessage.addAttachmentKeyPacket(AttachmentID, newKeyPackets);
                    }
                } catch (Exception e) {
                    if (!TextUtils.isEmpty(keyPackets)) {
                        newMessage.addAttachmentKeyPacket(AttachmentID, keyPackets);
                    }
                    Logger.doLogException(e);
                }
            }
        } else {
            updateAttachmentKeyPackets(attachmentList, newMessage);
        }
    }

    private void updateAttachmentKeyPackets(List<Attachment> attachmentList, NewMessage newMessage) {
        for (Attachment attachment : attachmentList) {
            String AttachmentID = attachment.getAttachmentId();
            String keyPackets = attachment.getKeyPackets();
            if (!TextUtils.isEmpty(keyPackets)) {
                newMessage.addAttachmentKeyPacket(AttachmentID, keyPackets);
            }
        }
    }

    private void fetchMissingSendPreferences(ContactsDatabase contactsDatabase, Message message, MailSettings mailSettings) {
       Set<String> emailSet = new HashSet<>();
        if (!TextUtils.isEmpty(message.getToListString())) {
            String[] emails = message.getToListString().split(Constants.EMAIL_DELIMITER);
            emailSet.addAll(Arrays.asList(emails));
        }
        if (!TextUtils.isEmpty(message.getCcListString())) {
            String[] emails = message.getCcListString().split(Constants.EMAIL_DELIMITER);
            emailSet.addAll(Arrays.asList(emails));
        }
        if (!TextUtils.isEmpty(message.getBccListString())) {
            String[] emails = message.getBccListString().split(Constants.EMAIL_DELIMITER);
            emailSet.addAll(Arrays.asList(emails));
        }
        for (SendPreference sp : mSendPreferences) {
            emailSet.remove(sp.getEmailAddress());
        }

        SendPreferencesFactory factory = new SendPreferencesFactory(getApi(), getUserManager(), mUsername, mailSettings, contactsDatabase);
        try {
            Map<String, SendPreference> sendPreferenceMap = factory.fetch(Arrays.asList(emailSet.toArray(new String[0])));
            ArrayList<SendPreference> sendPrefs = new ArrayList<>();
            sendPrefs.addAll(mSendPreferences);
            sendPrefs.addAll(sendPreferenceMap.values());
            mSendPreferences = sendPrefs;
        } catch (Exception e) {
            // TODO: 8/20/18 Kay, please check if this is ok, or when exception is thrown the UI should be informed
            Logger.doLogException(e);
        }
    }

    private void sendErrorSending(String error) {
        if (error == null) error = "";
        Message message = getMessageDetailsRepository().findMessageByMessageDbId(mMessageDbId);
        if (message != null) {
            String messageId = message.getMessageId();
            if (messageId != null) {
                SendingFailedNotificationsDatabase sendingFailedNotificationsDatabase = getMessageDetailsRepository().getDatabaseProvider().provideSendingFailedNotificationsDao(getUserManager().getUsername());
                sendingFailedNotificationsDatabase.insertSendingFailedNotification(new SendingFailedNotification(messageId, message.getSubject(), error));

                if (sendingFailedNotificationsDatabase.count() > 1) {
                    List<SendingFailedNotification> sendingFailedNotifications = sendingFailedNotificationsDatabase.findAllSendingFailedNotifications();
                    ProtonMailApplication.getApplication().notifyMultipleErrorSendingMessage(sendingFailedNotifications, getUserManager().getUser());
                } else {
                    ProtonMailApplication.getApplication().notifySingleErrorSendingMessage(message, error, getUserManager().getUser());
                }
            }
        }
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return RetryConstraint.createExponentialBackoff(runCount, 500);
    }
}

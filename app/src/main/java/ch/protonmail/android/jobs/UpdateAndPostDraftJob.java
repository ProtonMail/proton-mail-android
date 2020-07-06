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
package ch.protonmail.android.jobs;

import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.protonmail.android.api.interceptors.RetrofitTag;
import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.api.models.Keys;
import ch.protonmail.android.api.models.NewMessage;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.messages.receive.AttachmentFactory;
import ch.protonmail.android.api.models.messages.receive.MessageFactory;
import ch.protonmail.android.api.models.messages.receive.MessageResponse;
import ch.protonmail.android.api.models.messages.receive.MessageSenderFactory;
import ch.protonmail.android.api.models.messages.receive.ServerMessage;
import ch.protonmail.android.api.models.room.messages.Attachment;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.messages.MessageSender;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory;
import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.AttachmentFailedEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.crypto.AddressCrypto;
import ch.protonmail.android.utils.crypto.Crypto;

public class UpdateAndPostDraftJob extends ProtonMailBaseJob {

    private static final String TAG_UPDATE_AND_POST_DRAFT_JOB = "UpdateAndPostDraftJob";
    private static final int UPDATE_DRAFT_RETRY_LIMIT = 10;

    private Long mMessageDbId;
    private final List<String> mNewAttachments;
    private final boolean mUploadAttachments;
    private final String mOldSenderAddressID;
    private final String mUsername;

    public UpdateAndPostDraftJob(@NonNull Long messageDbId, @NonNull List<String> newAttachments,
                                 boolean uploadAttachments, String oldSenderId, String username) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_SENDING));
        mMessageDbId = messageDbId;
        mNewAttachments = newAttachments;
        mUploadAttachments = uploadAttachments;
        mOldSenderAddressID = oldSenderId;
        mUsername = username;
    }

    @Override
    protected int getRetryLimit() {
        return UPDATE_DRAFT_RETRY_LIMIT;
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        PendingActionsDatabase pendingActionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(
                getApplicationContext(), mUsername).getDatabase();

        if (mUploadAttachments && (mNewAttachments != null && mNewAttachments.size() > 0)) {
            pendingActionsDatabase.deletePendingDraftById(mMessageDbId);
        }
        return RetryConstraint.CANCEL;
    }

    @Override
    public void onRun() throws Throwable {
        messageDetailsRepository.reloadDependenciesForUser(mUsername);
        PendingActionsDatabase pendingActionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(getApplicationContext(), mUsername).getDatabase();

        Message message = messageDetailsRepository.findMessageByMessageDbId(mMessageDbId);
        if (message == null) {
            // todo show error to the user
            return;
        }
        String addressId = message.getAddressID();
        AttachmentFactory attachmentFactory = new AttachmentFactory();
        MessageSenderFactory messageSenderFactory = new MessageSenderFactory();
        MessageFactory messageFactory = new MessageFactory(attachmentFactory, messageSenderFactory);
        final ServerMessage serverMessage = messageFactory.createServerMessage(message);
        final NewMessage newMessage = new NewMessage(serverMessage);
        String encryptedMessage = message.getMessageBody();

        User user = mUserManager.getUser(mUsername);
        Address senderAddress = user.getAddressById(addressId);
        newMessage.setSender(new MessageSender(senderAddress.getDisplayName(), senderAddress.getEmail()));

        newMessage.addMessageBody(Fields.Message.SELF, encryptedMessage);
        updateAttachmentKeyPackets(mNewAttachments, newMessage, mOldSenderAddressID, senderAddress);
        if (message.getSenderEmail().contains("+")) { // it's being sent by alias
            newMessage.setSender(new MessageSender(message.getSenderName(), message.getSenderEmail()));
        }
        final MessageResponse draftResponse = mApi.updateDraft(newMessage.getMessage().getID(), newMessage, new RetrofitTag(mUsername));
        if (draftResponse.getCode() == Constants.RESPONSE_CODE_OK) {
            mApi.markMessageAsRead(new IDList(Arrays.asList(newMessage.getMessage().getID())));
        } else {
            pendingActionsDatabase.deletePendingUploadByMessageId(message.getMessageId());
            return;
        }
        message.setLabelIDs(draftResponse.getMessage().getEventLabelIDs());
        message.setIsRead(true);
        message.setDownloaded(true);
        message.setLocation(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue());
        saveMessage(message, pendingActionsDatabase);
    }

    @Override
    protected void onProtonCancel(int cancelReason, @Nullable Throwable throwable) {
        messageDetailsRepository.reloadDependenciesForUser(mUsername);
        Message message = messageDetailsRepository.findMessageByMessageDbId(mMessageDbId);
        if (message == null) {
            return;
        }
        PendingActionsDatabase actionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(getApplicationContext(), mUsername).getDatabase();
        actionsDatabase.deletePendingUploadByMessageId(message.getMessageId());
        actionsDatabase.deletePendingDraftById(mMessageDbId);
    }

    private void saveMessage(Message message, PendingActionsDatabase pendingActionsDatabase) {
        AddressCrypto addressCrypto = Crypto.forAddress(mUserManager, mUsername, message.getAddressID());
        Set<Attachment> currentAttachments = new HashSet<>(message.getAttachments());
        List<Attachment> updatedAtt = updateDraft(pendingActionsDatabase, addressCrypto, message.getMessageId());
        for (Attachment updatedAttachment : updatedAtt) {
            boolean found = false;
            Attachment att = null;
            for (Attachment currentAttachment : currentAttachments) {
                if (currentAttachment.getFileName().equals(updatedAttachment.getFileName())) {
                    att = currentAttachment;
                    found = true;
                    break;
                }
            }
            if (found) {
                currentAttachments.remove(att);
                currentAttachments.add(updatedAttachment);
            }
        }
        message.setAttachmentList(new ArrayList<>(currentAttachments));
        messageDetailsRepository.saveMessageInDB(message);
    }

    private void updateAttachmentKeyPackets(List<String> attachmentList, NewMessage newMessage, String oldSenderAddress, Address newSenderAddress) throws Exception {
        if (!TextUtils.isEmpty(oldSenderAddress)) {
            AddressCrypto oldCrypto = Crypto.forAddress(mUserManager, mUsername, oldSenderAddress);
            List<Keys> newAddressKeys = newSenderAddress.getKeys();
            String newPublicKey = oldCrypto.getArmoredPublicKey(newAddressKeys.get(0));
            for (String attachmentId : attachmentList) {
                Attachment attachment = messageDetailsRepository.findAttachmentById(attachmentId);
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
        }
    }

    private ArrayList<Attachment> updateDraft(PendingActionsDatabase pendingActionsDatabase, AddressCrypto addressCrypto, String messageId)  {
        ArrayList<Attachment> updatedAttachments = new ArrayList<>();
        Message message = messageDetailsRepository.findMessageByMessageDbId(mMessageDbId);
        if (message != null && mUploadAttachments && (mNewAttachments != null && mNewAttachments.size() > 0)) {
            String mMessageId = message.getMessageId();
            for (String attachmentId : mNewAttachments) {
                Attachment attachment = messageDetailsRepository.findAttachmentById(attachmentId);
                try {
                    if (attachment == null) {
                        continue;
                    }
                    if (attachment.getFilePath() == null) {
                        continue;
                    }
                    if (attachment.isUploaded()) {
                        continue;
                    }
                    String filePath = attachment.getFilePath();
                    if (TextUtils.isEmpty(filePath)) {
                        continue;
                    }
                    final File file = new File(attachment.getFilePath());
                    if (!file.exists()) {
                        continue;
                    }
                    // this is just a hack until new complete composer refactoring is done for some of the next versions
                    attachment.setMessageId(messageId);
                    attachment.setAttachmentId(attachment.uploadAndSave(messageDetailsRepository, mApi, addressCrypto));
                    updatedAttachments.add(attachment);
                } catch (Exception e) {
                    Logger.doLogException(TAG_UPDATE_AND_POST_DRAFT_JOB, "error while attaching file: " + attachment.getFilePath(), e);
                    AppUtil.postEventOnUi(new AttachmentFailedEvent(message.getMessageId(),
                            message.getSubject(), attachment.getFileName()));
                }
            }
            pendingActionsDatabase.deletePendingUploadByMessageId(mMessageId);
        }
        return updatedAttachments;
    }
}

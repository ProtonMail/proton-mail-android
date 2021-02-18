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
package ch.protonmail.android.storage;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.ProtonJobIntentService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadata;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.models.room.counters.CounterDatabase;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.api.models.room.notifications.NotificationsDatabaseFactory;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.data.local.AttachmentMetadataDao;
import ch.protonmail.android.data.local.AttachmentMetadataDatabase;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AttachmentClearingService extends ProtonJobIntentService {

    public static final String ACTION_REGULAR_CHECK = "ACTION_REGULAR_CHECK";
    public static final String ACTION_CLEAR_CACHE_IMMEDIATELY = "ACTION_CLEAR_CACHE_IMMEDIATELY";
    public static final String ACTION_CLEAR_CACHE_IMMEDIATELY_DELETE_TABLES = "ACTION_CLEAR_CACHE_IMMEDIATELY_DELETE_TABLES";
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";

    @Inject
    UserManager mUserManager;

    @Inject
    MessageDetailsRepository messageDetailsRepository;

    private AttachmentMetadataDao attachmentMetadataDao;

    public AttachmentClearingService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        attachmentMetadataDao = AttachmentMetadataDatabase.Companion.getInstance(this).getDao();
    }

    public static void startClearUpImmediatelyService() {
        final Context context = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(context, AttachmentClearingService.class);
        intent.setAction(ACTION_CLEAR_CACHE_IMMEDIATELY);
        enqueueWork(context, AttachmentClearingService.class, Constants.JOB_INTENT_SERVICE_ID_ATTACHMENT_CLEARING, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
            if (!mUserManager.isLoggedIn()) {
                return;
            }
            String action = intent.getAction();
            User user = mUserManager.getUser();
            if (ACTION_REGULAR_CHECK.equals(action)) {
                long currentEmbeddedImagesSize = attachmentMetadataDao.getAllAttachmentsSizeUsed();
                long maxSize = user.getMaxAllowedAttachmentSpace();
                if (maxSize == -1) {
                    return;
                }
                maxSize = maxSize * 1000 * 1000; // in bytes
                List<Message> leastAccessMessages = messageDetailsRepository.findAllMessageByLastMessageAccessTime(0);
                if (maxSize > currentEmbeddedImagesSize) {
                    return; // no need to delete attachments
                }
                long neededSpaceToFreeUp = (currentEmbeddedImagesSize - maxSize) + (long) (maxSize * Constants.MIN_LOCAL_STORAGE_CLEARING_SIZE); // we try to reduce the space by 20%
                long messagesFreedSize = doTheCleanMessages(leastAccessMessages, neededSpaceToFreeUp / 2);
                long embeddedImagesFreedSize = doTheCleanEmbeddedImages(attachmentMetadataDao,
                        neededSpaceToFreeUp / 2);
                if (embeddedImagesFreedSize + messagesFreedSize < neededSpaceToFreeUp) {
                    if (embeddedImagesFreedSize < neededSpaceToFreeUp / 2) {
                        doTheCleanMessages(leastAccessMessages, (neededSpaceToFreeUp / 2) - embeddedImagesFreedSize);
                    } else if (messagesFreedSize < neededSpaceToFreeUp / 2) {
                        doTheCleanEmbeddedImages(attachmentMetadataDao,
                                (neededSpaceToFreeUp / 2) - messagesFreedSize);
                    }
                }
            } else if (ACTION_CLEAR_CACHE_IMMEDIATELY.equals(action)) {
                clearStorage();
            } else if (ACTION_CLEAR_CACHE_IMMEDIATELY_DELETE_TABLES.equals(action)) {
                String username = intent.getStringExtra(EXTRA_USERNAME);
                clearStorage();
                Context context = this;
                ContactsDatabaseFactory.Companion.deleteDb(context, username);
                MessagesDatabaseFactory.Companion.deleteDb(context, username);
                NotificationsDatabaseFactory.Companion.deleteDb(context, username);
                CounterDatabase.Companion.deleteDb(context, username);
                AttachmentMetadataDatabase.Companion.deleteDb(context, username);
                PendingActionsDatabaseFactory.Companion.deleteDb(context, username);
            }
    }

    private void clearStorage() {
        List<AttachmentMetadata> attachmentForDeletion = attachmentMetadataDao.getAllAttachmentsMetadata();
        for (AttachmentMetadata attachmentMetadata : attachmentForDeletion) {
            File file = new File(getApplicationContext().getFilesDir() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS, attachmentMetadata.getFolderLocation());
            if (file.exists()) {
                file.delete();
                attachmentMetadataDao.deleteAttachmentMetadata(attachmentMetadata);
            }
        }
        File directory = new File(getApplicationContext().getFilesDir() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS);
        deleteRecursive(directory);
        List<Message> leastAccessMessages = messageDetailsRepository.findAllMessageByLastMessageAccessTime(0);
        List<Message> messageListForDeletion = getMessageListForDeletion(leastAccessMessages, -1);
        for (Message message : messageListForDeletion) {
            message.setMessageBody("");
            message.setDownloaded(false);
        }
        messageDetailsRepository.saveAllMessages(messageListForDeletion);
    }

    private long doTheCleanMessages(List<Message> leastAccessMessages, long neededSpaceToFreeUp) {
        List<Message> messageListForDeletion = getMessageListForDeletion(leastAccessMessages, neededSpaceToFreeUp);
        long messagesFreedSize = 0;
        for (Message message : messageListForDeletion) {
            messagesFreedSize += message.getTotalSize();
            message.setMessageBody("");
            message.setDownloaded(false);
            messageDetailsRepository.saveMessageInDB(message);
        }
        return messagesFreedSize;
    }

    private long doTheCleanEmbeddedImages(AttachmentMetadataDao attachmentMetadataDao,
                                          long neededSpaceToFreeUp) {
        long embeddedImagesFreedSize = 0;
        List<AttachmentMetadata> attachmentForDeletion = getAttachmentListForDeletion(
                attachmentMetadataDao, neededSpaceToFreeUp);
        // delete the attachments
        for (AttachmentMetadata attachmentMetadata : attachmentForDeletion) {
            File file = new File(getApplicationContext().getFilesDir() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS, attachmentMetadata.getFolderLocation());
            if (file.exists()) {
                file.delete();
                embeddedImagesFreedSize += attachmentMetadata.getSize();
                attachmentMetadataDao.deleteAttachmentMetadata(attachmentMetadata);
            }
        }
        return embeddedImagesFreedSize;
    }
    //TODO move database to receiver after Kotlin
    private List<AttachmentMetadata> getAttachmentListForDeletion(
            AttachmentMetadataDao attachmentMetadataDao, long neededSpaceToFree) {
        List<AttachmentMetadata> attachmentMetadataList = attachmentMetadataDao.getAllAttachmentsMetadata();
        List<AttachmentMetadata> attachmentMetadataListForDeletion = new ArrayList<>();
        long size = 0;
        for (AttachmentMetadata attachmentMetadata : attachmentMetadataList) {
            size += attachmentMetadata.getSize();
            if (size >= neededSpaceToFree) {
                break;
            }
            attachmentMetadataListForDeletion.add(attachmentMetadata);
        }
        return attachmentMetadataListForDeletion;
    }

    private List<Message> getMessageListForDeletion(List<Message> messageList, long neededSpaceToFree) {
        long size = 0;
        List<Message> messageListForDeletion = new ArrayList<>();
        for (Message message : messageList) {
            size += message.getTotalSize();
            if (size >= neededSpaceToFree && neededSpaceToFree != -1) {
                break;
            }
            messageListForDeletion.add(message);
        }
        return messageListForDeletion;
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}

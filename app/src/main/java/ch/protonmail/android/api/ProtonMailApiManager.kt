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
package ch.protonmail.android.api

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.AttachmentUploadResponse
import ch.protonmail.android.api.models.AvailableDomainsResponse
import ch.protonmail.android.api.models.AvailablePlansResponse
import ch.protonmail.android.api.models.CheckSubscriptionBody
import ch.protonmail.android.api.models.CheckSubscriptionResponse
import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.ContactResponse
import ch.protonmail.android.api.models.ContactsDataResponse
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.CreateContactV2BodyItem
import ch.protonmail.android.api.models.CreateOrganizationBody
import ch.protonmail.android.api.models.CreatePaymentTokenBody
import ch.protonmail.android.api.models.CreatePaymentTokenSuccessResponse
import ch.protonmail.android.api.models.CreateSubscriptionBody
import ch.protonmail.android.api.models.CreateUpdateSubscriptionResponse
import ch.protonmail.android.api.models.DeleteContactResponse
import ch.protonmail.android.api.models.DirectEnabledResponse
import ch.protonmail.android.api.models.DonateBody
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.GetPaymentTokenResponse
import ch.protonmail.android.api.models.GetSubscriptionResponse
import ch.protonmail.android.api.models.HumanVerifyOptionsResponse
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.KeySalts
import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.KeysSetupBody
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.LoginInfoResponse
import ch.protonmail.android.api.models.LoginResponse
import ch.protonmail.android.api.models.MailSettingsResponse
import ch.protonmail.android.api.models.MailboxResetBody
import ch.protonmail.android.api.models.ModulusResponse
import ch.protonmail.android.api.models.MoveToFolderResponse
import ch.protonmail.android.api.models.OrganizationResponse
import ch.protonmail.android.api.models.PasswordVerifier
import ch.protonmail.android.api.models.PaymentMethodResponse
import ch.protonmail.android.api.models.PaymentMethodsResponse
import ch.protonmail.android.api.models.PaymentsStatusResponse
import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.RefreshBody
import ch.protonmail.android.api.models.RefreshResponse
import ch.protonmail.android.api.models.RegisterDeviceRequestBody
import ch.protonmail.android.api.models.ResetTokenResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.SinglePasswordChange
import ch.protonmail.android.api.models.SrpResponseBody
import ch.protonmail.android.api.models.TokenPaymentBody
import ch.protonmail.android.api.models.TwoFABody
import ch.protonmail.android.api.models.TwoFAResponse
import ch.protonmail.android.api.models.UnreadTotalMessagesResponse
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.api.models.UserSettingsResponse
import ch.protonmail.android.api.models.VerificationCodeBody
import ch.protonmail.android.api.models.VerifyBody
import ch.protonmail.android.api.models.VerifyResponse
import ch.protonmail.android.api.models.address.AddressSetupBody
import ch.protonmail.android.api.models.address.AddressSetupResponse
import ch.protonmail.android.api.models.address.AddressesResponse
import ch.protonmail.android.api.models.address.KeyActivationBody
import ch.protonmail.android.api.models.contacts.receive.ContactGroupsResponse
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.api.models.messages.receive.LabelsResponse
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.models.messages.send.MessageSendBody
import ch.protonmail.android.api.models.messages.send.MessageSendResponse
import ch.protonmail.android.api.models.requests.PasswordChange
import ch.protonmail.android.api.models.requests.PostHumanVerificationBody
import ch.protonmail.android.api.models.requests.UpgradePasswordBody
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.segments.address.AddressApiSpec
import ch.protonmail.android.api.segments.attachment.AttachmentApiSpec
import ch.protonmail.android.api.segments.authentication.AuthenticationApiSpec
import ch.protonmail.android.api.segments.connectivity.ConnectivityApiSpec
import ch.protonmail.android.api.segments.contact.ContactApiSpec
import ch.protonmail.android.api.segments.device.DeviceApiSpec
import ch.protonmail.android.api.segments.domain.DomainApiSpec
import ch.protonmail.android.api.segments.key.KeyApiSpec
import ch.protonmail.android.api.segments.label.LabelApiSpec
import ch.protonmail.android.api.segments.message.MessageApiSpec
import ch.protonmail.android.api.segments.organization.OrganizationApiSpec
import ch.protonmail.android.api.segments.payment.PaymentApiSpec
import ch.protonmail.android.api.segments.report.ReportApiSpec
import ch.protonmail.android.api.segments.reset.ResetApiSpec
import ch.protonmail.android.api.segments.settings.mail.MailSettingsApiSpec
import ch.protonmail.android.api.segments.settings.mail.UserSettingsApiSpec
import ch.protonmail.android.api.segments.user.UserApiSpec
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.ContactLabel
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import ch.protonmail.android.domain.entity.Id
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.Call
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class takes an API implementation and acts as a proxy. The real implementation is in the {@param api}
 * which can work directly with the Proton API or use any alternative proxy.
 */
@Singleton
class ProtonMailApiManager @Inject constructor(var api: ProtonMailApi) :
    BaseApi(),
    AddressApiSpec,
    AttachmentApiSpec,
    AuthenticationApiSpec,
    ConnectivityApiSpec,
    ContactApiSpec,
    DeviceApiSpec,
    KeyApiSpec,
    LabelApiSpec,
    MessageApiSpec,
    OrganizationApiSpec,
    PaymentApiSpec,
    ReportApiSpec,
    ResetApiSpec,
    UserSettingsApiSpec,
    MailSettingsApiSpec,
    UserApiSpec,
    DomainApiSpec {

    fun reset(newApi: ProtonMailApi) {
        api = newApi
    }

    fun getSecuredServices(): SecuredServices = api.securedServices

    // region routes and services
    override fun fetchAddressesBlocking(): AddressesResponse = api.fetchAddressesBlocking()

    override suspend fun fetchAddresses(): AddressesResponse = api.fetchAddresses()

    override fun fetchAddressesBlocking(userId: Id): AddressesResponse = api.fetchAddressesBlocking(userId)

    override fun updateAlias(addressIds: List<String>): ResponseBody = api.updateAlias(addressIds)

    override fun setupAddress(
        addressSetupBody: AddressSetupBody
    ): AddressSetupResponse = api.setupAddress(addressSetupBody)

    override fun editAddress(
        addressId: String,
        displayName: String,
        signature: String
    ): ResponseBody = api.editAddress(addressId, displayName, signature)

    override fun deleteAttachment(attachmentId: String): ResponseBody = api.deleteAttachment(attachmentId)


    override fun downloadAttachmentBlocking(attachmentId: String): ByteArray =
        api.downloadAttachmentBlocking(attachmentId)

    override fun downloadAttachmentBlocking(attachmentId: String, progressListener: ProgressListener): ByteArray =
        api.downloadAttachmentBlocking(attachmentId, progressListener)

    override suspend fun downloadAttachment(attachmentId: String): okhttp3.ResponseBody? =
        api.downloadAttachment(attachmentId)

    override fun uploadAttachmentInlineBlocking(
        attachment: Attachment,
        MessageID: String,
        contentID: String,
        KeyPackage: RequestBody,
        DataPackage: RequestBody,
        Signature: RequestBody
    ): AttachmentUploadResponse =
        api.uploadAttachmentInlineBlocking(attachment, MessageID, contentID, KeyPackage, DataPackage, Signature)

    override fun uploadAttachmentBlocking(
        attachment: Attachment,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse = api.uploadAttachmentBlocking(attachment, keyPackage, dataPackage, signature)

    override suspend fun uploadAttachmentInline(
        attachment: Attachment,
        messageID: String,
        contentID: String,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse =
        api.uploadAttachmentInline(attachment, messageID, contentID, keyPackage, dataPackage, signature)

    override suspend fun uploadAttachment(
        attachment: Attachment,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse = api.uploadAttachment(attachment, keyPackage, dataPackage, signature)

    override fun getAttachmentUrl(attachmentId: String): String = api.getAttachmentUrl(attachmentId)

    override fun revokeAccessBlocking(userId: Id): ResponseBody = api.revokeAccessBlocking(userId)

    override suspend fun revokeAccess(userId: Id): ResponseBody = api.revokeAccess(userId)

    override fun loginInfo(username: String): LoginInfoResponse = api.loginInfo(username)

    /**
     * This call strips out all user-specific headers so we get clean SRP session.
     */
    override fun loginInfoForAuthentication(
        username: String
    ): LoginInfoResponse = api.loginInfoForAuthentication(username)

    override fun login(
        username: String,
        srpSession: String,
        clientEphemeral: ByteArray,
        clientProof: ByteArray
    ): LoginResponse = api.login(username, srpSession, clientEphemeral, clientProof)

    override fun randomModulus(): ModulusResponse = api.randomModulus()

    override suspend fun refreshAuth(
        refreshBody: RefreshBody,
        userIdTag: UserIdTag?
    ): RefreshResponse = api.refreshAuth(refreshBody, userIdTag)

    override fun refreshAuthBlocking(
        refreshBody: RefreshBody,
        userIdTag: UserIdTag
    ): RefreshResponse = api.refreshAuthBlocking(refreshBody, userIdTag)

    override fun twoFactor(twoFABody: TwoFABody): TwoFAResponse = api.twoFactor(twoFABody)

    override suspend fun pingAsync(): ResponseBody = api.pingAsync()

    override suspend fun fetchContacts(
        page: Int,
        pageSize: Int
    ): ContactsDataResponse = api.fetchContacts(page, pageSize)

    override suspend fun fetchContactEmails(page: Int, pageSize: Int): ContactEmailsResponseV2 =
        api.fetchContactEmails(page, pageSize)

    override fun fetchContactsEmailsByLabelId(
        page: Int,
        labelId: String
    ): Observable<ContactEmailsResponseV2> = api.fetchContactsEmailsByLabelId(page, labelId)

    override fun fetchContactDetailsBlocking(
        contactId: String
    ): FullContactDetailsResponse? = api.fetchContactDetailsBlocking(contactId)

    override suspend fun fetchContactDetails(
        contactId: String
    ): FullContactDetailsResponse = api.fetchContactDetails(contactId)

    override fun fetchContactDetailsBlocking(
        contactIDs: Collection<String>
    ): Map<String, FullContactDetailsResponse?> = api.fetchContactDetailsBlocking(contactIDs)

    override fun createContactBlocking(body: CreateContact): ContactResponse? = api.createContactBlocking(body)

    override suspend fun createContact(body: CreateContact): ContactResponse? = api.createContact(body)

    override fun updateContact(contactId: String, body: CreateContactV2BodyItem): FullContactDetailsResponse? =
        api.updateContact(contactId, body)

    override fun deleteContactSingle(contactIds: IDList): Single<DeleteContactResponse> =
        api.deleteContactSingle(contactIds)

    override suspend fun deleteContact(contactIds: IDList): DeleteContactResponse = api.deleteContact(contactIds)

    override fun labelContacts(labelContactsBody: LabelContactsBody): Completable = api.labelContacts(labelContactsBody)

    override fun unlabelContactEmailsCompletable(labelContactsBody: LabelContactsBody): Completable =
        api.unlabelContactEmailsCompletable(labelContactsBody)

    override suspend fun unlabelContactEmails(labelContactsBody: LabelContactsBody) =
        api.unlabelContactEmails(labelContactsBody)

    override suspend fun registerDevice(
        registerDeviceRequestBody: RegisterDeviceRequestBody,
        userId: Id
    ) = api.registerDevice(registerDeviceRequestBody, userId)

    override suspend fun unregisterDevice(deviceToken: String) = api.unregisterDevice(deviceToken)

    override fun getPublicKeysBlocking(email: String): PublicKeyResponse = api.getPublicKeysBlocking(email)

    override suspend fun getPublicKeys(email: String): PublicKeyResponse = api.getPublicKeys(email)

    override fun getPublicKeys(emails: Collection<String>): Map<String, PublicKeyResponse?> = api.getPublicKeys(emails)

    override fun updatePrivateKeys(body: SinglePasswordChange): ResponseBody = api.updatePrivateKeys(body)

    override fun activateKey(
        keyActivationBody: KeyActivationBody,
        keyId: String
    ): ResponseBody = api.activateKey(keyActivationBody, keyId)

    override suspend fun activateKeyLegacy(
        keyActivationBody: KeyActivationBody,
        keyId: String
    ): ResponseBody = api.activateKeyLegacy(keyActivationBody, keyId)

    override fun setupKeys(keysSetupBody: KeysSetupBody): UserInfo = api.setupKeys(keysSetupBody)

    override fun fetchLabels(userIdTag: UserIdTag): LabelsResponse = api.fetchLabels(userIdTag)

    override fun fetchContactGroups(): Single<ContactGroupsResponse> = api.fetchContactGroups()

    override suspend fun fetchContactGroupsList(): List<ContactLabel> = api.fetchContactGroupsList()

    override fun fetchContactGroupsAsObservable(): Observable<List<ContactLabel>> = api.fetchContactGroupsAsObservable()

    override fun createLabel(label: LabelBody): LabelResponse = api.createLabel(label)

    override fun createLabelCompletable(label: LabelBody): Single<ContactLabel> = api.createLabelCompletable(label)

    override fun updateLabel(labelId: String, label: LabelBody): LabelResponse = api.updateLabel(labelId, label)

    override fun updateLabelCompletable(labelId: String, label: LabelBody): Completable =
        api.updateLabelCompletable(labelId, label)

    override fun deleteLabelSingle(labelId: String): Single<ResponseBody> = api.deleteLabelSingle(labelId)

    override suspend fun deleteLabel(labelId: String): ResponseBody = api.deleteLabel(labelId)

    override fun fetchMessagesCount(userIdTag: UserIdTag): UnreadTotalMessagesResponse =
        api.fetchMessagesCount(userIdTag)

    override fun messages(location: Int): MessagesResponse? = api.messages(location)

    override fun messages(location: Int, userIdTag: UserIdTag): MessagesResponse? = api.messages(location, userIdTag)

    override fun fetchMessages(location: Int, time: Long): MessagesResponse? = api.fetchMessages(location, time)

    override suspend fun fetchMessageMetadata(messageId: String, userIdTag: UserIdTag): MessagesResponse =
        api.fetchMessageMetadata(messageId, userIdTag)

    override fun markMessageAsRead(messageIds: IDList) = api.markMessageAsRead(messageIds)

    override fun markMessageAsUnRead(messageIds: IDList) = api.markMessageAsUnRead(messageIds)

    override suspend fun deleteMessage(messageIds: IDList) = api.deleteMessage(messageIds)

    override fun emptyDrafts() = api.emptyDrafts()

    override fun emptySpam() = api.emptySpam()

    override fun emptyTrash() = api.emptyTrash()

    override fun emptyCustomFolder(labelId: String) = api.emptyCustomFolder(labelId)

    override fun fetchMessageDetailsBlocking(messageId: String): MessageResponse =
        api.fetchMessageDetailsBlocking(messageId)

    override suspend fun fetchMessageDetails(messageId: String, userIdTag: UserIdTag): MessageResponse =
        api.fetchMessageDetails(messageId, userIdTag)

    override fun fetchMessageDetailsBlocking(messageId: String, userIdTag: UserIdTag): MessageResponse? =
        api.fetchMessageDetailsBlocking(messageId, userIdTag)

    override fun messageDetailObservable(
        messageId: String
    ): Observable<MessageResponse> = api.messageDetailObservable(messageId)

    override fun search(query: String, page: Int): MessagesResponse = api.search(query, page)

    override fun searchByLabelAndPage(
        query: String,
        page: Int
    ): MessagesResponse = api.searchByLabelAndPage(query, page)

    override fun searchByLabelAndTime(
        query: String,
        unixTime: Long
    ): MessagesResponse = api.searchByLabelAndTime(query, unixTime)

    override suspend fun createDraft(draftBody: DraftBody): MessageResponse = api.createDraft(draftBody)

    override suspend fun updateDraft(
        messageId: String,
        draftBody: DraftBody,
        userIdTag: UserIdTag
    ): MessageResponse = api.updateDraft(messageId, draftBody, userIdTag)

    override suspend fun sendMessage(
        messageId: String,
        message: MessageSendBody,
        userIdTag: UserIdTag
    ): MessageSendResponse = api.sendMessage(messageId, message, userIdTag)

    override fun unlabelMessages(idList: IDList) = api.unlabelMessages(idList)

    override fun labelMessages(body: IDList): MoveToFolderResponse? = api.labelMessages(body)

    override fun fetchOrganization(): OrganizationResponse = api.fetchOrganization()

    override fun fetchOrganizationKeys(): Keys = api.fetchOrganizationKeys()

    override fun createOrganization(body: CreateOrganizationBody): OrganizationResponse? = api.createOrganization(body)

    override suspend fun fetchSubscription(): GetSubscriptionResponse = api.fetchSubscription()

    override suspend fun fetchPaymentMethods(): PaymentMethodsResponse = api.fetchPaymentMethods()

    override suspend fun fetchPaymentsStatus(): PaymentsStatusResponse = api.fetchPaymentsStatus()

    override suspend fun checkSubscription(body: CheckSubscriptionBody): CheckSubscriptionResponse =
        api.checkSubscription(body)

    override fun donate(body: DonateBody): ResponseBody? = api.donate(body)

    override suspend fun createUpdateSubscription(body: CreateSubscriptionBody): CreateUpdateSubscriptionResponse =
        api.createUpdateSubscription(body)

    override suspend fun createUpdatePaymentMethod(body: TokenPaymentBody): PaymentMethodResponse =
        api.createUpdatePaymentMethod(body)

    override fun fetchAvailablePlans(
        currency: String,
        cycle: Int
    ): AvailablePlansResponse = api.fetchAvailablePlans(currency, cycle)

    override fun verifyPayment(body: VerifyBody): VerifyResponse = api.verifyPayment(body)

    override fun createPaymentToken(
        body: CreatePaymentTokenBody,
        token: String?,
        tokenType: String?
    ): Call<CreatePaymentTokenSuccessResponse> = api.createPaymentToken(body, token, tokenType)

    override fun getPaymentToken(token: String): Call<GetPaymentTokenResponse> = api.getPaymentToken(token)

    override fun reportBug(
        osName: String,
        appVersion: String,
        client: String,
        clientVersion: String,
        title: String,
        description: String,
        username: String,
        email: String
    ): ResponseBody = api.reportBug(osName, appVersion, client, clientVersion, title, description, username, email)

    override fun postPhishingReport(
        messageId: String,
        messageBody: String,
        mimeType: String
    ): ResponseBody? = api.postPhishingReport(messageId, messageBody, mimeType)

    override fun resetMailboxToken(): ResetTokenResponse? = api.resetMailboxToken()

    override fun resetMailbox(token: String, body: MailboxResetBody): ResponseBody? = api.resetMailbox(token, body)

    override fun fetchUserSettings(): UserSettingsResponse = api.fetchUserSettings()

    override fun fetchUserSettings(userId: Id): UserSettingsResponse = api.fetchUserSettings(userId)

    override fun updateNotify(updateNotify: Boolean): ResponseBody? = api.updateNotify(updateNotify)

    override fun updateNotificationEmail(
        srpSession: String,
        clientEpheremal: String,
        clientProof: String,
        twoFactorCode: String?,
        email: String
    ): SrpResponseBody? = api.updateNotificationEmail(srpSession, clientEpheremal, clientProof, twoFactorCode, email)

    override fun updateLoginPassword(
        passwordChangeBody: PasswordChange
    ): SrpResponseBody? = api.updateLoginPassword(passwordChangeBody)

    override fun upgradeLoginPassword(
        upgradePasswordBody: UpgradePasswordBody
    ): ResponseBody? = api.upgradeLoginPassword(upgradePasswordBody)

    @Deprecated(message = "Use non-blocking version of the function", replaceWith = ReplaceWith("fetchMailSettings()"))
    override fun fetchMailSettingsBlocking(): MailSettingsResponse = api.fetchMailSettingsBlocking()

    override suspend fun fetchMailSettings(): MailSettingsResponse = api.fetchMailSettings()

    override fun fetchMailSettingsBlocking(userId: Id): MailSettingsResponse =
        api.fetchMailSettingsBlocking(userId)

    override fun updateSignature(signature: String): ResponseBody? = api.updateSignature(signature)

    override fun updateDisplayName(displayName: String): ResponseBody? = api.updateDisplayName(displayName)

    override fun updateLeftSwipe(swipeSelection: Int): ResponseBody? = api.updateLeftSwipe(swipeSelection)

    override fun updateRightSwipe(swipeSelection: Int): ResponseBody? = api.updateRightSwipe(swipeSelection)

    override fun updateAutoShowImages(autoShowImages: Int): ResponseBody? = api.updateAutoShowImages(autoShowImages)

    override fun updateViewMode(viewMode: Int): ResponseBody? = api.updateViewMode(viewMode)

    override fun fetchUserInfoBlocking(): UserInfo = api.fetchUserInfoBlocking()

    override suspend fun fetchUserInfo(): UserInfo = api.fetchUserInfo()

    override fun fetchUserInfoBlocking(userId: Id): UserInfo = api.fetchUserInfoBlocking(userId)

    override fun fetchKeySalts(): KeySalts = api.fetchKeySalts()

    override fun fetchHumanVerificationOptions(): HumanVerifyOptionsResponse = api.fetchHumanVerificationOptions()

    override fun postHumanVerification(body: PostHumanVerificationBody): ResponseBody? = api.postHumanVerification(body)

    override fun createUser(
        username: String,
        password: PasswordVerifier,
        updateMe: Boolean,
        tokenType: String,
        token: String,
        timestamp: String,
        payload: String
    ): UserInfo = api.createUser(username, password, updateMe, tokenType, token, timestamp, payload)

    override fun sendVerificationCode(
        verificationCodeBody: VerificationCodeBody
    ): ResponseBody = api.sendVerificationCode(verificationCodeBody)

    override fun isUsernameAvailable(username: String): ResponseBody = api.isUsernameAvailable(username)

    override fun fetchDirectEnabled(): DirectEnabledResponse = api.fetchDirectEnabled()

    override fun fetchAvailableDomains(): AvailableDomainsResponse = api.fetchAvailableDomains()
    // endregion
}

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

import android.util.Base64;

import com.birbit.android.jobqueue.Params;

import java.util.ArrayList;
import java.util.List;

import ch.protonmail.android.api.TokenManager;
import ch.protonmail.android.api.models.Keys;
import ch.protonmail.android.api.models.LoginInfoResponse;
import ch.protonmail.android.api.models.MailSettingsResponse;
import ch.protonmail.android.api.models.ModulusResponse;
import ch.protonmail.android.api.models.OrganizationResponse;
import ch.protonmail.android.api.models.PasswordVerifier;
import ch.protonmail.android.api.models.PrivateKeyBody;
import ch.protonmail.android.api.models.RefreshBody;
import ch.protonmail.android.api.models.RefreshResponse;
import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.models.SinglePasswordChange;
import ch.protonmail.android.api.models.SrpResponseBody;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.UserInfo;
import ch.protonmail.android.api.models.UserSettingsResponse;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.address.AddressKeyActivationWorker;
import ch.protonmail.android.api.models.address.AddressesResponse;
import ch.protonmail.android.api.models.requests.PasswordChange;
import ch.protonmail.android.api.services.LoginService;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.AuthStatus;
import ch.protonmail.android.events.PasswordChangeEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.ConstantTime;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.SRPClient;
import ch.protonmail.android.utils.crypto.OpenPGP;

import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_NEW_PASSWORD_INCORRECT;
import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_NEW_PASSWORD_MESSED_UP;
import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_OLD_PASSWORD_INCORRECT;

public class ChangePasswordJob extends ProtonMailBaseJob{

    private static final String TAG_CHANGE_PASSWORD_JOB = "ChangePasswordJob";

    private int passwordType;
    private Constants.PasswordMode passwordMode;
    private byte[] oldPassword;
    private String twoFactorCode;
    private final byte[] newPassword;
    private byte[] loginPasswordForKeyChange;

    public ChangePasswordJob(int passwordType, Constants.PasswordMode passwordMode, byte[] oldPassword, String twoFactorCode, byte[] newPassword){
        super(new Params(Priority.HIGH).requireNetwork());
        this.passwordType = passwordType;
        this.passwordMode = passwordMode;
        this.oldPassword = oldPassword;
        this.twoFactorCode = twoFactorCode;
        this.newPassword = newPassword;
        this.loginPasswordForKeyChange = oldPassword;
    }
    @Override
    public void onRun() throws Throwable {
        TokenManager tokenManager = mUserManager.getTokenManager();
        OpenPGP openPGP = ProtonMailApplication.getApplication().getOpenPGP();
        User user = mUserManager.getUser();
        if (passwordMode == Constants.PasswordMode.DUAL) {
            if (passwordType == Constants.PASSWORD_TYPE_LOGIN) {
                //region dual password mode change login password
                final LoginInfoResponse infoResponse = mApi.loginInfo(mUserManager.getUsername());
                final ModulusResponse newModulus = mApi.randomModulus();
                final PasswordVerifier verifier = PasswordVerifier.calculate(newPassword, newModulus);
                final SRPClient.Proofs proofs = LoginService.srpProofsForInfo(mUserManager.getUsername(), oldPassword, infoResponse, 2);
                final SrpResponseBody response = mApi.updateLoginPassword(new PasswordChange(infoResponse.getSRPSession(), ConstantTime.encodeBase64(proofs.clientEphemeral, true), ConstantTime.encodeBase64(proofs.clientProof, true), twoFactorCode, verifier));

                if (response.getCode() == RESPONSE_CODE_OLD_PASSWORD_INCORRECT || response.getCode() == RESPONSE_CODE_NEW_PASSWORD_INCORRECT || response.getCode() == RESPONSE_CODE_NEW_PASSWORD_MESSED_UP) {
                    AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.INVALID_CREDENTIAL, response.getError()));
                } else if (!ConstantTime.isEqual(Base64.decode(response.getServerProof(), Base64.DEFAULT), proofs.expectedServerProof)) {
                    AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.INVALID_SERVER_PROOF, response.getError()));
                } else {
                    AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.SUCCESS, response.getError()));
                }
                //endregion
            } else if (passwordType == Constants.PASSWORD_TYPE_MAILBOX) {
                //region dual password mode change mailbox password
                final LoginInfoResponse infoResponse = mApi.loginInfo(mUserManager.getUsername());

                final String keySalt = openPGP.createNewKeySalt();
                final byte[] generatedMailboxPassword = openPGP.generateMailboxPassword(keySalt, newPassword);

                final SRPClient.Proofs proofs = LoginService.srpProofsForInfo(mUserManager.getUsername(), loginPasswordForKeyChange, infoResponse, 2);

                final List<Address> userAddresses = user.getAddresses();
                ArrayList<PrivateKeyBody> privateKeyBodies = new ArrayList<>();
                List<Keys> userKeys = user.getKeys();
                for (Keys keys : userKeys) {
                    try {
                        PrivateKeyBody privateKeyBody = updateKey(keys, generatedMailboxPassword, openPGP,false);
                        if (privateKeyBody != null) {
                            privateKeyBodies.add(privateKeyBody);
                        }
                    } catch (Exception e){
                        //should catch keys that are not "decryptable" with the old mailbox password
                        Logger.doLogException(TAG_CHANGE_PASSWORD_JOB, e);
                    }
                }
                for (Address address : userAddresses) {
                    List<Keys> keysList = address.getKeys();
                    for (int i = 0; i < keysList.size(); i++) {
                        try {
                            Keys keys = keysList.get(i);
                            PrivateKeyBody privateKeyBody = updateKey(keys, generatedMailboxPassword, openPGP,i == 0);
                            if (privateKeyBody != null) {
                                privateKeyBodies.add(privateKeyBody);
                            }
                        } catch (Exception e){
                            //should catch keys that are not "decryptable" with the old mailbox password
                            Logger.doLogException(TAG_CHANGE_PASSWORD_JOB, e);
                        }
                    }
                }
                Keys keysResponse = null;
                if (mUserManager.getUser().isPaidUser()) {
                    OrganizationResponse organizationResponse = mApi.fetchOrganization();
                    if (organizationResponse.getCode() == Constants.RESPONSE_CODE_OK) {
                        keysResponse = mApi.fetchOrganizationKeys();
                    }
                }
                String newOrganizationPrivateKey = "";

                if (keysResponse != null && openPGP.checkPassphrase(keysResponse.getPrivateKey(), mUserManager.getMailboxPassword())) {
                    newOrganizationPrivateKey = openPGP.updatePrivateKeyPassphrase(keysResponse.getPrivateKey(), mUserManager.getMailboxPassword(), generatedMailboxPassword);
                } else if (keysResponse != null) {
                    newOrganizationPrivateKey = null;
                }
                PrivateKeyBody[] privateKeyArray = privateKeyBodies.toArray(new PrivateKeyBody[privateKeyBodies.size()]);
                ResponseBody response = mApi.updatePrivateKeys(
                            new SinglePasswordChange(keySalt, privateKeyArray, newOrganizationPrivateKey, infoResponse
                                    .getSRPSession(), ConstantTime.encodeBase64(proofs.clientEphemeral, true),
                                    ConstantTime.encodeBase64(proofs.clientProof, true), twoFactorCode));

                if (response.getCode() == Constants.RESPONSE_CODE_OK) {
                    UserInfo userInfo = mApi.fetchUserInfo();
                    UserSettingsResponse userSettings = mApi.fetchUserSettings();
                    MailSettingsResponse mailSettings = mApi.fetchMailSettings();
                    AddressesResponse addresses = mApi.fetchAddresses();
                    if (userInfo.getCode() != Constants.RESPONSE_CODE_OK) {
                        AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.FAILED, response.getError()));
                    } else {
                        mUserManager.setUserInfo(userInfo, mailSettings.getMailSettings(), userSettings.getUserSettings(), addresses.getAddresses());
                        mUserManager.saveMailboxPassword(generatedMailboxPassword, mUserManager.getUsername());
                        AddressKeyActivationWorker.Companion.activateAddressKeysIfNeeded(getApplicationContext(), addresses.getAddresses(), user.getUsername());
                        final RefreshBody refreshBody = tokenManager.createRefreshBody();
                        final RefreshResponse refreshResponse = mApi.refreshSync(refreshBody);
                        if (refreshResponse.getAccessToken() != null) {
                            tokenManager.handleRefresh(refreshResponse);
                        }
                        AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.SUCCESS, response.getError()));
                    }
                } else {
                    AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.FAILED, response.getError()));
                }
                //endregion
            }
        } else {
            //region single password mode change password
            final LoginInfoResponse infoResponse = mApi.loginInfo(mUserManager.getUsername());
            final ModulusResponse newModulus = mApi.randomModulus();

            final String keySalt = openPGP.createNewKeySalt();
            final byte[] generatedMailboxPassword = openPGP.generateMailboxPassword(keySalt, newPassword);

            final PasswordVerifier verifier = PasswordVerifier.calculate(newPassword, newModulus);
            final SRPClient.Proofs proofs = LoginService.srpProofsForInfo(mUserManager.getUsername(), oldPassword, infoResponse, 2);

            final List<Address> userAddresses = user.getAddresses();
            ArrayList<PrivateKeyBody> privateKeyBodies = new ArrayList<>();
            List<Keys> userKeys = user.getKeys();
            for (Keys keys : userKeys) {
                try {
                    PrivateKeyBody privateKeyBody = updateKey(keys, generatedMailboxPassword, openPGP,false);
                    if (privateKeyBody == null) {
                        continue;
                    }
                    privateKeyBodies.add(privateKeyBody);
                } catch (Exception e){
                    //should catch keys that are not "decryptable" with the old mailbox password
                    Logger.doLogException(TAG_CHANGE_PASSWORD_JOB, e);
                }
            }
            for (Address address : userAddresses) {
                List<Keys> keysList = address.getKeys();
                for (int i = 0; i < keysList.size(); i++) {
                    try {
                        Keys keys = keysList.get(i);
                        PrivateKeyBody privateKeyBody = updateKey(keys, generatedMailboxPassword, openPGP,i == 0);
                        if (privateKeyBody != null) {
                            privateKeyBodies.add(privateKeyBody);
                        }
                    } catch (Exception e){
                        //should catch keys that are not "decryptable" with the old mailbox password
                        Logger.doLogException(TAG_CHANGE_PASSWORD_JOB, e);
                    }
                }
            }
            Keys keysResponse = null;
            if (mUserManager.getUser().isPaidUser()) {
                OrganizationResponse organizationResponse = mApi.fetchOrganization();
                if (organizationResponse.getCode() == Constants.RESPONSE_CODE_OK) {
                    keysResponse = mApi.fetchOrganizationKeys();
                }
            }
            String newOrganizationPrivateKey = "";
            if (keysResponse != null && openPGP.checkPassphrase(keysResponse.getPrivateKey(), mUserManager.getMailboxPassword())) {
                newOrganizationPrivateKey = openPGP.updatePrivateKeyPassphrase(keysResponse.getPrivateKey(), mUserManager.getMailboxPassword(), generatedMailboxPassword);
            } else if (keysResponse != null) {
                newOrganizationPrivateKey = null;
            }

            PrivateKeyBody[] privateKeyArray = privateKeyBodies.toArray(new PrivateKeyBody[privateKeyBodies.size()]);
            ResponseBody response = mApi.updatePrivateKeys(
                        new SinglePasswordChange(keySalt, privateKeyArray, newOrganizationPrivateKey, infoResponse
                                .getSRPSession(), ConstantTime.encodeBase64(proofs.clientEphemeral, true),
                                ConstantTime.encodeBase64(proofs.clientProof, true), twoFactorCode, verifier));
            if (response.getCode() == Constants.RESPONSE_CODE_OK) {
                UserInfo userInfo = mApi.fetchUserInfo();
                UserSettingsResponse userSettings = mApi.fetchUserSettings();
                MailSettingsResponse mailSettings = mApi.fetchMailSettings();
                AddressesResponse addresses = mApi.fetchAddresses();
                if (userInfo.getCode() != Constants.RESPONSE_CODE_OK) {
                    AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.FAILED, response.getError()));
                } else {
                    mUserManager.setUserInfo(userInfo, mailSettings.getMailSettings(), userSettings.getUserSettings(), addresses.getAddresses());
                    mUserManager.saveMailboxPassword(generatedMailboxPassword, mUserManager.getUsername());
                    AddressKeyActivationWorker.Companion.activateAddressKeysIfNeeded(getApplicationContext(), addresses.getAddresses(), user.getUsername());
                    final RefreshBody refreshBody = tokenManager.createRefreshBody();
                    final RefreshResponse refreshResponse = mApi.refreshSync(refreshBody);
                    if (refreshResponse.getAccessToken() != null) {
                        tokenManager.handleRefresh(refreshResponse);
                    }
                    AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.SUCCESS, response.getError()));
                }
            } else {
                if (response.getCode() == RESPONSE_CODE_OLD_PASSWORD_INCORRECT ||
                        response.getCode() == RESPONSE_CODE_NEW_PASSWORD_INCORRECT ||
                        response.getCode() == RESPONSE_CODE_NEW_PASSWORD_MESSED_UP) {
                    AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.INVALID_CREDENTIAL, response.getError()));
                } else {
                    AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.FAILED, response.getError()));
                }
            }
            //endregion
        }
    }

    private PrivateKeyBody updateKey(Keys keys, byte[] generatedMailboxPassword, OpenPGP openPGP, boolean throwOnError) {
        try {
            String newPrivateKey = openPGP.updatePrivateKeyPassphrase(keys.getPrivateKey(), mUserManager.getMailboxPassword(), generatedMailboxPassword);
            return new PrivateKeyBody(newPrivateKey, keys.getID());
        } catch (Exception e) {
            if (throwOnError) {
                AppUtil.postEventOnUi(new PasswordChangeEvent(passwordType, AuthStatus.FAILED));
            }
        }
        return null;
    }

}

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
package ch.protonmail.android.contacts.details;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseActivity;
import ch.protonmail.android.activities.UpsellingActivity;
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity;
import ch.protonmail.android.activities.contactDetails.ExtractFullContactDetailsTask;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.api.models.ContactEncryptedData;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.room.contacts.ContactLabel;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.models.room.contacts.FullContactDetails;
import ch.protonmail.android.contacts.ErrorEnum;
import ch.protonmail.android.contacts.ErrorResponse;
import ch.protonmail.android.contacts.details.edit.EditContactDetailsActivity;
import ch.protonmail.android.crypto.CipherText;
import ch.protonmail.android.crypto.Crypto;
import ch.protonmail.android.crypto.UserCrypto;
import ch.protonmail.android.events.ContactEvent;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.usecase.model.FetchContactDetailsResult;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.DateUtil;
import ch.protonmail.android.utils.FileHelper;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.VCardUtil;
import ch.protonmail.android.utils.crypto.TextDecryptionResult;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.views.CustomFontButton;
import ch.protonmail.android.views.CustomFontTextView;
import ch.protonmail.android.views.VCardLinearLayout;
import ch.protonmail.android.views.contactDetails.ContactAvatarView;
import dagger.hilt.android.AndroidEntryPoint;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Anniversary;
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.FormattedName;
import ezvcard.property.Gender;
import ezvcard.property.Nickname;
import ezvcard.property.Note;
import ezvcard.property.Organization;
import ezvcard.property.Photo;
import ezvcard.property.Role;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Url;
import ezvcard.util.PartialDate;
import kotlin.Unit;
import timber.log.Timber;

import static ch.protonmail.android.usecase.create.CreateContactKt.VCARD_TEMP_FILE_NAME;
import static ch.protonmail.android.views.contactDetails.ContactAvatarViewKt.TYPE_INITIALS;
import static ch.protonmail.android.views.contactDetails.ContactAvatarViewKt.TYPE_PHOTO;

@AndroidEntryPoint
public class ContactDetailsActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener {

    public static final String EXTRA_CONTACT = "extra_contact";
    private static final int REQUEST_CODE_EDIT_CONTACT = 1;
    private static final int REQUEST_CODE_UPGRADE = 2;

    @BindView(R.id.animToolbar)
    Toolbar toolbar;
    @BindView(R.id.contactCollapsedTitleContainer)
    LinearLayout contactCollapsedTitleContainer;
    @BindView(R.id.contactCollapsedTitle)
    TextView contactCollapsedTitle;
    @BindView(R.id.collapsingToolbar)
    CollapsingToolbarLayout collapsingToolbar;
    @BindView(R.id.contactTitle)
    TextView contactTitle;
    @BindView(R.id.contactAvatar)
    ContactAvatarView contactAvatar;
    @BindView(R.id.appbar)
    AppBarLayout appBarLayout;
    @BindView(R.id.emailAddressesContainer)
    VCardLinearLayout mAddressesContainer;
    @BindView(R.id.encryptedDataContainer)
    LinearLayout mEncryptedDataContainer;
    @BindView(R.id.contactDetailsProgressBar)
    View progressBar;
    @BindView(R.id.upgradeEncryptedStub)
    ViewStub mUpgradeEncryptedStub;
    @BindView(R.id.emptyEncryptedStub)
    ViewStub mEmptyEncryptedStub;
    @BindView(R.id.errorEncryptedStub)
    ViewStub mErrorEncryptedStub;
    @BindView(R.id.top_panel)
    View mTopPanel;
    @BindView(R.id.top_panel_verification_error)
    View mTopPanelVerificationError;
    @BindView(R.id.bottom_panel_verification_error)
    ViewStub mBottomPanelVerificationErrorStub;
    @BindView(R.id.cardViewBottomPart)
    View mCardViewBottomPart;
    @BindView(R.id.encryptedContactTitle)
    TextView mEncryptedContactTitleView;
    View mBottomPanelErrorView;

    @BindView(R.id.fabMail)
    ImageButton fabMail;
    @BindView(R.id.fabPhone)
    ImageButton fabPhone;
    @BindView(R.id.fabWeb)
    ImageButton fabWeb;

    @Inject
    FileHelper fileHelper;

    private ContactsDatabase contactsDatabase;
    private User mUser;
    private LayoutInflater inflater;
    private String mContactId;
    private String mVCardType0;
    private String mVCardType2;
    private String mVCardType3;
    private String mVCardType2Signature;
    private String mVCardType3Signature;
    private View mEmptyEncryptedView;
    private View mErrorEncryptedView;
    private List<String> mVCardEmailUIOptions;
    private List<String> mVCardEmailOptions;
    private List<String> mVCardPhoneUIOptions;
    private List<String> mVCardPhoneOptions;
    private List<String> mVCardAddressUIOptions;
    private List<String> mVCardAddressOptions;
    private Menu collapsedMenu;
    private ContactDetailsViewModel viewModel;
    private ContactEditDetailsEmailGroupsAdapter contactEditDetailsEmailGroupsAdapter;
    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.9f;
    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS = 0.3f;
    private static final int ALPHA_ANIMATIONS_DURATION = 200;
    private boolean mIsTheTitleVisible = false;
    private boolean mIsTheTitleContainerVisible = true;
    private boolean isDropDownShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactsDatabase = ContactsDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        viewModel = new ViewModelProvider(this).get(ContactDetailsViewModel.class);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mUser = mUserManager.getUser();
        if (!mUser.isPaidUser()) {
            View mUpgradeView = mUpgradeEncryptedStub.inflate();
            mUpgradeView.findViewById(R.id.upgrade).setOnClickListener(mUpgradeClickListener);
        }

        Bundle extras = getIntent().getExtras();
        inflater = LayoutInflater.from(this);
        mContactId = extras.getString(EXTRA_CONTACT);
        mVCardEmailUIOptions = Arrays.asList(getResources().getStringArray(R.array.vcard_option_email));
        mVCardEmailOptions = Arrays.asList(getResources().getStringArray(R.array.vcard_option_email_val));
        mVCardPhoneUIOptions = Arrays.asList(getResources().getStringArray(R.array.vcard_option_phone));
        mVCardPhoneOptions = Arrays.asList(getResources().getStringArray(R.array.vcard_option_phone_val));
        mVCardAddressUIOptions = Arrays.asList(getResources().getStringArray(R.array.vcard_option_address));
        mVCardAddressOptions = Arrays.asList(getResources().getStringArray(R.array.vcard_option_address_val));
        contactEditDetailsEmailGroupsAdapter = new ContactEditDetailsEmailGroupsAdapter(ContactDetailsActivity.this, new ArrayList<>());
        viewModel.getSetupComplete().observe(this, booleanEvent -> {
            Boolean didSetupCompleteBoxed = booleanEvent.getContentIfNotHandled();
            boolean didSetupComplete;
            if (didSetupCompleteBoxed != null) didSetupComplete = didSetupCompleteBoxed;
            else didSetupComplete = false;

            if (didSetupComplete) {
                new ExtractFullContactDetailsTask(contactsDatabase, mContactId, fullContactDetails -> {
                    onInitialiseContact(fullContactDetails);
                    return Unit.INSTANCE;
                }).execute();
            }
        });
        viewModel.getSetupError().observe(this, event -> {
            ErrorResponse error = new ErrorResponse("", ErrorEnum.DEFAULT);
            if (event != null) {
                error = event.getContentIfNotHandled();
            }
            if (error != null) {
                TextExtensions.showToast(ContactDetailsActivity.this,
                        error.getMessage(ContactDetailsActivity.this));
            }
        });

        viewModel.getProfilePicture().observe(this, observer -> {
            observer.doOnData(bitmap -> {
                contactAvatar.setAvatarType(TYPE_PHOTO);
                contactAvatar.setImage(bitmap);
                return Unit.INSTANCE;
            });
            observer.doOnError(error -> {
                contactAvatar.setName(mDisplayName);
                contactAvatar.setAvatarType(TYPE_INITIALS);
                TextExtensions.showToast(this, error);
                return Unit.INSTANCE;
            });

            return Unit.INSTANCE;
        });

        viewModel.getContactDetailsFetchResult().observe(
                this,
                this::onContactDetailsLoadedEvent
        );

        viewModel.fetchContactGroupsAndContactEmails(mContactId);
        appBarLayout.addOnOffsetChangedListener(this);
        startAlphaAnimation(contactCollapsedTitle, 0, View.INVISIBLE);
    }

    void onInitialiseContact(FullContactDetails fullContactDetails) {
        if (fullContactDetails == null || fullContactDetails.getEncryptedData() == null || fullContactDetails.getEncryptedData().size() == 0) {
            viewModel.fetchDetails(mContactId);
        } else {
            decryptAndFillVCard(fullContactDetails);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApp.getBus().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mApp.getBus().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_details_menu, menu);
        collapsedMenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(collapsedMenu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_CONTACT && resultCode == RESULT_OK) {
            updateDisplayedContact();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (itemId == R.id.action_delete) {
            DialogInterface.OnClickListener clickListener = (dialog, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    viewModel.deleteContact(mContactId);
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 500);
                }
                dialog.dismiss();
            };
            if (!isFinishing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.confirm)
                        .setMessage(String.format(getString(R.string.delete_contact), mDisplayName))
                        .setNegativeButton(R.string.no, clickListener)
                        .setPositiveButton(R.string.yes, clickListener)
                        .create()
                        .show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        saveLastInteraction();
        finish();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_contact_details;
        // TODO change to activity_contact_details_new. Some adjustments are needed
    }

    private void updateDisplayedContact() {
        new ExtractFullContactDetailsTask(contactsDatabase, mContactId, fullContactDetails -> {
            decryptAndFillVCard(fullContactDetails);
            onEditContact(fullContactDetails, mContactId);
            return Unit.INSTANCE;
        }).execute();
    }

    private void onEditContact(FullContactDetails fullContactDetails, String contactId) {
        if (fullContactDetails == null && !TextUtils.isEmpty(contactId)) {
            viewModel.fetchDetails(contactId);
        } else if (fullContactDetails == null && TextUtils.isEmpty(contactId)) {
            onBackPressed();
        } else if (fullContactDetails != null) {
            refresh();
        }
    }

    private void decryptAndFillVCard(@Nullable FullContactDetails contact) {
        boolean hasDecryptionError = false;

        Crypto crypto = Crypto.forUser(mUserManager, mUserManager.getUsername());
        List<ContactEncryptedData> encData = new ArrayList<>();
        if (contact != null && contact.getEncryptedData() != null) {
            encData = contact.getEncryptedData();
        } else {
            hasDecryptionError = true;
        }

        for (ContactEncryptedData contactEncryptedData : encData) {
            if (contactEncryptedData.getType() == 0) {
                mVCardType0 = contactEncryptedData.getData();

            } else if (contactEncryptedData.getType() == 2) {
                mVCardType2 = contactEncryptedData.getData();
                mVCardType2Signature = contactEncryptedData.getSignature();
            } else if (contactEncryptedData.getType() == 3) {
                try {
                    CipherText tct = new CipherText(contactEncryptedData.getData());
                    TextDecryptionResult tdr = crypto.decrypt(tct);
                    mVCardType3 = tdr.getDecryptedData();
                } catch (Exception e) {
                    hasDecryptionError = true;
                    Logger.doLogException(e);
                }
                mVCardType3Signature = contactEncryptedData.getSignature();
            }
        }
        fillVCard(hasDecryptionError);
    }

    @SuppressLint("ClickableViewAccessibility")
    private View createEmailRow(String type, final String email) {

        View emailRowView = inflater.inflate(R.layout.contact_email_item_w_edit_button_v2, mAddressesContainer, false);
        TextView titleView = emailRowView.findViewById(R.id.emailTitle);
        TextView emailView = emailRowView.findViewById(R.id.email);
        LinearLayout groupsView = emailRowView.findViewById(R.id.groupsView);
        ImageButton groupsOption = emailRowView.findViewById(R.id.groupsOption);
        RelativeLayout rowWrapper = emailRowView.findViewById(R.id.rowWrapper);
        if (mUser.isPaidUser()) {
            groupsView.setVisibility(View.VISIBLE);
            groupsOption.setVisibility(View.VISIBLE);
            CustomFontTextView menuHeader = emailRowView.findViewById(R.id.menuHeader);

            rowWrapper.setOnClickListener(v -> {
                rowWrapper.setOnTouchListener(null);
                PopupWindow groupsDropDown = initDialog(email);
                if (isDropDownShowing) {
                    isDropDownShowing = false;
                } else {
                    menuHeader.setVisibility(View.VISIBLE);
                    groupsView.setVisibility(View.GONE);
                    groupsOption.setImageResource(R.drawable.triangle_up);
                    // Handler used for Android 6 issue workaround
                    new Handler().postDelayed(() -> {
                        if (!isFinishing()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                groupsDropDown.showAsDropDown(rowWrapper, 0, 0, Gravity.CENTER);
                            } else {
                                groupsDropDown.showAsDropDown(rowWrapper, 0, 0);
                            }
                            isDropDownShowing = true;
                        }
                    }, 100);
                }
                groupsDropDown.setOnDismissListener(() -> {
                    menuHeader.setVisibility(View.GONE);
                    groupsView.setVisibility(View.VISIBLE);
                    groupsOption.setImageResource(R.drawable.triangle_down);


                    rowWrapper.setOnTouchListener((c, event) -> {
                        isDropDownShowing = true;
                        return false;
                    });
                });
            });
        }

        emailView.setText(email);
        emailView.setSelected(true);
        ImageButton writeEmailButton = emailRowView.findViewById(R.id.btnCompose);
        writeEmailButton.setVisibility(View.VISIBLE);
        titleView.setText(type);
        writeEmailButton.setOnClickListener(v -> {
            final Intent intent = AppUtil.decorInAppIntent(new Intent(ContactDetailsActivity.this, ComposeMessageActivity.class));
            intent.putExtra(ComposeMessageActivity.EXTRA_TO_RECIPIENTS, new String[]{email});
            startActivity(intent);
        });
        return emailRowView;
    }

    private View createNewEncryptedItemRow(final String title, final String value) {

        return createNewEncryptedItemRow(title, value, true, 2);
    }

    @SuppressLint("ClickableViewAccessibility")
    private View createNewEncryptedItemRow(final String title, final String value, boolean singleLine, int minLines) {

        View emailRowView = inflater.inflate(R.layout.contact_email_item_w_edit_button_v2, mEncryptedDataContainer, false);
        TextView titleView = emailRowView.findViewById(R.id.emailTitle);
        TextView emailView = emailRowView.findViewById(R.id.email);
        final ImageView titleIcon = emailRowView.findViewById(R.id.titleIcon);
        if (minLines == 1) {
            titleIcon.setImageResource(R.drawable.ic_contact_phone);
        } else if (minLines == 4) {
            titleIcon.setImageResource(R.drawable.ic_contact_note);
        } else {
            titleIcon.setImageResource(R.drawable.ic_contact_title);
        }
        titleIcon.setColorFilter(getResources().getColor(R.color.contact_heading));
        emailView.setSingleLine(singleLine);
        if (!singleLine) {
            emailView.setMinLines(minLines);
        }
        emailView.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                if (!TextUtils.isEmpty(value)) {
                    copyValueToClipboard(title, value);
                }
                return true;
            }
            return false;
        });
        titleView.setText(title);
        emailView.setText(value);
        return emailRowView;
    }

    @SuppressLint("ClickableViewAccessibility")
    private View createNewEncryptedItemRow(final String title, final Address address) {

        View addressRowView = inflater.inflate(R.layout.contact_email_item_w_edit_button_v2, mEncryptedDataContainer, false);
        final TextView addressFullCombined = addressRowView.findViewById(R.id.email);
        final TextView titleView = addressRowView.findViewById(R.id.emailTitle);
        final ImageView titleIcon = addressRowView.findViewById(R.id.titleIcon);
        titleIcon.setImageResource(R.drawable.ic_contact_address);
        titleIcon.setColorFilter(getResources().getColor(R.color.contact_heading));
        String street = address.getStreetAddress();
        String locality = address.getLocality();
        String region = address.getRegion();
        String postalCode = address.getPostalCode();
        String country = address.getCountry();
        List<String> addressParts = new ArrayList<>();
        if (!TextUtils.isEmpty(street)) {
            addressParts.add(street);
        }
        if (!TextUtils.isEmpty(locality)) {
            addressParts.add(locality);
        }
        if (!TextUtils.isEmpty(region)) {
            addressParts.add(region);
        }
        if (!TextUtils.isEmpty(postalCode)) {
            addressParts.add(postalCode);
        }
        if (!TextUtils.isEmpty(country)) {
            addressParts.add(country);
        }
        titleView.setText(title);
        final String value = TextUtils.join(" ", addressParts);
        addressFullCombined.setText(value);
        addressFullCombined.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                if (!TextUtils.isEmpty(value)) {
                    copyValueToClipboard(title, value);
                }
                return true;
            }
            return false;
        });
        return addressRowView;
    }

    @Subscribe
    public void onContactEvent(ContactEvent event) {

        switch (event.status) {
            case ContactEvent.SUCCESS:
            case ContactEvent.SAVED:
                updateDisplayedContact();
                break;
            case ContactEvent.ALREADY_EXIST:
                TextExtensions.showToast(this, R.string.contact_exist);
                break;
            case ContactEvent.INVALID_EMAIL:
                TextExtensions.showToast(this, R.string.invalid_email);
                break;
            case ContactEvent.DUPLICATE_EMAIL:
                TextExtensions.showToast(this, R.string.duplicate_email);
                break;
            default:
                TextExtensions.showToast(this, R.string.contact_saved_offline);
                saveLastInteraction();
                finish();
                break;
            case ContactEvent.ERROR:
            case ContactEvent.NOT_ALL_SYNC:
            case ContactEvent.NO_NETWORK:
                break;
        }
    }


    public void onContactDetailsLoadedEvent(FetchContactDetailsResult result) {
        Timber.v("FetchContactDetailsResult received");
        if (result instanceof FetchContactDetailsResult.Data) {
            if (mErrorEncryptedView != null) {
                mErrorEncryptedView.setVisibility(View.GONE);
            }
            FetchContactDetailsResult.Data data = (FetchContactDetailsResult.Data) result;
            mVCardType0 = data.getDecryptedVCardType0();
            mVCardType2 = data.getDecryptedVCardType2();
            mVCardType3 = data.getDecryptedVCardType3();
            mVCardType2Signature = data.getVCardType2Signature();
            mVCardType3Signature = data.getVCardType3Signature();
            fillVCard(false);
        } else if (result instanceof FetchContactDetailsResult.Error) {
            FetchContactDetailsResult.Error error = (FetchContactDetailsResult.Error) result;
            Timber.w(error.getException(), "Fetch contact details error");
            hideProgress();
            if (mErrorEncryptedView != null) {
                mErrorEncryptedView.setVisibility(View.VISIBLE);
                return;
            }
            mErrorEncryptedView = mErrorEncryptedStub.inflate();
            mErrorEncryptedView.findViewById(R.id.retry).setOnClickListener(v -> {
                progressBar.setVisibility(View.VISIBLE);
                viewModel.fetchDetails(mContactId);
            });
        }
    }

    private void hideProgress() {

        progressBar.setVisibility(View.GONE);
    }

    private void fillVCard(boolean hasDecryptionError) {

        hideProgress();
        if (hasDecryptionError) {
            if (mEmptyEncryptedView == null) {
                mEmptyEncryptedView = mEmptyEncryptedStub.inflate();
            }
            showDecryptionErrorBottomPart();
        }
        if (mEmptyEncryptedView != null) {
            mEmptyEncryptedView.setVisibility(View.GONE);
        }
        mEncryptedDataContainer.removeAllViews();
        UserCrypto crypto = Crypto.forUser(mUserManager, mUserManager.getUsername());
        boolean type2Verify = false;
        if (!TextUtils.isEmpty(mVCardType2Signature) && !TextUtils.isEmpty(mVCardType2)) {
            try {
                type2Verify = crypto.verify(mVCardType2, mVCardType2Signature).isSignatureValid();
            } catch (Exception e) {
                Timber.w(e, "VCard type2 verification error");
            }
        } else {
            type2Verify = true;
        }
        boolean type3Verify = false;
        if (!TextUtils.isEmpty(mVCardType3Signature) && !TextUtils.isEmpty(mVCardType3)) {
            try {
                type3Verify = crypto.verify(mVCardType3, mVCardType3Signature).isSignatureValid();
            } catch (Exception e) {
                Timber.w(e, "VCard type3 verification error");
            }
        } else {
            type3Verify = true;
        }
        if (!type2Verify) {
            showSignatureErrorTopPart();
        }
        if (!type3Verify) {
            showSignatureErrorBottomPart();
        }
        final VCard vCardType0 = mVCardType0 != null ? Ezvcard.parse(mVCardType0).first() : null;
        final VCard vCardType2 = mVCardType2 != null ? Ezvcard.parse(mVCardType2).first() : null;
        final VCard vCardType3 = mVCardType3 != null ? Ezvcard.parse(mVCardType3).first() : null;
        boolean isEmpty = true;
        fillTopPart(vCardType0, vCardType2);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            fabWeb.setBackgroundColor(getResources().getColor(R.color.new_purple));
        } else {
            ViewCompat.setBackgroundTintList(fabWeb, ColorStateList.valueOf(getResources().getColor(R.color.new_purple)));
        }

        File vcfFile = new File(this.getExternalFilesDir(null), mDisplayName + ".vcf");
        try {
            FileWriter fw = new FileWriter(vcfFile);
            if (mVCardType0 != null) {
                fw.write(mVCardType0);
            } else if (mVCardType2 != null) {
                fw.write(mVCardType2);
            }
            fw.close();
        } catch (IOException e) {
            Timber.w(e, "VCard file operation error");
        }

        fabWeb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ContactDetailsActivity.this, getApplicationContext().getPackageName() + ".provider", vcfFile));
            intent.putExtra(Intent.EXTRA_SUBJECT, mDisplayName);
            intent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
            startActivity(intent);
        });

        if (vCardType3 != null) {
            isEmpty = fillVCard(vCardType3); // fills bottom part
        }
        if (isEmpty && mUser.isPaidUser()) {
            if (mEmptyEncryptedView == null) {
                mEmptyEncryptedView = mEmptyEncryptedStub.inflate();
                mEmptyEncryptedView.findViewById(R.id.add_contact_details).setOnClickListener(v ->
                        startEditContacts());
            } else {
                if (!hasDecryptionError) {
                    mEmptyEncryptedView.setVisibility(View.VISIBLE);
                }
            }
            mCardViewBottomPart.setVisibility(View.GONE);
        } else {
            mCardViewBottomPart.setVisibility(View.VISIBLE);
        }
    }

    private void refresh() {

        viewModel.fetchContactGroupsAndContactEmails(mContactId);
    }

    private void fillTopPart(VCard vCardType0, VCard vCardType2) {

        mAddressesContainer.removeAllViews();
        if (vCardType0 != null) {
            fillTopPart(vCardType0);
        }
        if (vCardType2 != null) {
            fillTopPart(vCardType2);
        }
    }

    private String mDisplayName = "";

    private void fillTopPart(VCard vCard) {

        List<Email> vCardEmails = vCard.getEmails();
        viewModel.getContactEmailsGroups().observe(this, contactEmailsGroups -> {
            int rowId = contactEmailsGroups.getRowID();
            List<Integer> childIds = mAddressesContainer.getChildIds();
            for (Integer id : childIds) {
                if (id == rowId) {
                    View emailView = mAddressesContainer.findViewById(id);
                    ViewGroup groupsView = emailView.findViewById(R.id.groupsView);
                    groupsView.removeAllViews();
                    for (ContactLabel contactLabel : contactEmailsGroups.getGroups()) {
                        View groupChild = inflater.inflate(R.layout.contact_details_email_groups_item, groupsView, false);
                        ImageView groupItem = groupChild.findViewById(R.id.groupItem);
                        String colorString = UiUtil.normalizeColor(contactLabel.getColor());
                        Drawable groupDrawable = groupItem.getDrawable();
                        groupDrawable.mutate();
                        groupDrawable.setColorFilter(
                                Color.parseColor(colorString),
                                PorterDuff.Mode.SRC_IN
                        );
                        groupsView.addView(groupChild);
                    }
                }
            }
        });
        viewModel.getContactEmailsError().observe(this, event -> {
            ErrorResponse error = new ErrorResponse("", ErrorEnum.DEFAULT);
            if (event != null) {
                error = event.getContentIfNotHandled();
            }
            if (error != null) {
                TextExtensions.showToast(ContactDetailsActivity.this,
                        error.getMessage(ContactDetailsActivity.this));
            }
        });
        for (Email email : vCardEmails) {
            List<EmailType> emailTypes = email.getTypes();
            String emailType = getString(R.string.email);
            if (emailTypes != null && emailTypes.size() > 0) {
                emailType = emailTypes.iterator().next().getValue();
                emailType = VCardUtil.capitalizeType(VCardUtil.removeCustomPrefixForCustomType(emailType));
                if (mVCardEmailOptions.contains(emailType)) {
                    emailType = mVCardEmailUIOptions.get(mVCardEmailOptions.indexOf(emailType));
                }
            }
            View emailRowView = createEmailRow(emailType, email.getValue());
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                fabMail.setBackgroundColor(getResources().getColor(R.color.new_purple));
            } else {
                ViewCompat.setBackgroundTintList(fabMail, ColorStateList.valueOf(getResources().getColor(R.color.new_purple)));
            }
            fabMail.setOnClickListener(v -> {
                final Intent intent = AppUtil.decorInAppIntent(new Intent(ContactDetailsActivity.this, ComposeMessageActivity.class));
                intent.putExtra(ComposeMessageActivity.EXTRA_TO_RECIPIENTS, new String[]{vCardEmails.get(0).getValue()});
                startActivity(intent);
            });
            UiUtil.generateViewId(emailRowView);
            mAddressesContainer.addView(emailRowView);
            viewModel.fetchContactEmailGroups(emailRowView.getId(), email.getValue());
        }
        FormattedName formattedName = vCard.getFormattedName();
        if (formattedName != null && !TextUtils.isEmpty(formattedName.getValue())) {
            mDisplayName = formattedName.getValue();
        } else {
            StructuredName structuredName = vCard.getStructuredName();
            if (structuredName != null) {
                mDisplayName = structuredName.getGiven() + " " + structuredName.getFamily();
            }
        }
        collapsingToolbar.setTitle("");
        contactCollapsedTitle.setText(mDisplayName);
        contactTitle.setText(mDisplayName);
        contactAvatar.setName(mDisplayName);
        contactAvatar.setAvatarType(TYPE_INITIALS);
    }

    private boolean fillVCard(VCard vCard) {

        boolean isEmpty = true;
        List<Telephone> vCardPhones = vCard.getTelephoneNumbers();
        if (vCardPhones != null && vCardPhones.size() > 0) {
            isEmpty = false;
            for (Telephone phone : vCardPhones) {
                List<TelephoneType> types = phone.getTypes();
                String phoneType = getResources().getStringArray(R.array.vcard_option_phone)[0];
                if (types != null && types.size() > 0) {
                    phoneType = types.iterator().next().getValue();
                    phoneType = VCardUtil.capitalizeType(VCardUtil.removeCustomPrefixForCustomType(phoneType));
                    if (mVCardPhoneOptions.contains(phoneType)) {
                        phoneType = mVCardPhoneUIOptions.get(mVCardPhoneOptions.indexOf(phoneType));
                    }
                }
                mEncryptedDataContainer.addView(createNewEncryptedItemRow(phoneType, phone.getText(), false, 1));
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                    fabPhone.setBackgroundColor(getResources().getColor(R.color.new_purple));
                } else {
                    ViewCompat.setBackgroundTintList(fabPhone, ColorStateList.valueOf(getResources().getColor(R.color.new_purple)));
                }
                fabPhone.setOnClickListener(v -> {
                    String uri = "tel:" + vCardPhones.get(0).getText();
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse(uri));
                    startActivity(callIntent);
                });
            }
        }
        List<Address> vCardAddresses = vCard.getAddresses();
        if (vCardAddresses != null && vCardAddresses.size() > 0) {
            isEmpty = false;
            for (Address address : vCardAddresses) {
                List<AddressType> addressTypes = address.getTypes();
                String type = getResources().getStringArray(R.array.vcard_option_address)[0];
                if (addressTypes != null && addressTypes.size() > 0) {
                    type = addressTypes.iterator().next().getValue();
                    type = VCardUtil.capitalizeType(VCardUtil.removeCustomPrefixForCustomType(type));
                    if (mVCardAddressOptions.contains(type)) {
                        type = mVCardAddressUIOptions.get(mVCardAddressOptions.indexOf(type));
                    }
                }
                mEncryptedDataContainer.addView(createNewEncryptedItemRow(type, address));
            }
        }

        //region other misc information
        List<Photo> vCardPhotos = vCard.getPhotos();
        List<Organization> vCardOrganizations = vCard.getOrganizations();
        List<Title> vCardTitles = vCard.getTitles();
        List<Nickname> vCardNicknames = vCard.getNicknames();
        List<Birthday> vCardBirthdays = vCard.getBirthdays();
        List<Anniversary> vCardAnniversary = vCard.getAnniversaries();
        List<Role> vCardRoles = vCard.getRoles();
        List<Url> vCardUrls = vCard.getUrls();
        Gender vCardGender = vCard.getGender();

        if (vCardPhotos != null && vCardPhotos.size() > 0) {
            Photo photo = vCardPhotos.get(0);
            byte[] photoData = photo.getData();
            if (photoData != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(photoData, 0, photoData.length);
                contactAvatar.setImage(bmp);
                contactAvatar.setAvatarType(TYPE_PHOTO);
            } else if (!vCardPhotos.get(0).getUrl().isEmpty()) {
                contactAvatar.setName(mDisplayName);
                contactAvatar.setAvatarType(TYPE_INITIALS);
                viewModel.getBitmapFromURL(photo.getUrl());
            }
        } else {
            contactAvatar.setName(mDisplayName);
            contactAvatar.setAvatarType(TYPE_INITIALS);
        }

        if ((vCardOrganizations != null && !vCardOrganizations.isEmpty()) || (vCardTitles != null && !vCardTitles.isEmpty())
                || (vCardNicknames != null && !vCardNicknames.isEmpty()) || (vCardBirthdays != null && !vCardBirthdays.isEmpty())
                || (vCardAnniversary != null && !vCardAnniversary.isEmpty()) || (vCardRoles != null && !vCardRoles.isEmpty())
                || (vCardUrls != null && !vCardUrls.isEmpty()) || vCardGender != null) {
            isEmpty = false;
        }
        if (vCardOrganizations != null && vCardOrganizations.size() > 0) {
            for (Organization organization : vCardOrganizations) {
                mEncryptedDataContainer.addView(createNewEncryptedItemRow(getString(R.string.vcard_other_option_org),
                        organization.getValues().get(0), false, 2));
            }
        }

        if (vCardTitles != null && vCardTitles.size() > 0) {
            for (Title title : vCardTitles) {
                mEncryptedDataContainer.addView(createNewEncryptedItemRow(getString(R.string.vcard_other_option_title), title.getValue()));
            }
        }

        if (vCardNicknames != null && vCardNicknames.size() > 0) {
            for (Nickname nickname : vCardNicknames) {
                String type = nickname.getType();
                if (TextUtils.isEmpty(type)) {
                    type = getString(R.string.vcard_other_option_nickname);
                }
                mEncryptedDataContainer.addView(createNewEncryptedItemRow(type, nickname.getValues().get(0)));
            }
        }

        if (vCardBirthdays != null && vCardBirthdays.size() > 0) {
            for (Birthday birthday : vCardBirthdays) {
                Date birthdayDate = birthday.getDate();
                PartialDate birthdayPartialDate = birthday.getPartialDate();
                String birthdayText = birthday.getText();
                String value = "";
                if (birthdayDate != null) {
                    value = DateUtil.formatDate(birthdayDate);
                } else if (birthdayPartialDate != null) {
                    value = birthdayPartialDate.toISO8601(false);
                } else if (!TextUtils.isEmpty(birthdayText)) {
                    value = birthdayText;
                }
                if (!TextUtils.isEmpty(value)) {
                    mEncryptedDataContainer.addView(createNewEncryptedItemRow(getString(R.string.vcard_other_option_birthday), value));
                }
            }
        }

        if (vCardAnniversary != null && vCardAnniversary.size() > 0) {
            for (Anniversary anniversary : vCardAnniversary) {
                Date anniversaryDate = anniversary.getDate();
                PartialDate anniversaryPartialDate = anniversary.getPartialDate();
                String anniversaryText = anniversary.getText();
                String value = "";
                if (anniversaryDate != null) {
                    value = DateUtil.formatDate(anniversaryDate);
                } else if (anniversaryPartialDate != null) {
                    value = anniversaryPartialDate.toISO8601(false);
                } else if (!TextUtils.isEmpty(anniversaryText)) {
                    value = anniversaryText;
                }
                mEncryptedDataContainer.addView(createNewEncryptedItemRow(getString(R.string.vcard_other_option_anniversary), value));
            }
        }

        if (vCardRoles != null && vCardRoles.size() > 0) {
            for (Role role : vCardRoles) {
                mEncryptedDataContainer.addView(createNewEncryptedItemRow(getString(R.string.vcard_other_option_role), role.getValue()));
            }
        }

        if (vCardUrls != null && vCardUrls.size() > 0) {
            for (Url url : vCardUrls) {
                mEncryptedDataContainer.addView(createNewEncryptedItemRow(getString(R.string.vcard_other_option_url), url.getValue()));
            }
        }

        if (vCardGender != null) {
            mEncryptedDataContainer.addView(createNewEncryptedItemRow(getString(R.string.vcard_other_option_gender), vCardGender.getGender()));
        }
        //endregion

        List<Note> vCardNotes = vCard.getNotes();
        if (vCardNotes != null && vCardNotes.size() > 0) {
            isEmpty = false;
            for (Note note : vCardNotes) {
                mEncryptedDataContainer.addView(createNewEncryptedItemRow(getString(R.string.contact_vcard_note), note.getValue(), false, 4));
            }
        }

        return isEmpty;
    }

    private final View.OnClickListener mUpgradeClickListener = v -> {
        Intent upgradeIntent = new Intent(ContactDetailsActivity.this, UpsellingActivity.class);
        upgradeIntent.putExtra(UpsellingActivity.EXTRA_OPEN_UPGRADE_CONTAINER, true);
        startActivityForResult(AppUtil.decorInAppIntent(upgradeIntent), REQUEST_CODE_UPGRADE);
    };

    private void showSignatureErrorTopPart() {
        mTopPanel.setBackgroundDrawable(getResources().getDrawable(R.drawable.signature_error_border));
        mTopPanelVerificationError.setVisibility(View.VISIBLE);
        TextView learnMoreText = findViewById(R.id.learn_more);
        learnMoreText.setOnClickListener(onLearnMoreClickListener);
    }

    private void showSignatureErrorBottomPart() {
        mEncryptedContactTitleView.setVisibility(View.GONE);
        if (mBottomPanelErrorView == null) {
            mBottomPanelErrorView = mBottomPanelVerificationErrorStub.inflate();
        }
        TextView bottomPanelErrorText = mBottomPanelErrorView.findViewById(R.id.signature_error);
        TextView bottomPanelErrorDescText = mBottomPanelErrorView.findViewById(R.id.signature_error_desc);
        TextView learnMoreText = mBottomPanelErrorView.findViewById(R.id.learn_more);
        learnMoreText.setOnClickListener(onLearnMoreClickListener);
        bottomPanelErrorText.setText(getString(R.string.signature_error));
        bottomPanelErrorDescText.setText(getString(R.string.signature_error_desc));
    }

    private void showDecryptionErrorBottomPart() {
        mEncryptedDataContainer.setBackgroundDrawable(getResources().getDrawable(R.drawable.signature_error_border));
        if (mBottomPanelErrorView == null) {
            mBottomPanelErrorView = mBottomPanelVerificationErrorStub.inflate();
        }
        TextView bottomPanelErrorText = mBottomPanelErrorView.findViewById(R.id.signature_error);
        TextView bottomPanelErrorDescText = mBottomPanelErrorView.findViewById(R.id.signature_error_desc);
        TextView learnMoreText = mBottomPanelErrorView.findViewById(R.id.learn_more);
        learnMoreText.setOnClickListener(onLearnMoreClickListener);
        bottomPanelErrorText.setText(getString(R.string.decryption_error));
        bottomPanelErrorDescText.setText(getString(R.string.decryption_error_desc));
    }

    View.OnClickListener onLearnMoreClickListener = v -> {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.contacts_learn_more)));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            TextExtensions.showToast(ContactDetailsActivity.this, R.string.no_browser_found, Toast.LENGTH_SHORT);
        }
    };

    @OnClick(R.id.editContactDetails)
    public void onEditContactDetailsClicked() {
        startEditContacts();
    }

    private void startEditContacts() {
        String vCardFilePath = "";
        if (mVCardType3 != null && mVCardType3.length() > 0) {
            vCardFilePath = getCacheDir().toString() + File.separator +  VCARD_TEMP_FILE_NAME;
            fileHelper.saveStringToFile(vCardFilePath, mVCardType3);
        }
        EditContactDetailsActivity.startEditContactActivity(
                this, mContactId, REQUEST_CODE_EDIT_CONTACT, mVCardType0, mVCardType2, vCardFilePath
        );
    }

    private void copyValueToClipboard(CharSequence title, CharSequence value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(title, value);
        if (clipboard != null) {
            try {
                clipboard.setPrimaryClip(clip);
            } catch (IllegalStateException ise) {
                Timber.w(ise, "Clipboard bug");
            }
        }
        TextExtensions.showToast(ContactDetailsActivity.this, R.string.details_copied, Toast.LENGTH_SHORT);
    }

    private PopupWindow initDialog(String email) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int pwHeight = displayMetrics.heightPixels;
        int pwWidth = displayMetrics.widthPixels;

        PopupWindow pw = new PopupWindow(inflater.inflate(R.layout.contact_groups_dropdown_layout,
                null, false), pwWidth - 20, (pwHeight / 3) + 100, false);
        pw.setOutsideTouchable(true);
        pw.update();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pw.setElevation(20);
        }

        View popupView = pw.getContentView();
        TextView noResults = popupView.findViewById(R.id.noResults);
        CustomFontButton applyBtn = popupView.findViewById(R.id.applyBtn);

        viewModel.mergeContactEmailGroups(email);
        viewModel.getMergedContactEmailGroupsResult().observe(ContactDetailsActivity.this, contactLabels -> {

            if (contactLabels != null) {
                if (contactLabels.isEmpty()) {
                    noResults.setVisibility(View.VISIBLE);
                    applyBtn.setVisibility(View.INVISIBLE);
                } else {
                    noResults.setVisibility(View.INVISIBLE);
                    applyBtn.setVisibility(View.VISIBLE);
                }
                contactEditDetailsEmailGroupsAdapter.setData(contactLabels);
            }
        });

        viewModel.getMergedContactEmailGroupsError().observe(ContactDetailsActivity.this, event -> {
            ErrorResponse error = new ErrorResponse("", ErrorEnum.DEFAULT);
            if (event != null) {
                error = event.getContentIfNotHandled();
            }
            if (error != null) {
                TextExtensions.showToast(ContactDetailsActivity.this,
                        error.getMessage(ContactDetailsActivity.this));
            }
        });

        RecyclerView groupsView = popupView.findViewById(R.id.groupsView);
        groupsView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        groupsView.setAdapter(contactEditDetailsEmailGroupsAdapter);

        applyBtn.setOnClickListener(v -> {
            for (ContactLabel label : contactEditDetailsEmailGroupsAdapter.getItems()) {
                viewModel.updateContactEmailGroup(label, email);
            }
            pw.dismiss();
        });

        return pw;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;
        handleAlphaOnTitle(percentage);
        handleToolbarTitleVisibility(percentage);
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
    }

    private void handleToolbarTitleVisibility(float percentage) {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {
            if (!mIsTheTitleVisible) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayShowTitleEnabled(false);
                    contactCollapsedTitleContainer.setVisibility(View.VISIBLE);
                }
                startAlphaAnimation(contactCollapsedTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleVisible = true;
            }
        } else {
            if (mIsTheTitleVisible) {
                startAlphaAnimation(contactCollapsedTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleVisible = false;
            }
        }
    }

    private void handleAlphaOnTitle(float percentage) {
        TextView title = new TextView(ContactDetailsActivity.this);
        if (toolbar != null) {
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                if (toolbar.getChildAt(i) instanceof TextView) {
                    title = (TextView) toolbar.getChildAt(i);
                    if (getSupportActionBar() != null && title.getText().equals(getSupportActionBar().getTitle())) {
                        break;
                    }
                }
            }
        }

        if (percentage >= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if (mIsTheTitleContainerVisible) {
                if (getSupportActionBar() != null) {
                    startAlphaAnimation(title, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                }
                startAlphaAnimation(contactTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleContainerVisible = false;
            }
        } else {
            if (!mIsTheTitleContainerVisible) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                    startAlphaAnimation(title, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                    contactCollapsedTitleContainer.setVisibility(View.GONE);
                }
                startAlphaAnimation(contactTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleContainerVisible = true;
            }
        }
    }

    public static void startAlphaAnimation(View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);
        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }
}

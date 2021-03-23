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
package ch.protonmail.android.contacts.details.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.squareup.otto.Subscribe;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseConnectivityActivity;
import ch.protonmail.android.activities.UpsellingActivity;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.contacts.UnsavedChangesDialog;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.data.local.model.ContactEmail;
import ch.protonmail.android.events.ContactEvent;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.DateUtil;
import ch.protonmail.android.utils.Event;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.VCardUtil;
import ch.protonmail.android.utils.extensions.CommonExtensionsKt;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.views.ContactAddressView;
import ch.protonmail.android.views.ContactBirthdayClickListener;
import ch.protonmail.android.views.ContactOptionTypeClickListener;
import ch.protonmail.android.views.CustomFontButton;
import ch.protonmail.android.views.CustomFontTextView;
import ch.protonmail.android.views.VCardLinearLayout;
import ch.protonmail.android.views.models.LocalContact;
import ch.protonmail.android.views.models.LocalContactAddress;
import dagger.hilt.android.AndroidEntryPoint;
import ezvcard.VCard;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImageType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Anniversary;
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.FormattedName;
import ezvcard.property.Gender;
import ezvcard.property.Key;
import ezvcard.property.Nickname;
import ezvcard.property.Note;
import ezvcard.property.Organization;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Role;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Url;
import ezvcard.util.PartialDate;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import timber.log.Timber;

import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.EXTRA_CONTACT;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.EXTRA_CONTACT_VCARD_TYPE0;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.EXTRA_CONTACT_VCARD_TYPE2;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.EXTRA_CONTACT_VCARD_TYPE3;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.EXTRA_EMAIL;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.EXTRA_FLOW;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.EXTRA_LOCAL_CONTACT;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.EXTRA_NAME;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.FLOW_CONVERT_CONTACT;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.FLOW_EDIT_CONTACT;
import static ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelKt.FLOW_NEW_CONTACT;

@AndroidEntryPoint
public class EditContactDetailsActivity extends BaseConnectivityActivity {

    private static final int REQUEST_CODE_UPGRADE = 2;
    private static final int REQUEST_CODE_OPEN_GALLERY = 1000;
    private static final int REQUEST_CODE_OPEN_CAMERA = 1001;

    @BindView(R.id.contact_display_name)
    EditText mDisplayNameView;
    @BindView(R.id.emailAddressesContainer)
    VCardLinearLayout mEmailAddressesContainer;
    @BindView(R.id.progress_bar)
    View mProgressBar;
    @BindView(R.id.encryptedDataContainer)
    LinearLayout mEncryptedDataContainer;
    @BindView(R.id.encrypted_data_address)
    VCardLinearLayout mEncryptedDataAddress;
    @BindView(R.id.encrypted_data_phone)
    VCardLinearLayout mEncryptedDataPhone;
    @BindView(R.id.encrypted_data_other)
    VCardLinearLayout mEncryptedDataOther;
    @BindView(R.id.encrypted_data_note)
    VCardLinearLayout mEncryptedDataNote;
    @BindView(R.id.upgradeEncryptedStub)
    ViewStub mUpgradeEncryptedDataStub;
    @BindView(R.id.scroll_parent)
    ScrollView mScrollParentView;
    @BindView(R.id.addPhotoBtn)
    CustomFontButton addPhotoBtn;
    @BindView(R.id.contactPhoto)
    ImageView contactPhoto;
    @BindView(R.id.photoCardViewWrapper)
    CardView photoCardViewWrapper;
    @BindView(R.id.contactInitials)
    CustomFontTextView contactInitials;

    private LayoutInflater inflater;
    private final AtomicBoolean mSavingInProgress = new AtomicBoolean(false);

    private EditContactDetailsViewModel viewModel;

    public static Intent startNewContactActivity(@NonNull Context context) {
        final Intent intent = AppUtil.decorInAppIntent(
                new Intent(context, EditContactDetailsActivity.class));
        intent.putExtra(EXTRA_FLOW, FLOW_NEW_CONTACT);
        return intent;
    }

    public static Intent startNewContactActivity(@NonNull Context context, String name, String email) {
        final Intent intent = AppUtil.decorInAppIntent(
                new Intent(context, EditContactDetailsActivity.class));
        intent.putExtra(EXTRA_FLOW, FLOW_NEW_CONTACT);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_EMAIL, email);
        return intent;
    }

    public static Intent startConvertContactActivity(@NonNull Context context, LocalContact localContact) {
        final Intent intent = AppUtil.decorInAppIntent(
                new Intent(context, EditContactDetailsActivity.class));
        intent.putExtra(EXTRA_FLOW, FLOW_CONVERT_CONTACT);
        intent.putExtra(EXTRA_LOCAL_CONTACT, localContact);
        return intent;
    }

    public static void startEditContactActivity(@NonNull Activity context, String contactId, int requestCode, String vCardType0, String vCardType2, String vCardType3) {
        final Intent intent = AppUtil.decorInAppIntent(
                new Intent(context, EditContactDetailsActivity.class));
        intent.putExtra(EXTRA_FLOW, FLOW_EDIT_CONTACT)
                .putExtra(EXTRA_CONTACT, contactId)
                .putExtra(EXTRA_CONTACT_VCARD_TYPE0, vCardType0)
                .putExtra(EXTRA_CONTACT_VCARD_TYPE2, vCardType2)
                .putExtra(EXTRA_CONTACT_VCARD_TYPE3, vCardType3);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EditContactDetailsViewModel.class);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        inflater = LayoutInflater.from(this);
        mEmailAddressesContainer.removeAllViews();
        mDisplayNameView.addTextChangedListener(new DirtyWatcher());

        startObserving();
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }
        viewModel.setup(
                extras.getInt(EXTRA_FLOW),
                extras.getString(EXTRA_CONTACT, ""),
                (LocalContact) extras.getSerializable(EXTRA_LOCAL_CONTACT),
                extras.getString(EXTRA_EMAIL, ""),
                Arrays.asList(getResources().getStringArray(R.array.vcard_option_phone)),
                Arrays.asList(getResources().getStringArray(R.array.vcard_option_phone_val)),
                Arrays.asList(getResources().getStringArray(R.array.vcard_option_email)),
                Arrays.asList(getResources().getStringArray(R.array.vcard_option_email_val)),
                Arrays.asList(getResources().getStringArray(R.array.vcard_option_address)),
                Arrays.asList(getResources().getStringArray(R.array.vcard_option_address_val)),
                Arrays.asList(getResources().getStringArray(R.array.vcard_option_other)),
                extras.getString(EXTRA_CONTACT_VCARD_TYPE0),
                extras.getString(EXTRA_CONTACT_VCARD_TYPE2),
                extras.getString(EXTRA_CONTACT_VCARD_TYPE3));

        viewModel.getSetupComplete().observe(this, setupCompleteObserver);

        addPhotoBtn.setOnClickListener(v -> {

            AlertDialog.Builder pictureDialog = new AlertDialog.Builder(EditContactDetailsActivity.this);
            pictureDialog.setTitle(getString(R.string.contacts_add_photo_title));
            String[] pictureDialogItems = {
                    getString(R.string.contacts_add_photo_select_gallery),
                    getString(R.string.contacts_add_photo_select_camera)};
            pictureDialog.setItems(pictureDialogItems,
                    (dialog, which) -> {
                        switch (which) {
                            case 0:

                                Intent intent = new Intent();
                                intent.setType("image/*");
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_CODE_OPEN_GALLERY);
                                break;
                            case 1:
                                Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                startActivityForResult(intent1, REQUEST_CODE_OPEN_CAMERA);
                                break;
                        }
                    });
            pictureDialog.show();
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (requestCode == REQUEST_CODE_OPEN_GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    photoCardViewWrapper.setVisibility(View.VISIBLE);
                    contactInitials.setVisibility(View.GONE);
                    contactPhoto.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else if (requestCode == REQUEST_CODE_OPEN_CAMERA) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            photoCardViewWrapper.setVisibility(View.VISIBLE);
            contactInitials.setVisibility(View.GONE);
            contactPhoto.setImageBitmap(thumbnail);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.checkConnectivity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ProtonMailApplication.getApplication().getBus().unregister(this);
    }

    @Override
    public void onBackPressed() {
        viewModel.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (viewModel.isConvertContactFlow()) {
            getMenuInflater().inflate(R.menu.contact_details_convert_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.contact_details_edit_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_save:
                boolean previous = mSavingInProgress.getAndSet(true);
                if (!previous) {
                    saveContact();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveContact() {
        String contactName = mDisplayNameView.getText().toString();

        List<ContactEmail> emails = new ArrayList<>();
        if (TextUtils.isEmpty(contactName)) {
            contactName = getString(R.string.contacts_unknown);
        }
        VCard vCardEncrypted = viewModel.buildEncryptedCard();
        VCard vCardSigned = viewModel.buildSignedCard(contactName);

        if (contactPhoto.getVisibility() == View.VISIBLE && contactPhoto.getDrawable() != null) {
            Bitmap bitmap = ((BitmapDrawable) contactPhoto.getDrawable()).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
            byte[] bytemapdata = stream.toByteArray();
            Photo photo = new Photo(bytemapdata, ImageType.JPEG);
            vCardEncrypted.addPhoto(photo);
        }

        String optionType;
        String optionValue;
        List<Integer> childIds = mEmailAddressesContainer.getChildIds();
        int i = 1;
        List<String> emailsToBeRemoved = new ArrayList<>();
        for (Integer id : childIds) {
            View rowView = mEmailAddressesContainer.findViewById(id);
            TextView optionTypeView = rowView.findViewById(R.id.optionTitle);
            Object optionTypeTag = optionTypeView.getTag();
            optionType = (String) optionTypeTag;
            EditText optionEditText = rowView.findViewById(R.id.option);
            optionValue = optionEditText.getText().toString();
            Object groupTag = rowView.getTag();
            String groupId = null;
            if (groupTag instanceof String) {
                groupId = (String) groupTag;
            }
            if (!TextUtils.isEmpty(optionValue) && !CommonExtensionsKt.isValidEmail(optionValue)) {
                optionEditText.setError(getString(R.string.invalid_email));
                mScrollParentView.requestChildFocus(optionEditText, optionEditText);
                mSavingInProgress.set(false);
                return;
            }
            if (!TextUtils.isEmpty(optionValue)) {
                emailsToBeRemoved.add(optionValue);
                emails.add(new ContactEmail(viewModel.getContactId(), optionValue, contactName));
                Email vCardEmail = new Email(optionValue);
                if (!TextUtils.isEmpty(optionType)) {
                    EmailType emailType = EmailType.find(optionType);
                    if (emailType == null) {
                        vCardEmail.addParameter("TYPE", optionType);
                    } else {
                        vCardEmail.getTypes().add(emailType);
                    }
                }
                vCardEmail.setGroup(TextUtils.isEmpty(groupId) ? ("item" + i) : groupId);
                vCardSigned.addEmail(vCardEmail); // we add the emails to the signed vCard
                i++;
            }
        }
        for (RawProperty rawProperty : viewModel.getExtendedPropertiesType2()) {
            RawProperty rawProperty1 = new RawProperty(rawProperty);
            vCardSigned.addProperty(rawProperty1);
        }
        for (Key key : viewModel.getKeysType2()) {
            vCardSigned.addKey(key);
        }
        childIds = mEncryptedDataPhone.getChildIds();
        for (Integer id : childIds) {
            View rowView = mEncryptedDataPhone.findViewById(id);
            TextView optionTypeView = rowView.findViewById(R.id.optionTitle);
            optionType = (String) optionTypeView.getTag();
            optionType = optionType == null ? "" : optionType;
            optionValue = ((TextView) rowView.findViewById(R.id.option)).getText().toString();
            if (TextUtils.isEmpty(optionValue)) {
                continue;
            }
            TelephoneType type = TelephoneType.find(optionType);
            Telephone telephone = new Telephone(optionValue);
            if (type == null) {
                type = TelephoneType.find(optionType);
            }
            if (type != null) {
                telephone.getTypes().add(type);
            } else {
                telephone.addParameter("TYPE", optionType);
            }
            vCardEncrypted.addTelephoneNumber(telephone);
        }
        childIds = mEncryptedDataAddress.getChildIds();
        for (Integer id : childIds) {
            View rowView = mEncryptedDataAddress.findViewById(id);
            TextView optionTypeView = rowView.findViewById(R.id.optionTitle);
            optionType = (String) optionTypeView.getTag();
            EditText addressStreetView = rowView.findViewById(R.id.address_street);
            EditText addressCityView = rowView.findViewById(R.id.address_city);
            EditText addressRegionView = rowView.findViewById(R.id.address_region);
            EditText addressPostcodeView = rowView.findViewById(R.id.address_postcode);
            EditText addressCountryView = rowView.findViewById(R.id.address_country);

            String streetAddress = addressStreetView.getText().toString();
            String localityAddress = addressCityView.getText().toString();
            String regionAddress = addressRegionView.getText().toString();
            String postalCodeAddress = addressPostcodeView.getText().toString();
            String countryAddress = addressCountryView.getText().toString();
            boolean isEmpty = TextUtils.isEmpty(streetAddress) && TextUtils.isEmpty(
                    localityAddress) && TextUtils.isEmpty(regionAddress) && TextUtils.isEmpty(
                    postalCodeAddress) && TextUtils.isEmpty(countryAddress);
            if (!isEmpty) {
                Address address = new Address();
                if (!TextUtils.isEmpty(optionType)) {
                    address.addParameter("TYPE", optionType);
                }
                address.setStreetAddress(addressStreetView.getText().toString());
                address.setLocality(addressCityView.getText().toString());
                address.setRegion(addressRegionView.getText().toString());
                address.setPostalCode(addressPostcodeView.getText().toString());
                address.setCountry(addressCountryView.getText().toString());
                vCardEncrypted.addAddress(address);
            }
        }
        childIds = mEncryptedDataOther.getChildIds();
        for (Integer id : childIds) {
            View rowView = mEncryptedDataOther.findViewById(id);
            TextView optionTypeView = rowView.findViewById(R.id.optionTitle);
            optionType = optionTypeView.getText().toString();
            TextView optionValueView = rowView.findViewById(R.id.option);
            optionValue = optionValueView.getText().toString();
            Constants.VCardOtherInfoType otherInfoType = (Constants.VCardOtherInfoType) rowView.getTag();
            if (otherInfoType != null && !TextUtils.isEmpty(optionType) && !TextUtils.isEmpty(
                    optionValue)) {
                switch (otherInfoType) {
                    case ORGANIZATION:
                        Organization organization = new Organization();
                        organization.setType(optionType);
                        organization.getValues().add(optionValue);
                        vCardEncrypted.addOrganization(organization);
                        break;
                    case NICKNAME:
                        Nickname nickname = new Nickname();
                        nickname.setType(optionType);
                        nickname.getValues().add(optionValue);
                        vCardEncrypted.addNickname(nickname);
                        break;
                    case TITLE:
                        vCardEncrypted.addTitle(optionValue);
                        break;
                    case BIRTHDAY:
                        Birthday birthday = new Birthday(optionValue);
                        vCardEncrypted.setBirthday(birthday);
                        break;
                    case ANNIVERSARY:
                        Anniversary anniversary = new Anniversary(optionValue);
                        vCardEncrypted.setAnniversary(anniversary);
                        break;
                    case ROLE:
                        Role role = new Role(optionValue);
                        vCardEncrypted.addRole(role);
                        break;
                    case URL:
                        Url url = new Url(optionValue);
                        vCardEncrypted.addUrl(url);
                        break;
                    case GENDER:
                        Gender gender = new Gender(optionValue);
                        vCardEncrypted.setGender(gender);
                        break;
                    default:
                        vCardEncrypted.addExtendedProperty(optionType, optionValue);
                        break;
                }
            }
        }
        childIds = mEncryptedDataNote.getChildIds();
        for (Integer id : childIds) {
            View rowView = mEncryptedDataNote.findViewById(id);
            TextView optionValueView = rowView.findViewById(R.id.option);
            optionValue = optionValueView.getText().toString();
            if (!TextUtils.isEmpty(optionValue)) {
                Note note = new Note(optionValue);
                vCardEncrypted.addNote(note);
            }
        }
        List<RawProperty> vCardExtendedProperties = viewModel.getExtendedPropertiesType3();
        if (vCardExtendedProperties.size() > 0) {
            for (RawProperty rawProperty : vCardExtendedProperties) {
                vCardEncrypted.addProperty(rawProperty);
            }
        }
        TextExtensions.showToast(EditContactDetailsActivity.this, R.string.saving);

        viewModel.getCreateContactResult().observe(
                this, resultStringId -> {
                    String message = getString(resultStringId);
                    Toast.makeText(EditContactDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                    saveAndFinish();
                });
        viewModel.save(emailsToBeRemoved, contactName, emails);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_edit_contact_details;
    }

    private void fillTopPart(VCard vCardType0, VCard vCardType2) {
        mProgressBar.setVisibility(View.GONE);
        mEmailAddressesContainer.removeAllViews();
        boolean hasEmails = false;
        if (vCardType0 != null) {
            hasEmails = fillTopPart(vCardType0);
        }
        if (!hasEmails) {
            mEmailAddressesContainer.removeAllViews();
        }
        if (vCardType2 != null) {
            fillTopPart(vCardType2);
        }
    }

    private boolean fillTopPart(VCard vCard) {
        List<Email> vCardEmails = vCard.getEmails();
        if (vCardEmails.size() == 0 && mEmailAddressesContainer.getChildCount() == 0) {
            createEmailAddressView(viewModel.getDefaultEmailOption(), viewModel.getDefaultEmailUIOption(), "");
        }
        for (Email email : vCardEmails) {
            List<EmailType> emailTypes = email.getTypes();
            String emailUIType = viewModel.getDefaultEmailUIOption();
            String emailType = viewModel.getDefaultEmailOption();
            if (emailTypes != null && emailTypes.size() > 0) {
                emailType = emailTypes.iterator().next().getValue();
                emailUIType = VCardUtil.capitalizeType(VCardUtil.removeCustomPrefixForCustomType(emailType));
                List<String> vCardEmailOptions = viewModel.getEmailOptions();
                List<String> vCardEmailUIOptions = viewModel.getEmailUIOptions();
                if (vCardEmailOptions.contains(emailUIType)) {
                    emailUIType = vCardEmailUIOptions.get(vCardEmailOptions.indexOf(emailUIType));
                }
            }
            View view = createEmailAddressView(emailType, emailUIType, email.getValue());
            view.setTag(email.getGroup());
        }
        FormattedName formattedName = vCard.getFormattedName();
        if (formattedName != null && !TextUtils.isEmpty(formattedName.getValue())) {
            mDisplayNameView.setText(formattedName.getValue());
            contactInitials.setText(UiUtil.extractInitials(formattedName.getValue()));
        }

        return vCardEmails.size() > 0;
    }

    private View createExistingVCardOptionRow(String optionType, String optionUIType,
                                              Address address, String optionHint,
                                              List<String> standardOptionUIValues,
                                              List<String> standardOptionValues,
                                              VCardLinearLayout rootView) {
        return new ContactAddressView(this, optionType, optionUIType, address,
                optionHint, standardOptionUIValues, standardOptionValues, rootView);
    }

    private View createExistingVCardOptionRow(String optionType, String optionUIType,
                                              String optionValue, String optionHint,
                                              final List<String> standardOptionUIValues,
                                              final List<String> standardOptionValues,
                                              ViewGroup rootView, int inputType) {
        return createExistingVCardOptionRow(optionType, optionUIType, optionValue, optionHint,
                standardOptionUIValues, standardOptionValues, rootView, true, 1, inputType);
    }

    @SuppressLint("ClickableViewAccessibility")
    private View createExistingVCardOptionRow(String optionType, String optionUIType,
                                              String optionValue, String optionHint,
                                              final List<String> standardOptionUIValues,
                                              final List<String> standardOptionValues,
                                              ViewGroup rootView, boolean singleLine, int maxLines,
                                              int inputType) {
        final View emailRowView = inflater.inflate((singleLine) ?
                R.layout.contact_vcard_item_editable :
                R.layout.contact_vcard_item_note, rootView, false);
        UiUtil.generateViewId(emailRowView);
        final TextView optionIcon = emailRowView.findViewById(R.id.optionIcon);
        final TextView titleView = emailRowView.findViewById(R.id.optionTitle);
        final EditText optionValueView = emailRowView.findViewById(R.id.option);
        optionValueView.setVisibility(View.VISIBLE);
        if (singleLine) {
            optionValueView.setInputType(inputType);
        } else {
            optionValueView.setLines(maxLines);
            optionValueView.setMaxLines(maxLines);
            optionValueView.setOnTouchListener((v, event) -> {
                if (optionValueView.hasFocus()) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_SCROLL:
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            return true;
                    }
                }
                return false;
            });
        }
        final ImageButton btnMinus = emailRowView.findViewById(R.id.btn_minus);
        btnMinus.setVisibility(View.VISIBLE);
        optionValueView.addTextChangedListener(new DirtyWatcher());
        optionValueView.setHint(optionHint);
        ImageButton btnEmailType = emailRowView.findViewById(R.id.btnOptionType);
        if (standardOptionUIValues == null || standardOptionUIValues.size() == 0) {
            btnEmailType.setVisibility(View.GONE);
        }
        String optionUITypeText = " ";
        if (optionUIType.contains(" ")) {
            String[] optionUITypeTextSplit = optionUIType.split(" ");
            if (optionUITypeTextSplit.length > 1) {
                optionUITypeText = optionUITypeTextSplit[1];
            } else {
                optionUITypeText = standardOptionUIValues.get(0);
            }
            optionIcon.setText(optionUITypeTextSplit[0]);
        } else {
            optionUITypeText = optionUIType;
        }
        ContactOptionTypeClickListener optionTypeClickListener = new ContactOptionTypeClickListener(
                this, getSupportFragmentManager(), emailRowView, optionUIType.isEmpty() ? standardOptionUIValues.get(0) : optionUITypeText,
                standardOptionUIValues, standardOptionValues);
        btnEmailType.setOnClickListener(optionTypeClickListener);
        titleView.setOnClickListener(optionTypeClickListener);
        titleView.setText(optionUITypeText);
        titleView.setTag(optionType);
        optionValueView.setText(optionValue);

        btnMinus.setOnClickListener(v -> {
            VCardLinearLayout viewParent = (VCardLinearLayout) emailRowView.getParent();
            int childCount = viewParent.getChildCount();
            if (childCount <= 2) {
                optionValueView.setText("");
                if (standardOptionUIValues != null && standardOptionUIValues.size() > 0) {
                    titleView.setText(standardOptionUIValues.get(0));
                }
                if (standardOptionValues != null && standardOptionValues.size() > 0) {
                    titleView.setTag(standardOptionValues.get(0));
                }
            } else {
                viewParent.removeView(emailRowView);
            }
        });

        return emailRowView;
    }

    private View createNewVCardOptionRow(final String optionTitleText, final String editTextHint, final String addOptionTitleText,
                                         final List<String> standardOptionUIValues,
                                         final List<String> standardOptionValues,
                                         final ViewGroup rootView, boolean singleLine, final int inputType) {
        View newOptionRowView;
        if (singleLine) {
            newOptionRowView = inflater.inflate(R.layout.contact_new_vcard_item_email_address, rootView, false);
        } else {
            newOptionRowView = inflater.inflate(R.layout.contact_vcard_item_note, rootView, false);
        }
        UiUtil.generateViewId(newOptionRowView);
        final Button btnAddNewRow = newOptionRowView.findViewById(R.id.btnAddNewRow);
        final EditText option = newOptionRowView.findViewById(R.id.option);
        final View inputFields = newOptionRowView.findViewById(R.id.fields_parent);
        final ImageButton btnOptionType = newOptionRowView.findViewById(R.id.btnOptionType);
        final ImageButton btnMinus = newOptionRowView.findViewById(R.id.btn_minus);
        final TextView optionTitle = newOptionRowView.findViewById(R.id.optionTitle);
        final TextView optionIcon = newOptionRowView.findViewById(R.id.optionIcon);
        String optionUITypeText;
        if (optionTitleText.contains(" ")) {
            optionIcon.setText(optionTitleText.split(" ")[0]);
            optionUITypeText = optionTitleText.split(" ")[1];
        } else {
            optionUITypeText = optionTitleText;
        }

        option.setHint(editTextHint);
        option.addTextChangedListener(new DirtyWatcher());
        option.setInputType(inputType);
        optionTitle.setText(optionUITypeText);

        btnAddNewRow.setVisibility(View.VISIBLE);
        option.setVisibility(View.GONE);
        btnMinus.setVisibility(View.GONE);
        if (!singleLine) {
            btnAddNewRow.setText(addOptionTitleText);
            inputFields.setVisibility(View.GONE);
            option.setVisibility(View.VISIBLE);
            btnMinus.setVisibility(View.VISIBLE);
        }

        btnAddNewRow.setOnClickListener(v -> {
            inputFields.setVisibility(View.VISIBLE);
            btnAddNewRow.setVisibility(View.GONE);
            option.setVisibility(View.VISIBLE);
            btnMinus.setVisibility(View.VISIBLE);
            option.requestFocus();
            UiUtil.toggleKeyboard(this, option);
            ContactOptionTypeClickListener optionTypeClickListener = new ContactOptionTypeClickListener(
                    EditContactDetailsActivity.this, getSupportFragmentManager(),
                    newOptionRowView, optionTitleText,
                    standardOptionUIValues, standardOptionValues);
            btnOptionType.setOnClickListener(optionTypeClickListener);
            optionTitle.setOnClickListener(optionTypeClickListener);
            rootView.addView(createNewVCardOptionRow(optionTitleText, editTextHint, addOptionTitleText,
                    standardOptionUIValues, standardOptionValues, rootView, singleLine, inputType));
        });

        btnMinus.setOnClickListener(v -> {
            VCardLinearLayout viewParent = (VCardLinearLayout) newOptionRowView.getParent();
            int childCount = viewParent.getChildCount();
            if (childCount <= 2) {
                option.setText("");
            } else {
                viewParent.removeView(newOptionRowView);
            }
        });

        return newOptionRowView;
    }

    private View createNewVCardAddressRow(final String titleText, final String optionTitleText,
                                          final List<String> standardOptionUIValues,
                                          final List<String> standardOptionValues,
                                          final VCardLinearLayout rootView) {
        return new ContactAddressView(this, titleText, optionTitleText, standardOptionUIValues, standardOptionValues, rootView);
    }

    private void onConnectivityEvent(Constants.ConnectionState connectivity) {
        Timber.v("onConnectivityEvent hasConnectivity:%s", connectivity.name());
        if (connectivity != Constants.ConnectionState.CONNECTED) {
            networkSnackBarUtil.getNoConnectionSnackBar(
                    mSnackLayout,
                    mUserManager.getUser(),
                    this,
                    null,
                    null,
                    connectivity == Constants.ConnectionState.NO_INTERNET
            ).show();
        } else {
            networkSnackBarUtil.hideAllSnackBars();
        }
    }

    @NotNull
    private Function0<Unit> onConnectivityCheckRetry() {
        return () -> {
            networkSnackBarUtil.getCheckingConnectionSnackBar(mSnackLayout, null).show();
            viewModel.checkConnectivityDelayed();
            return null;
        };
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
    }

    @Subscribe
    public void onContactEvent(ContactEvent event) {
        switch (event.status) {
            case ContactEvent.SUCCESS:
            case ContactEvent.SAVED:
                TextExtensions.showToast(this, R.string.contact_saved, Toast.LENGTH_SHORT);
                new Handler().postDelayed(this::saveAndFinish, 500);
                break;
            case ContactEvent.ERROR:
                TextExtensions.showToast(this, R.string.error);
                break;
            case ContactEvent.NO_NETWORK:
                TextExtensions.showToast(this, R.string.contact_saved_offline);
                new Handler().postDelayed(this::saveAndFinish, 500);
            default:
                break;
        }
    }

    private void saveAndFinish() {
        saveLastInteraction();
        setResult(RESULT_OK);
        finish();
    }

    private View createEmailAddressView(String emailType, String emailUIType, String emailValue) {
        View view = createExistingVCardOptionRow(emailType, "\ue914 " + emailUIType, emailValue,
                getString(R.string.contact_vcard_hint_email), viewModel.getEmailUIOptions(),
                viewModel.getEmailOptions(), mEmailAddressesContainer, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        mEmailAddressesContainer.addView(view);
        return view;
    }

    private void createPhoneView(String phoneType, String phoneUIType, String phoneValue) {
        View view = createExistingVCardOptionRow(phoneType, "\ue913 " + phoneUIType, phoneValue,
                getString(R.string.contact_vcard_hint_phone), viewModel.getPhoneUIOptions(),
                viewModel.getPhoneOptions(), mEncryptedDataPhone, true, 1,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_PHONE);
        mEncryptedDataPhone.addView(view);
    }

    private void createAddressView(String addressType, String addressUIType, Address address) {
        View view = createExistingVCardOptionRow(addressType, addressUIType, address,
                getString(R.string.contact_vcard_hint_address), viewModel.getAddressUIOptions(),
                viewModel.getAddressOptions(), mEncryptedDataAddress);
        mEncryptedDataAddress.addView(view);
    }

    private View createOtherView(String otherInformationType, String otherInformationValue,
                                 Constants.VCardOtherInfoType otherInfoType) {
        View view = createExistingVCardOptionRow(otherInformationType, "\ue905 " + otherInformationType,
                otherInformationValue, getString(R.string.contact_vcard_hint_other),
                viewModel.getOtherOptions(), viewModel.getOtherOptions(), mEncryptedDataOther, false, 1,
                InputType.TYPE_CLASS_TEXT);
        view.setTag(otherInfoType);
        mEncryptedDataOther.addView(view);
        return view;
    }

    private View.OnClickListener mUpgradeClickListener = v -> {
        Intent upgradeIntent = new Intent(EditContactDetailsActivity.this, UpsellingActivity.class);
        upgradeIntent.putExtra(UpsellingActivity.EXTRA_OPEN_UPGRADE_CONTAINER, true);
        startActivityForResult(AppUtil.decorInAppIntent(upgradeIntent), REQUEST_CODE_UPGRADE);
    };

    private void showUnsavedChangesDialog() {
        new UnsavedChangesDialog(this, () -> {
            saveAndFinish();
            return null;
        }, () -> null);
    }

    private class DirtyWatcher implements TextWatcher {
        private boolean firstChange;

        DirtyWatcher() {
            super();
            firstChange = true;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            if (!firstChange) {
                viewModel.setChanged();
            }
            firstChange = false;
        }
    }

    private void startObserving() {
        viewModel.getCleanUpComplete().observe(this, booleanEvent -> {
            if (booleanEvent == null) {
                return;
            }
            Boolean changed = booleanEvent.getContentIfNotHandled();
            if (changed != null && changed) {
                showUnsavedChangesDialog();
            } else {
                super.onBackPressed();
            }
        });
        viewModel.getSetupNewContactFlow().observe(this, setupNewContactObserver);
        viewModel.getSetupEditContactFlow().observe(this, setupEditContactObserver);
        viewModel.getSetupConvertContactFlow().observe(this, setupConvertContactObserver);
        viewModel.getFreeUserEvent().observe(this, unit -> {
            View upgradeView = mUpgradeEncryptedDataStub.inflate();
            upgradeView.findViewById(R.id.upgrade).setOnClickListener(mUpgradeClickListener);

            new Handler().postDelayed(() -> enableControls(false, mEncryptedDataContainer), 10);
        });
        viewModel.getProfilePicture().observe(this, observer -> {
            observer.doOnData(bitmap -> {
                photoCardViewWrapper.setVisibility(View.VISIBLE);
                contactInitials.setVisibility(View.GONE);
                contactPhoto.setImageBitmap(bitmap);
                return Unit.INSTANCE;
            });
            observer.doOnError(error -> {
                photoCardViewWrapper.setVisibility(View.GONE);
                contactInitials.setVisibility(View.VISIBLE);
                TextExtensions.showToast(this, error);
                return Unit.INSTANCE;
            });

            return Unit.INSTANCE;
        });
        viewModel.getHasConnectivity().observe(this, this::onConnectivityEvent);
    }

    private Observer setupNewContactObserver = (Observer<String>) email -> {
        getSupportActionBar().setTitle(R.string.add_contact);
        createEmailAddressView(viewModel.getDefaultEmailOption(), viewModel.getDefaultEmailUIOption(), email);
        initEmptyEmailView();
        createPhoneView(viewModel.getDefaultPhoneOption(), viewModel.getDefaultPhoneUIOption(), "");
        initNewPhone();
        createAddressView(viewModel.getDefaultAddressOption(), viewModel.getDefaultAddressUIOption(), new Address());
        initNewAddressRow();
        createOtherView(viewModel.getDefaultOtherOption(), "", Constants.VCardOtherInfoType.ORGANIZATION);
        initNewOptionRow();
        initExistingOptionsRow();

    };

    private Observer setupEditContactObserver = (Observer<EditContactDetailsViewModel.EditContactCardsHolder>) editContactCardsHolder -> {
        mProgressBar.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(R.string.edit_contact);
        fillTopPart(editContactCardsHolder.getVCardType0(), editContactCardsHolder.getVCardType2());
        initEmptyEmailView();
        VCard vCardType3 = editContactCardsHolder.getVCardType3();
        List<Telephone> vCardPhones = vCardType3.getTelephoneNumbers();
        if (vCardPhones != null && vCardPhones.size() > 0) {
            for (Telephone phone : vCardPhones) {
                List<TelephoneType> types = phone.getTypes();
                String typeUIPhone = viewModel.getDefaultPhoneUIOption();
                String typePhone = viewModel.getDefaultPhoneOption();
                if (types != null && types.size() > 0) {
                    typePhone = types.iterator().next().getValue();
                    typeUIPhone = VCardUtil.capitalizeType(
                            VCardUtil.removeCustomPrefixForCustomType(typePhone));
                }
                createPhoneView(typePhone, typeUIPhone, phone.getText());
            }
        } else {
            createPhoneView(viewModel.getDefaultPhoneOption(), viewModel.getDefaultPhoneUIOption(), "");
        }
        initPhone();
        initAddresses(vCardType3);
        initNewAddressRow();

        List<Photo> vCardPhotos = viewModel.getPhotos();
        List<Organization> vCardOrganizations = viewModel.getOrganizations();
        List<Title> vCardTitles = viewModel.getTitles();
        List<Nickname> vCardNicknames = viewModel.getNicknames();
        List<Birthday> vCardBirthdays = viewModel.getBirthdays();
        List<Anniversary> vCardAnniversary = viewModel.getAnniversaries();
        List<Role> vCardRoles = viewModel.getRoles();
        List<Url> vCardUrls = viewModel.getUrls();
        Gender vCardGender = viewModel.getGender();
        List<RawProperty> vCardCustomFields = viewModel.getExtendedPropertiesType3();

        if (vCardPhotos != null && vCardPhotos.size() > 0) {
            photoCardViewWrapper.setVisibility(View.VISIBLE);
            contactInitials.setVisibility(View.GONE);
            if (vCardPhotos.get(0).getData() != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(vCardPhotos.get(0).getData(), 0, vCardPhotos.get(0).getData().length);
                contactPhoto.setImageBitmap(bmp);
            } else if (!vCardPhotos.get(0).getUrl().isEmpty()) {
                photoCardViewWrapper.setVisibility(View.GONE);
                contactInitials.setVisibility(View.VISIBLE);
                if (mNetworkUtil.isConnected()) {
                    viewModel.getBitmapFromURL(vCardPhotos.get(0).getUrl());
                }
            }
        }

        if ((vCardOrganizations != null && !vCardOrganizations.isEmpty()) || (vCardTitles != null && !vCardTitles
                .isEmpty()) || (vCardNicknames != null && !vCardNicknames.isEmpty()) || (vCardBirthdays != null && !vCardBirthdays
                .isEmpty()) || (vCardAnniversary != null && !vCardAnniversary.isEmpty()) || (vCardRoles != null && !vCardRoles
                .isEmpty()) || (vCardUrls != null && !vCardUrls.isEmpty()) || vCardGender != null || (vCardCustomFields != null && !vCardCustomFields
                .isEmpty())) {
            //region other info
            if (vCardOrganizations != null && vCardOrganizations.size() > 0) {
                for (Organization organization : vCardOrganizations) {
                    String organizationType = getString(R.string.vcard_other_option_org);
                    String organizationValue = organization.getValues().get(0);
                    createOtherView(organizationType, organizationValue, Constants.VCardOtherInfoType.ORGANIZATION);
                }
            }

            if (vCardTitles != null && vCardTitles.size() > 0) {
                for (Title title : vCardTitles) {
                    createOtherView(getString(R.string.vcard_other_option_title), title.getValue(), Constants.VCardOtherInfoType.TITLE);
                }
            }

            if (vCardNicknames != null && vCardNicknames.size() > 0) {
                for (Nickname nickname : vCardNicknames) {
                    String nicknameValue = nickname.getValues().get(0);
                    createOtherView(nickname.getType(), nicknameValue, Constants.VCardOtherInfoType.NICKNAME);
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
                        View view = createOtherView(getString(R.string.vcard_other_option_birthday), value, Constants.VCardOtherInfoType.BIRTHDAY);
                        final TextView birthdayValue = view.findViewById(R.id.option);
                        birthdayValue.setFocusable(false);
                        birthdayValue.setFocusableInTouchMode(false);
                        birthdayValue.setOnClickListener(new ContactBirthdayClickListener(this, getSupportFragmentManager()));
                    }
                }
            }

            if (vCardCustomFields != null && vCardCustomFields.size() > 0) {
                for (RawProperty rawProperty : vCardCustomFields) {
                    String name = rawProperty.getPropertyName();
                    String value = rawProperty.getValue();
                    if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                        createOtherView(name, value, Constants.VCardOtherInfoType.CUSTOM);
                    }
                }
            }

            if (vCardAnniversary != null && vCardAnniversary.size() > 0) {
                for (Anniversary anniversary : vCardAnniversary) {
                    Date anniversaryDate = anniversary.getDate();
                    PartialDate anniversariPartialDate = anniversary.getPartialDate();
                    String anniversaryText = anniversary.getText();
                    String value = "";
                    if (anniversaryDate != null) {
                        value = DateUtil.formatDate(anniversaryDate);
                    } else if (anniversariPartialDate != null) {
                        value = anniversariPartialDate.toISO8601(false);
                    } else if (!TextUtils.isEmpty(anniversaryText)) {
                        value = anniversaryText;
                    }
                    createOtherView(getString(R.string.vcard_other_option_anniversary), value, Constants.VCardOtherInfoType.ANNIVERSARY);
                }
            }

            if (vCardRoles != null && vCardRoles.size() > 0) {
                for (Role role : vCardRoles) {
                    createOtherView(getString(R.string.vcard_other_option_role), role.getValue(), Constants.VCardOtherInfoType.ROLE);
                }
            }

            if (vCardUrls != null && vCardUrls.size() > 0) {
                for (Url url : vCardUrls) {
                    createOtherView(getString(R.string.vcard_other_option_url), url.getValue(), Constants.VCardOtherInfoType.URL);
                }
            }

            if (vCardGender != null) {
                createOtherView(getString(R.string.vcard_other_option_gender),
                        vCardGender.getGender(), Constants.VCardOtherInfoType.GENDER);
            }
            //endregion
        } else {
            createOtherView(viewModel.getDefaultOtherOption(), "", Constants.VCardOtherInfoType.ORGANIZATION);
        }
        initNewOptionRow();

        List<Note> vCardNotes = viewModel.getNotes();
        if (vCardNotes != null && vCardNotes.size() > 0) {
            for (Note note : vCardNotes) {
                mEncryptedDataNote.addView(
                        createExistingVCardOptionRow(getString(R.string.contact_vcard_note),
                                getString(R.string.contact_vcard_note), note.getValue(),
                                getString(R.string.contact_vcard_hint_note), null, null,
                                mEncryptedDataNote, false, 4,
                                InputType.TYPE_TEXT_FLAG_MULTI_LINE));
            }
        } else {
            mEncryptedDataNote.addView(
                    createExistingVCardOptionRow(getString(R.string.contact_vcard_note),
                            getString(R.string.contact_vcard_note), "",
                            getString(R.string.contact_vcard_hint_note), null, null,
                            mEncryptedDataNote, false, 4, InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        }
    };

    private Observer setupConvertContactObserver = o -> {
        getSupportActionBar().setTitle(R.string.convert_contact);
        LocalContact localContact = viewModel.getLocalContact();
        mDisplayNameView.setText(localContact.getName());
        contactInitials.setText(UiUtil.extractInitials(localContact.getName()));

        List<String> emails = localContact.getEmails();
        List<String> phones = localContact.getPhones();
        List<LocalContactAddress> addresses = localContact.getAddresses();
        if (emails.size() > 0) {
            for (String email : emails) {
                createEmailAddressView(viewModel.getDefaultEmailOption(), viewModel.getDefaultEmailUIOption(), email);
            }
        } else {
            createEmailAddressView(viewModel.getDefaultEmailOption(), viewModel.getDefaultEmailUIOption(), "");
        }
        if (phones.size() > 0) {
            for (String phone : phones) {
                createPhoneView(viewModel.getDefaultPhoneOption(), viewModel.getDefaultPhoneUIOption(), phone);
            }
        } else {
            createPhoneView(viewModel.getDefaultPhoneOption(), viewModel.getDefaultPhoneUIOption(), "");
        }
        if (addresses.size() > 0) {
            for (LocalContactAddress localContactAddress : addresses) {
                Address address = new Address();
                address.setStreetAddress(localContactAddress.getStreet());
                address.setLocality(localContactAddress.getCity());
                address.setRegion(localContactAddress.getRegion());
                address.setPostalCode(localContactAddress.getPostcode());
                address.setCountry(localContactAddress.getCountry());
                createAddressView(viewModel.getDefaultAddressOption(), viewModel.getDefaultAddressUIOption(), address);
            }
        } else {
            createAddressView(viewModel.getDefaultAddressOption(), viewModel.getDefaultAddressUIOption(), new Address());
        }
        initEmptyEmailView();
        initPhone();
        initNewAddressRow();
        createOtherView(viewModel.getDefaultOtherOption(), "", Constants.VCardOtherInfoType.ORGANIZATION);
        initNewOptionRow();
        initExistingOptionsRow();
    };

    private Observer setupCompleteObserver = (Observer<Event<Boolean>>) event -> {
        viewModel.fetchContactGroupsForEmails();
    };

    // region helper methods
    private void initPhone() {
        mEncryptedDataPhone.addView(
                createNewVCardOptionRow("\ue913 " + viewModel.getDefaultPhoneUIOption(), getString(R.string.contact_vcard_hint_phone), getString(R.string.contact_vcard_new_row_phone),
                        viewModel.getPhoneUIOptions(), viewModel.getPhoneOptions(),
                        mEncryptedDataPhone, false, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_PHONE));
    }

    private void initNewAddressRow() {
        mEncryptedDataAddress.addView(createNewVCardAddressRow(getString(R.string.contact_vcard_new_row_address),
                viewModel.getDefaultAddressUIOption(),
                viewModel.getAddressUIOptions(),
                viewModel.getAddressOptions(), mEncryptedDataAddress));
    }

    private void initAddresses(VCard vCardType3) {
        List<Address> vCardAddresses = vCardType3.getAddresses();
        if (vCardAddresses != null && vCardAddresses.size() > 0) {
            for (Address address : vCardAddresses) {
                List<AddressType> addressTypes = address.getTypes();
                String typeUIAddress = viewModel.getDefaultAddressUIOption();
                String typeAddress = viewModel.getDefaultAddressOption();
                if (addressTypes != null && addressTypes.size() > 0) {
                    typeAddress = addressTypes.iterator().next().getValue();
                    typeUIAddress = VCardUtil.capitalizeType(
                            VCardUtil.removeCustomPrefixForCustomType(typeAddress));
                }
                createAddressView(typeAddress, typeUIAddress, address);
            }
        } else {
            createAddressView(viewModel.getDefaultAddressOption(), viewModel.getDefaultAddressUIOption(), new Address());
        }
    }

    private void initNewOptionRow() {
        mEncryptedDataOther.addView(createNewVCardOptionRow("\ue905 " + viewModel.getDefaultOtherOption(), getString(R.string.contact_vcard_hint_other), getString(R.string.contact_vcard_new_row_other),
                viewModel.getOtherOptions(), viewModel.getOtherOptions(),
                mEncryptedDataOther, false, InputType.TYPE_CLASS_TEXT));
    }

    private void initExistingOptionsRow() {
        mEncryptedDataNote.addView(createExistingVCardOptionRow(getString(R.string.contact_vcard_note),
                getString(R.string.contact_vcard_note), "",
                getString(R.string.contact_vcard_hint_note), null, null,
                mEncryptedDataNote, false, 4, InputType.TYPE_TEXT_FLAG_MULTI_LINE));
    }

    private void initEmptyEmailView() {
        mEmailAddressesContainer.addView(createNewVCardOptionRow("\ue914 " + viewModel.getDefaultEmailUIOption(), getString(R.string.contact_vcard_hint_email), getString(R.string.contact_vcard_new_row_email),
                viewModel.getEmailUIOptions(), viewModel.getEmailOptions(),
                mEmailAddressesContainer, true, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS));
    }

    private void initNewPhone() {
        mEncryptedDataPhone.addView(
                createNewVCardOptionRow(getString(R.string.contact_vcard_new_row_phone),
                        "\ue913 " + viewModel.getDefaultPhoneUIOption(), getString(R.string.contact_vcard_new_row_phone),
                        viewModel.getPhoneUIOptions(),
                        viewModel.getPhoneOptions(), mEncryptedDataPhone, false,
                        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_PHONE));
    }
    // endregion

    private void enableControls(boolean enable, View v){

        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            v.setEnabled(enable);
            v.setFocusable(enable);
            return ;
        }

        ViewGroup vg = (ViewGroup) v;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            enableControls(enable, child);
        }
    }
}

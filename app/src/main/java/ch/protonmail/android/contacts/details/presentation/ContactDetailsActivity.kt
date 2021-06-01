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

package ch.protonmail.android.contacts.details.presentation

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsUiItem
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsViewState
import ch.protonmail.android.databinding.ActivityContactDetailsBinding
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.views.ListItemThumbnail
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber


@AndroidEntryPoint
class ContactDetailsActivity : AppCompatActivity() {

    private lateinit var thumbnailImage: ImageView
    private lateinit var detailsAdapter: ContactDetailsAdapter
    private lateinit var thumbnail: ListItemThumbnail
    private lateinit var contactNameView: TextView
    private lateinit var detailsContainer: NestedScrollView
    private lateinit var progressBar: ProgressBar
    private var vCardToShare: String = EMPTY_STRING
    private var contactEmail: String = EMPTY_STRING
    private var contactPhone: String = EMPTY_STRING
    private val viewModel: ContactDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityContactDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar as Toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.contact_details)
        }

        progressBar = binding.progressBarContactDetails
        detailsContainer = binding.scrollViewContactDetails
        contactNameView = binding.textViewContactDetailsContactName
        thumbnail = binding.thumbnailContactDetails
        thumbnailImage = binding.imageViewContactDetailsThumbnail

        val itemDecoration = DividerItemDecoration(
            this,
            LinearLayout.VERTICAL
        ).apply {
            getDrawable(R.drawable.list_divider)?.let {
                setDrawable(it)
            }
        }

        detailsAdapter = ContactDetailsAdapter(
            ::onWriteToContact,
            ::onCallContact,
            ::onAddressClicked,
            ::onUrlClicked
        )
        binding.recyclerViewContactsDetails.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = detailsAdapter
            addItemDecoration(itemDecoration)
        }

        binding.includeContactDetailsButtons.textViewContactDetailsCompose.setOnClickListener {
            onWriteToContact(contactEmail)
        }

        binding.includeContactDetailsButtons.textViewContactDetailsCall.setOnClickListener {
            onCallContact(contactPhone)
        }

        binding.includeContactDetailsButtons.textViewContactDetailsShare.setOnClickListener {
            onShare(contactNameView.text.toString(), vCardToShare, this)
        }

        viewModel.contactsViewState
            .onEach { renderState(it) }
            .launchIn(lifecycleScope)

        viewModel.vCardShareFlow
            .onEach { shareVcard(it, contactNameView.text.toString()) }
            .launchIn(lifecycleScope)

        val contactId = requireNotNull(intent.extras?.getString(EXTRA_ARG_CONTACT_ID))
        viewModel.getContactDetails(contactId)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_contact_details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_contact_details_edit -> onEditContacts()
            R.id.action_contact_details_delete ->
                onDeleteContact(
                    requireNotNull(intent.extras?.getString(EXTRA_ARG_CONTACT_ID)),
                    contactNameView.text.toString()
                )
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun onEditContacts() {
        // TODO: Display in edit mode here
        showToast(R.string.edit_contact, Toast.LENGTH_SHORT, Gravity.CENTER)
    }

    private fun onDeleteContact(contactId: String, contactName: String) {
        val clickListener = DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                viewModel.deleteContact(contactId)
            }
            dialog.dismiss()
        }
        if (!isFinishing) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.confirm)
                .setMessage(String.format(getString(R.string.delete_contact), contactName))
                .setNegativeButton(R.string.no, clickListener)
                .setPositiveButton(R.string.yes, clickListener)
                .create()
                .show()
        }
    }

    private fun renderState(state: ContactDetailsViewState) {
        Timber.v("State $state received")
        when (state) {
            is ContactDetailsViewState.Loading -> showLoading()
            is ContactDetailsViewState.Error -> showError(state.exception)
            is ContactDetailsViewState.Data -> showData(state)
        }
    }

    private fun showError(exception: Throwable) {
        progressBar.isVisible = false
        detailsContainer.isVisible = false
        Timber.i(exception, "Fetching contacts data has failed")
    }

    private fun showLoading() {
        progressBar.isVisible = true
        detailsContainer.isVisible = false
    }

    private fun showData(state: ContactDetailsViewState.Data) {
        progressBar.isVisible = false
        detailsContainer.isVisible = true

        vCardToShare = state.vCardToShare

        setHeaderData(
            state.title,
            state.initials,
            state.photoUrl,
            state.photoBytes
        )

        detailsAdapter.submitList(state.contactDetailsItems)

        state.contactDetailsItems.onEach { item ->
            setContactDataForActionButtons(item)
        }
    }

    private fun setHeaderData(
        title: String,
        initials: String,
        photoUrl: String?,
        photoBytes: List<Byte>?
    ) {
        contactNameView.text = title
        thumbnail.bind(isSelectedActive = false, isMultiselectActive = false, initials = initials)

        if (!photoBytes.isNullOrEmpty()) {
            setThumbnailImage(photoBytes)
        } else if (photoUrl != null) {
            loadThumbnailImage(photoUrl)
        }
    }

    private fun setThumbnailImage(photoBytes: List<Byte>) {
        val byteArray = photoBytes.toByteArray()
        val imageBitmap: Bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        val roundedImage = RoundedBitmapDrawableFactory.create(resources, imageBitmap).apply {
            setAntiAlias(true)
            cornerRadius = resources.getDimensionPixelSize(R.dimen.avatar_size) / 2f
        }
        thumbnailImage.apply {
            setImageDrawable(roundedImage)
            isVisible = true
        }
        thumbnail.isVisible = false
    }

    private fun loadThumbnailImage(photoUrl: String?) {
        Timber.v("Loading photo url: $photoUrl")
        val targetSize = resources.getDimensionPixelSize(R.dimen.avatar_size)
        ImageRequest.Builder(this)
            .data(photoUrl)
            .size(targetSize, targetSize)
            .transformations(CircleCropTransformation())
            .target(
                onSuccess = { drawable ->
                    Timber.d("Thumbnail loading finished")
                    thumbnailImage.apply {
                        setImageDrawable(drawable)
                        isVisible = true
                    }
                    thumbnail.isVisible = false
                },
                onError = {
                    Timber.i("Thumbnail loading error")
                    thumbnailImage.isVisible = false
                    thumbnail.isVisible = true
                }
            )
    }

    private fun setContactDataForActionButtons(item: ContactDetailsUiItem) {
        if (item is ContactDetailsUiItem.Email) {
            contactEmail = item.value
        }
        if (item is ContactDetailsUiItem.TelephoneNumber) {
            contactPhone = item.value
        }
    }

    private fun onWriteToContact(emailAddress: String) {
        if (emailAddress.isNotEmpty()) {
            val intent = Intent(this, ComposeMessageActivity::class.java)
            intent.putExtra(BaseActivity.EXTRA_IN_APP, true)
            intent.putExtra(ComposeMessageActivity.EXTRA_TO_RECIPIENTS, arrayOf(emailAddress))
            startActivity(intent)
        } else {
            showToast(R.string.email_empty, Toast.LENGTH_SHORT)
        }
    }

    private fun onCallContact(phoneNumber: String) {
        Timber.v("On Call $phoneNumber")
        if (phoneNumber.isNotEmpty()) {
            val callIntent = Intent(Intent.ACTION_DIAL)
            callIntent.data = Uri.parse("tel:$phoneNumber")
            startActivity(callIntent)
        } else {
            showToast(R.string.contact_vcard_new_row_phone, Toast.LENGTH_SHORT)
        }
    }

    private fun onShare(contactName: String, vCardToShare: String, context: Context) {
        if (vCardToShare.isNotEmpty()) {
            viewModel.saveVcard(vCardToShare, contactName, context)
        } else {
            showToast(R.string.default_error_message, Toast.LENGTH_SHORT)
        }
    }

    private fun shareVcard(vcfFileUri: Uri, contactName: String) {
        if (vcfFileUri != Uri.EMPTY) {
            Timber.v("Share contact uri: $vcfFileUri")
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, vcfFileUri)
                type = ContactsContract.Contacts.CONTENT_VCARD_TYPE
            }
            startActivity(Intent.createChooser(intent, contactName))
        }
    }

    private fun onAddressClicked(address: String) {
        onUrlClicked("geo:0,0?q=$address")
    }

    private fun onUrlClicked(url: String) {
        Timber.v("On Url clicked $url")
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        startActivity(intent)
    }

    companion object {

        const val EXTRA_ARG_CONTACT_ID = "extra_arg_contact_id"
    }
}

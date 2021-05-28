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

import android.os.Bundle
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsUiItem
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsViewState
import ch.protonmail.android.databinding.ActivityContactDetailsBinding
import ch.protonmail.android.views.ListItemThumbnail
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@AndroidEntryPoint
class ContactDetailsActivity : AppCompatActivity() {

    private lateinit var thumbnail: ListItemThumbnail
    private lateinit var contactName: TextView
    private lateinit var detailsContainer: NestedScrollView
    private lateinit var progressBar: ProgressBar
    private val viewModel: ContactDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityContactDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.includeToolbar.toolbar as Toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.contact_details)
        }

        progressBar = binding.progressBarContactDetails
        detailsContainer = binding.scrollViewContactDetails
        contactName = binding.textViewContactDetailsContactName
        thumbnail = binding.thumbnailContactDetails

        viewModel.contactsViewState
            .onEach { renderState(it) }
            .launchIn(lifecycleScope)

        val contactId = requireNotNull(intent.extras?.getString(EXTRA_ARG_CONTACT_ID))
        viewModel.getContactDetails(contactId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun renderState(state: ContactDetailsViewState) {
        Timber.v("State $state received")
        when (state) {
            is ContactDetailsViewState.Loading -> showLoading()
            is ContactDetailsViewState.Error -> showError(state.exception)
            is ContactDetailsViewState.Data -> showData(
                state
            )
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

        state.contactDetailsItems.onEach { item ->
            when (item) {
                is ContactDetailsUiItem.HeaderData -> {
                    setHeaderData(item)
                }
                else -> showDetail(item)
            }
        }
    }

    private fun showDetail(item: ContactDetailsUiItem) {
        Timber.v("Detail item $item")
        // TODO : add to recycler view
    }

    private fun setHeaderData(item: ContactDetailsUiItem.HeaderData) {
        contactName.text = item.title
        thumbnail.bind(isSelectedActive = false, isMultiselectActive = false, initials = item.initials)
    }


    companion object {

        const val EXTRA_ARG_CONTACT_ID = "extra_arg_contact_id"
    }
}

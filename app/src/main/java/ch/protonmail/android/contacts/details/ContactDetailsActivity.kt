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

package ch.protonmail.android.contacts.details

import android.os.Bundle
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.databinding.ActivityContactDetailsBinding
import ch.protonmail.android.usecase.model.FetchContactDetailsResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@AndroidEntryPoint
class ContactDetailsActivity : AppCompatActivity() {

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

        viewModel.contactsResultFlow
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

    private fun renderState(state: FetchContactDetailsResult) {
        Timber.v("State $state received")
        when (state) {
            is FetchContactDetailsResult.Loading -> showLoading()
            is FetchContactDetailsResult.Error -> showError(state.exception)
            is FetchContactDetailsResult.Data -> showData(
                state.decryptedVCardType0,
                state.decryptedVCardType2,
                state.decryptedVCardType3,
                state.vCardType2Signature,
                state.vCardType3Signature
            )
        }
    }

    private fun showData(
        decryptedVCardType0: String,
        decryptedVCardType2: String,
        decryptedVCardType3: String,
        vCardType2Signature: String,
        vCardType3Signature: String
    ) {
        progressBar.isVisible = false
        detailsContainer.isVisible = true
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

    companion object {

        const val EXTRA_ARG_CONTACT_ID = "extra_arg_contact_id"
    }
}

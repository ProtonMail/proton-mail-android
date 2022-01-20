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
package ch.protonmail.android.contacts.groups.edit.chooser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.contacts.groups.ContactGroupEmailsAdapter
import ch.protonmail.android.contacts.groups.GroupsItemAdapterMode
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.utils.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.content_contact_group_details.*
import javax.inject.Inject

// region constants
const val EXTRA_CONTACT_EMAILS = "extra_contact_emails"
// endregion

@AndroidEntryPoint
class AddressChooserActivity : BaseActivity() {

    @Inject
    lateinit var addressChooserViewModelFactory: AddressChooserViewModelFactory
    private lateinit var addressChooserViewModel: AddressChooserViewModel
    private lateinit var contactGroupEmailsAdapter: ContactGroupEmailsAdapter

    override fun getLayoutId(): Int = R.layout.activity_address_chooser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addressChooserViewModel = ViewModelProvider(this, addressChooserViewModelFactory)
            .get(AddressChooserViewModel::class.java)
        setupToolbar()
        initAdapter()
        initFilterView()
        addressChooserViewModel.contactGroupEmailsResult.observe(
            this,
            {
                contactGroupEmailsAdapter.setData(it ?: ArrayList())
            }
        )
        addressChooserViewModel.contactGroupEmailsEmpty.observe(
            this,
            {
                contactGroupEmailsAdapter.setData(ArrayList())
            }
        )
        val selected: ArrayList<ContactEmail> =
            ArrayList(intent.getSerializableExtra(EXTRA_CONTACT_EMAILS) as HashSet<ContactEmail>)
        addressChooserViewModel.getAllEmails(HashSet(selected))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.done_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        R.id.action_save -> {
            val selected = addressChooserViewModel.updateSelected(
                contactGroupEmailsAdapter.getUnSelected(),
                contactGroupEmailsAdapter.getSelected()
            )
            val intent = Intent()
            intent.putExtra(EXTRA_CONTACT_EMAILS, selected)
            setResult(Activity.RESULT_OK, intent)
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initAdapter() {
        contactGroupEmailsAdapter =
            ContactGroupEmailsAdapter(this, ArrayList(), null, mode = GroupsItemAdapterMode.CHECKBOXES)
        with(contactGroupEmailsAdapter) {
            registerAdapterDataObserver(
                ch.protonmail.android.utils.ui.RecyclerViewEmptyViewSupport(
                    contactEmailsRecyclerView,
                    noResults
                )
            )
        }
        with(contactEmailsRecyclerView) {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@AddressChooserActivity)
            adapter = contactGroupEmailsAdapter
        }
    }

    private fun initFilterView() {
        filterView.apply {
            UiUtil.setTextViewDrawableColor(this@AddressChooserActivity, this, R.color.lead_gray)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(editable: Editable?) {
                    addressChooserViewModel.doFilter(
                        filterView.text.toString(), contactGroupEmailsAdapter.getSelected()
                    )
                }
            })
        }
    }
}

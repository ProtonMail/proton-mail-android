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
package ch.protonmail.android.contacts.groups.edit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.contacts.UnsavedChangesDialog
import ch.protonmail.android.contacts.groups.ContactGroupEmailsAdapter
import ch.protonmail.android.contacts.groups.GroupsItemAdapterMode
import ch.protonmail.android.contacts.groups.details.EXTRA_CONTACT_GROUP
import ch.protonmail.android.contacts.groups.edit.chooser.AddressChooserActivity
import ch.protonmail.android.contacts.groups.edit.chooser.ColorChooserFragment
import ch.protonmail.android.contacts.groups.edit.chooser.EXTRA_CONTACT_EMAILS
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.content_edit_create_contact_group_header.*
import java.util.ArrayList
import java.util.HashSet
import java.util.Random
import javax.inject.Inject

// region constants
private const val REQUEST_CODE_ADDRESSES = 1
// endregion

@AndroidEntryPoint
class ContactGroupEditCreateActivity : BaseActivity(), ColorChooserFragment.IColorChooserListener {

    @Inject
    lateinit var contactGroupEditCreateViewModelFactory: ContactGroupEditCreateViewModelFactory
    private lateinit var contactGroupEditCreateViewModel: ContactGroupEditCreateViewModel
    private lateinit var contactGroupEmailsAdapter: ContactGroupEmailsAdapter

    override fun colorChosen(@ColorInt color: Int) {
        contactGroupEditCreateViewModel.setGroupColor(color)
        setColor()
        contactGroupEditCreateViewModel.setChanged()
    }

    override fun getLayoutId(): Int = if (intent?.extras?.containsKey(EXTRA_CONTACT_GROUP)!!) {
        R.layout.activity_edit_contact_group
    } else {
        R.layout.activity_create_contact_group
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactGroupEditCreateViewModel = ViewModelProviders.of(this, contactGroupEditCreateViewModelFactory)
            .get(ContactGroupEditCreateViewModel::class.java)
        contactGroupEditCreateViewModel.setData(intent?.extras?.getParcelable(EXTRA_CONTACT_GROUP))
        setColor()
        setName()
        contactGroupEditCreateViewModel.contactGroupSetupLayout.observe(
            this,
            { event ->
                event?.getContentIfNotHandled()?.let { contactGroupMode ->
                    when (contactGroupMode) {
                        ContactGroupMode.EDIT -> setupEditContactGroupLayout()
                        ContactGroupMode.CREATE -> setupNewContactGroupLayout()
                    }
                    manageMembers.setOnClickListener {
                        val intent = Intent(this@ContactGroupEditCreateActivity, AddressChooserActivity::class.java)
                        intent.putExtra(EXTRA_CONTACT_EMAILS, contactGroupEmailsAdapter.getData())
                        val addressChooserIntent = AppUtil.decorInAppIntent(intent)
                        startActivityForResult(addressChooserIntent, REQUEST_CODE_ADDRESSES)
                    }
                    chooseGroupColor.setOnClickListener {
                        showColorsDialog()
                    }
                }
            }
        )
        initAdapter()
        startObserving()
    }

    override fun onStart() {
        super.onStart()
        ProtonMailApplication.getApplication().bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        ProtonMailApplication.getApplication().bus.unregister(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.done_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_save -> {
                progress.visibility = View.VISIBLE
                contactGroupEditCreateViewModel.contactGroupUpdateResult.observe(
                    this,
                    {
                        progress.visibility = View.GONE
                        it?.getContentIfNotHandled()?.let {
                            val status = it.status
                            when (status) {
                                Status.FAILED -> showToast(it.message ?: getString(R.string.error))
                                Status.SUCCESS -> {
                                    showToast(R.string.contact_group_saved)
                                    saveLastInteraction()
                                    finish()
                                }
                                Status.UNAUTHORIZED -> showToast(R.string.paid_plan_needed)
                                Status.VALIDATION_FAILED -> showToast(R.string.save_group_validation_error)
                                else -> {
                                    showToast(R.string.error)
                                }
                            }
                        }
                    }
                )
                contactGroupEditCreateViewModel.save(contactGroupName.text.toString())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        contactGroupEditCreateViewModel.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_ADDRESSES) {
            data?.let {
                val selected = data.extras?.getSerializable(EXTRA_CONTACT_EMAILS) as ArrayList<ContactEmail>
                contactGroupEditCreateViewModel.calculateDiffMembers(selected)
                contactGroupEditCreateViewModel.setChanged()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun initAdapter() {
        contactGroupEmailsAdapter = ContactGroupEmailsAdapter(
            this, ArrayList(), null,
            {
                showToast("delete!")
            },
            mode = GroupsItemAdapterMode.NORMAL
        )
        with(contactEmailsRecyclerView) {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ContactGroupEditCreateActivity)
            adapter = contactGroupEmailsAdapter
        }
    }

    private fun setupNewContactGroupLayout() {
        setupToolbar(R.string.create_contact_group)
    }

    private fun setupEditContactGroupLayout() {
        setupToolbar(R.string.edit_contact_group)
    }

    private fun showColorsDialog() {
        val colorChooserFragment = ColorChooserFragment.newInstance()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(colorChooserFragment, colorChooserFragment.fragmentKey)
        transaction.commitAllowingStateLoss()
    }

    private fun setupToolbar(@StringRes title: Int) {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(title)
        }
    }

    private fun startObserving() {
        contactGroupEditCreateViewModel.contactGroupEmailsResult.observe(
            this,
            Observer {
                // prevent flickering between previous and future contacts list while waiting for response
                if (progress.visibility != View.VISIBLE) {
                    refreshData(HashSet(it))
                }
            }
        )
        contactGroupEditCreateViewModel.contactGroupEmailsError.observe(
            this,
            Observer {
                membersList.visibility = View.GONE
            }
        )
        contactGroupEditCreateViewModel.cleanUpComplete.observe(
            this,
            Observer {
                it?.getContentIfNotHandled()?.let {
                    if (!it) {
                        buildAndShowUnsavedEditDialog()
                    } else {
                        saveLastInteraction()
                        finish()
                    }
                }
            }
        )
        contactGroupEditCreateViewModel.getContactGroupEmails()
    }

    private fun refreshData(it: HashSet<ContactEmail>?) {
        val list = it?.let {
            ArrayList(it)
        }
        contactGroupEmailsAdapter.setData(list ?: ArrayList())
        with(membersList) {
            if (it == null || it.isEmpty()) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = resources.getQuantityString(R.plurals.contact_group_members, it.size, it.size)
            }
        }
    }

    private fun setName() {
        contactGroupName.text = contactGroupEditCreateViewModel.getGroupName()
        contactGroupName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                contactGroupEditCreateViewModel.setChanged()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { // noop
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { // noop
            }
        })
    }

    private fun setColor() {
        val colorInt: Int? = contactGroupEditCreateViewModel.getGroupColor()
        val color = if (colorInt != null && colorInt != 0) {
            colorInt
        } else {
            val randomColor = generateNewColor()
            contactGroupEditCreateViewModel.setGroupColor(randomColor)
            randomColor
        }
        thumbnail_contact_group_details.bind(
            isSelectedActive = false,
            isMultiselectActive = false,
            circleColor = color
        )
    }

    private fun buildAndShowUnsavedEditDialog() {
        UnsavedChangesDialog(
            this, { },
            {
                saveLastInteraction()
                finish()
            }
        ).build()
    }

    @ColorInt
    private fun generateNewColor(): Int {
        val colorOptions = resources.getIntArray(R.array.label_colors)
        val random = Random()
        val currentSelection = random.nextInt(colorOptions.size)
        return colorOptions[currentSelection]
    }
}

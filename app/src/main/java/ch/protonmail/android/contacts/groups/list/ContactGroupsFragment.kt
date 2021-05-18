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
package ch.protonmail.android.contacts.groups.list

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.fragments.BaseFragment
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.contacts.IContactsFragment
import ch.protonmail.android.contacts.IContactsListFragmentListener
import ch.protonmail.android.contacts.groups.details.ContactGroupDetailsActivity
import ch.protonmail.android.contacts.groups.details.EXTRA_CONTACT_GROUP
import ch.protonmail.android.contacts.list.search.ISearchListenerViewModel
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_contacts_groups.*
import timber.log.Timber
import java.io.Serializable

// region constants
private const val TAG_CONTACT_GROUPS_FRAGMENT = "ProtonMail.ContactGroupsFragment"
// endregion

@AndroidEntryPoint  // TODO: Investigate if it could be merged together with [ContactsListFragment].
class ContactGroupsFragment : BaseFragment(), IContactsFragment {

    private lateinit var contactGroupsAdapter: ContactsGroupsListAdapter
    private val contactGroupsViewModel: ContactGroupsViewModel by viewModels()
    override var actionMode: ActionMode? = null
        private set

    private val listener: IContactsListFragmentListener by lazy {
        requireActivity() as IContactsListFragmentListener
    }

    override fun onItemCheckedStateChanged(
        mode: ActionMode?,
        position: Int,
        id: Long,
        checked: Boolean
    ) = Unit

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_delete -> {
                DialogUtils.showDeleteConfirmationDialog(
                    requireContext(), getString(R.string.delete),
                    requireContext().resources.getQuantityString(
                        R.plurals.are_you_sure_delete_group,
                        getSelectedItems().toList().size,
                        getSelectedItems().toList().size
                    )
                ) {
                    onDelete()
                    mode?.finish()
                }
            }
        }
        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        actionMode = mode
        UiUtil.setStatusBarColor(
            activity as Activity,
            UiUtil.scaleColor(ContextCompat.getColor(requireContext(), R.color.dark_purple_statusbar), 1f, true)
        )
        mode!!.menuInflater.inflate(R.menu.contacts_menu, menu)
        menu!!.findItem(R.id.action_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.findItem(R.id.action_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.findItem(R.id.action_sync).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.findItem(R.id.action_convert).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu): Boolean {
        menu.findItem(R.id.action_delete).isVisible = true
        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_sync).isVisible = false
        menu.findItem(R.id.action_convert).isVisible = false
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode?.finish()
        actionMode = null

        listener.setTitle(getString(R.string.contacts))
    }

    override fun onContactPermissionChange(hasPermission: Boolean) {}

    override fun getLayoutResourceId() = R.layout.fragment_contacts_groups

    override fun getFragmentKey() = TAG_CONTACT_GROUPS_FRAGMENT

    override fun getSearchListener(): ISearchListenerViewModel = contactGroupsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
    }

    override fun onStart() {
        super.onStart()
        startObserving()
    }

    private fun initAdapter() {
        contactGroupsAdapter = ContactsGroupsListAdapter(
            this::onContactGroupClick,
            this::onWriteToContactGroup,
            this::onContactGroupSelect
        )

        contactGroupsRecyclerView.layoutManager = LinearLayoutManager(context)
        contactGroupsRecyclerView.adapter = contactGroupsAdapter
    }

    override fun updateRecyclerViewBottomPadding(@Px size: Int) {
        contactGroupsRecyclerView.updatePadding(bottom = size)
    }

    private fun startObserving() {
        contactGroupsViewModel.contactGroupsResult.observe(this) { list ->
            Timber.d("contactGroupsResult size: ${list.size}")
            if (list.isEmpty()) {
                noResults.visibility = VISIBLE
            } else {
                noResults.visibility = GONE
            }
            listener.dataUpdated(1, list?.size ?: 0)
            contactGroupsAdapter.submitList(list)
        }

        contactGroupsViewModel.contactGroupsError.observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                Timber.i("contactGroupsResult Error: $it")
                context?.showToast(it.ifEmpty { getString(R.string.default_error_message) })
            }
        }
        contactGroupsViewModel.observeContactGroups()
    }

    private fun onContactGroupSelect(labelItem: ContactGroupListItem) {
        Timber.v("onContactGroupSelect ${labelItem.contactId}")

        val updatedItems = contactGroupsAdapter.currentList.map { item ->
            if (item.contactId == labelItem.contactId) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
        contactGroupsAdapter.submitList(updatedItems)

        if (actionMode == null) {
            actionMode = listener.doStartActionMode(this@ContactGroupsFragment)
        } else {
            actionMode?.invalidate()
        }

        val checkedItemsCount = updatedItems.filter { it.isSelected }.size
        if (checkedItemsCount == 0) {
            listener.setTitle(getString(R.string.contacts))
            actionMode?.finish()
        } else {
            actionMode?.title = String.format(getString(R.string.contact_group_selected), checkedItemsCount)
        }
    }

    private fun onContactGroupClick(labelItem: ContactGroupListItem) {
        val detailsIntent = Intent(context, ContactGroupDetailsActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable(EXTRA_CONTACT_GROUP, labelItem)
        detailsIntent.putExtra(EXTRA_CONTACT_GROUP, bundle)
        startActivity(AppUtil.decorInAppIntent(detailsIntent))
    }

    override fun onDelete() {
        contactGroupsViewModel.deleteSelected(getSelectedItems().toList())
    }

    private fun onWriteToContactGroup(contactGroup: ContactGroupListItem) {
        val composeIntent = Intent(context, ComposeMessageActivity::class.java)
        if (!contactGroupsViewModel.isPaidUser()) {
            context?.showToast(R.string.paid_plan_needed)
            return
        }
        contactGroupsViewModel.contactGroupEmailsResult.observe(this) { event ->
            event?.getContentIfNotHandled()?.let { list ->
                Timber.v("Contact email list received $list")
                composeIntent.putExtra(
                    ComposeMessageActivity.EXTRA_TO_RECIPIENT_GROUPS,
                    list.asSequence().map { email ->
                        MessageRecipient(email.name, email.email, contactGroup.name)
                    }.toList() as Serializable
                )
                startActivity(AppUtil.decorInAppIntent(composeIntent))
                contactGroupsViewModel.contactGroupEmailsResult.removeObservers(this)
            }
        }

        contactGroupsViewModel.contactGroupEmailsError.observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                context?.showToast(it.ifBlank { getString(R.string.default_error_message) })
            }
        }

        contactGroupsViewModel.getContactGroupEmails(contactGroup)
    }

    override fun onStop() {
        super.onStop()
        if (actionMode != null) {
            actionMode!!.finish()
            actionMode = null
        }
    }

    private fun getSelectedItems() = contactGroupsAdapter.currentList.filter { it.isSelected }

    companion object {
        fun newInstance() = ContactGroupsFragment()
    }
}

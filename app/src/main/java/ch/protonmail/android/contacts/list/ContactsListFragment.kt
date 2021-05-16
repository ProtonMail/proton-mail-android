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
package ch.protonmail.android.contacts.list

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.annotation.Px
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Operation
import androidx.work.WorkManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.fragments.BaseFragment
import ch.protonmail.android.contacts.IContactsFragment
import ch.protonmail.android.contacts.IContactsListFragmentListener
import ch.protonmail.android.contacts.REQUEST_CODE_CONTACT_DETAILS
import ch.protonmail.android.contacts.REQUEST_CODE_CONVERT_CONTACT
import ch.protonmail.android.contacts.details.ContactDetailsActivity
import ch.protonmail.android.contacts.details.edit.EditContactDetailsActivity
import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.contacts.list.listView.ContactsListAdapter
import ch.protonmail.android.contacts.list.progress.ProgressDialogFactory
import ch.protonmail.android.contacts.list.progress.UploadProgressObserver
import ch.protonmail.android.contacts.list.search.ISearchListenerViewModel
import ch.protonmail.android.contacts.list.viewModel.ContactsListViewModel
import ch.protonmail.android.events.ContactEvent
import ch.protonmail.android.events.ContactProgressEvent
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum
import com.birbit.android.jobqueue.JobManager
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_contacts.*
import timber.log.Timber
import javax.inject.Inject

// region constants
private const val TAG_CONTACTS_LIST_FRAGMENT = "ProtonMail.ContactsFragment"
private const val EXTRA_PERMISSION = "extra_permission"
// endregion

@AndroidEntryPoint
class ContactsListFragment : BaseFragment(), IContactsFragment {

    @Inject
    lateinit var viewModelFactory: ContactsListViewModel.Factory
    private val viewModel: ContactsListViewModel by viewModels {
        viewModelFactory.apply { loaderManager = LoaderManager.getInstance(this@ContactsListFragment) }
    }
    private lateinit var contactsAdapter: ContactsListAdapter
    private var hasContactsPermission: Boolean = false

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var jobManager: JobManager

    override var actionMode: ActionMode? = null
        private set

    private val listener: IContactsListFragmentListener by lazy {
        activity as? IContactsListFragmentListener
            ?: throw IllegalStateException("Activity must implement IContactsListFragmentListener")
    }

    private val Int.statusTextId: Int
        get() = when (this) {
            ContactEvent.SUCCESS -> R.string.contact_saved
            ContactEvent.ALREADY_EXIST -> R.string.contact_exist
            ContactEvent.INVALID_EMAIL -> R.string.invalid_email_some_contacts
            ContactEvent.DUPLICATE_EMAIL -> R.string.duplicate_email
            ContactEvent.SAVED -> R.string.contact_saved
            else -> R.string.contact_saved_offline
        }

    private fun getSelectedContactsIds(): List<String> {
        val selectedContactIds = ArrayList<String>()
        contactsAdapter.getSelectedItems!!.forEach {
            it.contactId?.let { contactId ->
                if (contactId.isNotEmpty()) {
                    selectedContactIds.add(contactId)
                }
            }
        }
        return selectedContactIds
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        menu.findItem(R.id.transform_phone_contacts).isVisible =
            contactsAdapter.getSelectedItems!!.none(
                ContactItem::isProtonMailContact
            )
        return true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        actionMode = mode
        mode.menuInflater.inflate(R.menu.contacts_selection_menu, menu)
        return true
    }

    override fun onItemCheckedStateChanged(
        mode: ActionMode,
        position: Int,
        id: Long,
        checked: Boolean
    ) {
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode!!.finish()
        actionMode = null
        contactsAdapter.endSelectionMode()
        UiUtil.setStatusBarColor(
            requireActivity() as AppCompatActivity,
            ContextCompat.getColor(
                requireContext(),
                R.color.dark_purple_statusbar
            )
        )

        listener.setTitle(getString(R.string.contacts))
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        val menuItemId = menuItem.itemId
        val selectedContacts = contactsAdapter.getSelectedItems!!
        val allContactsLocal = selectedContacts.none(ContactItem::isProtonMailContact)
        val allContactsProtonMail = selectedContacts.all(ContactItem::isProtonMailContact)

        when (menuItemId) {
            R.id.delete_contacts ->
                if (!allContactsProtonMail) {
                    requireContext().showToast(R.string.please_select_only_phone_contacts)
                } else {
                    DialogUtils.showDeleteConfirmationDialog(
                        requireContext(),
                        getString(R.string.delete),
                        requireContext().resources.getQuantityString(
                            R.plurals.are_you_sure_delete_contact,
                            contactsAdapter.getSelectedItems!!.toList().size,
                            contactsAdapter.getSelectedItems!!.toList().size
                        )
                    ) {
                        onDelete()
                        mode.finish()
                    }
                }
            R.id.transform_phone_contacts ->
                if (!allContactsLocal) {
                    requireContext().showToast(R.string.please_select_only_phone_contacts)
                } else {
                    LocalContactsConverter(jobManager, viewModel)
                        .startConversion(
                            contactsAdapter.getSelectedItems!!.toList()
                        )
                    mode.finish()
                }
        }

        return true
    }

    override fun getSearchListener(): ISearchListenerViewModel = viewModel

    override fun onStart() {
        super.onStart()
        listener.registerObject(this)
        viewModel.fetchContactItems()
    }

    override fun onStop() {
        listener.unregisterObject(this)
        super.onStop()
        if (actionMode != null) {
            actionMode!!.finish()
            actionMode = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hasContactsPermission = arguments?.getBoolean(EXTRA_PERMISSION) ?: false

        initAdapter()
        listener.selectPage(0)
        listener.doRequestContactsPermission()
        startObserving()
    }

    private fun startObserving() {
        viewModel.contactItems.observe(viewLifecycleOwner) { contactItems ->
            Timber.v("New Contact items size: ${contactItems.size}")
            if (contactItems.isEmpty()) {
                noResults.visibility = VISIBLE
            } else {
                noResults.visibility = GONE
            }
            contactsAdapter.apply {
                submitList(contactItems)
                val count = contactItems.size - contactItems
                    .count { contactItem -> contactItem.contactId == "-1" }
                listener.dataUpdated(0, count)
            }
        }

        val progressDialogFactory = ProgressDialogFactory(requireContext())
        viewModel.uploadProgress.observe(viewLifecycleOwner) {
            UploadProgressObserver(progressDialogFactory::create)
        }

        viewModel.contactToConvert.observe(viewLifecycleOwner) { event ->
            Timber.v("ContactToConvert event: $event")
            val localContact = event?.getContentIfNotHandled() ?: return@observe
            val intent = EditContactDetailsActivity.startConvertContactActivity(
                requireContext(),
                localContact
            )
            listener.doStartActivityForResult(intent, REQUEST_CODE_CONVERT_CONTACT)
        }
    }

    override fun getLayoutResourceId() = R.layout.fragment_contacts

    override fun getFragmentKey() = TAG_CONTACTS_LIST_FRAGMENT

    fun optionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_convert -> {
                val localContactsConverter = LocalContactsConverter(jobManager, viewModel)

                if (viewModel.hasPermission) {
                    val contacts = viewModel.androidContacts.value
                    contacts?.let {
                        context?.showConvertsContactsDialog(localContactsConverter, it)
                    }
                } else {
                    listener.doRequestContactsPermission()
                }

                true
            }
            R.id.action_delete -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun Context.showConvertsContactsDialog(
        localContactsConverter: LocalContactsConverter,
        contacts: List<ContactItem>
    ) {
        val clickListener = DialogInterface.OnClickListener { _, _ ->
            localContactsConverter.startConversion(contacts)
        }
        AlertDialog.Builder(this).setTitle(R.string.convert_question)
            .setMessage(R.string.convert_question_subtitle)
            .setPositiveButton(R.string.yes, clickListener)
            .setNegativeButton(R.string.no, null)
            .create()
            .show()
    }

    private fun Activity.startContactDetails(contactId: String) {
        val intent = AppUtil.decorInAppIntent(
            Intent(
                this,
                ContactDetailsActivity::class.java
            )
        )
        intent.putExtra(ContactDetailsActivity.EXTRA_CONTACT, contactId)
        startActivityForResult(intent, REQUEST_CODE_CONTACT_DETAILS)
    }

    override fun onContactPermissionChange(hasPermission: Boolean) {
        this.hasContactsPermission = hasPermission
        viewModel.setHasContactsPermission(hasPermission)
    }

    override fun onDelete() {
        viewModel.deleteSelected(getSelectedContactsIds()).observe(
            this,
            { state ->
                if (state is Operation.State.FAILURE) {
                    context?.showToast(getString(R.string.default_error_message))
                } else {
                    Timber.v("Delete contacts state $state")
                }
            }
        )
    }

    @Subscribe
    @Suppress("unused")
    fun onContactProgress(event: ContactProgressEvent) = viewModel.setProgress(event.completed)

    @Subscribe
    @Suppress("unused")
    fun onContactEvent(event: ContactEvent) {
        if (event.contactCreation) {
            context?.showToast(event.status.statusTextId)
        } else if (event.status == ContactEvent.SUCCESS) {
            viewModel.setProgress(null)
            viewModel.setProgressMax(null)
        } else {
            val statuses = event.statuses
            if (statuses != null) {
                statuses.forEach {
                    context?.showToast(it.statusTextId)
                }
            } else {
                context?.showToast(event.status.statusTextId)
            }
        }

    }

    private fun initAdapter() {
        var actionMode: ActionMode? = null
        contactsAdapter = ContactsListAdapter(
            this::onContactClick,
            this::onContactSelect
        ) { selectionModeEvent ->
            when (selectionModeEvent) {
                SelectionModeEnum.STARTED -> {
                    actionMode = listener.doStartActionMode(this@ContactsListFragment)
                }
                SelectionModeEnum.ENDED -> {
                    if (actionMode != null) {
                        actionMode!!.finish()
                        actionMode = null
                    }
                }
            }
        }

        contactsRecyclerView.layoutManager = LinearLayoutManager(context)
        contactsRecyclerView.adapter = contactsAdapter
    }

    override fun updateRecyclerViewBottomPadding(@Px size: Int) {
        contactsRecyclerView.updatePadding(bottom = size)
    }

    private fun onContactClick(contactItem: ContactItem) {
        if (contactItem.isProtonMailContact) {
            activity?.startContactDetails(contactItem.contactId!!)
        } else {
            viewModel.startConvertDetails(contactItem.contactId!!)
        }
    }

    private fun onContactSelect() {
        val checkedItemsCount = contactsAdapter.getSelectedItems?.size
        if (checkedItemsCount == null) {
            listener.setTitle(getString(R.string.contacts))
        } else {
            actionMode?.title =
                String.format(getString(R.string.contact_group_selected), checkedItemsCount)
        }
    }

    companion object {

        fun newInstance(hasPermission: Boolean): ContactsListFragment {
            val fragment = ContactsListFragment()
            val extras = Bundle()
            extras.putBoolean(EXTRA_PERMISSION, hasPermission)
            fragment.arguments = extras
            return fragment
        }
    }
}

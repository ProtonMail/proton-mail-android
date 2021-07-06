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
package ch.protonmail.android.compose.recipients

import android.content.Context
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import ch.protonmail.android.R
import ch.protonmail.android.activities.dialogs.AbstractDialogFragment
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.ErrorResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.extensions.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.dialog_fragment_group_recipients.*
import javax.inject.Inject

// region constants
private const val ARGUMENT_RECIPIENTS = "extra_contact_group_recipients"
private const val ARGUMENT_LOCATION =
    "extra_recipient_view_location" // should be one of To/CC/BCC, please use one of Constants.RecipientLocation
// endregion

@AndroidEntryPoint
class GroupRecipientsDialogFragment : AbstractDialogFragment() {

    @Inject
    lateinit var groupRecipientsViewModelFactory: GroupRecipientsViewModelFactory
    private lateinit var groupRecipientsViewModel: GroupRecipientsViewModel
    private lateinit var groupRecipientsAdapter: GroupRecipientsAdapter
    private lateinit var groupRecipientsListener: IGroupRecipientsListener

    override fun onBackPressed() {
        CANCELED = true
        dismiss()
    }

    override fun getLayoutResourceId(): Int = R.layout.dialog_fragment_group_recipients

    override fun onAttach(context: Context) {
        super.onAttach(context)
        groupRecipientsListener = context as IGroupRecipientsListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupRecipientsViewModel = ViewModelProviders.of(this, groupRecipientsViewModelFactory)
            .get(GroupRecipientsViewModel::class.java)

        groupRecipientsViewModel.contactGroupResult.observe(
            this,
            {
            }
        )

        groupRecipientsViewModel.contactGroupError.observe(
            this,
            { event ->
                var error: ErrorResponse? = ErrorResponse("", ErrorEnum.DEFAULT_ERROR)
                if (event != null) {
                    error = event.getContentIfNotHandled()
                }
                if (error != null) {
                    context?.showToast(error.getMessage(context!!))
                }
            }
        )

        val args = arguments
        args?.let {
            groupRecipientsViewModel.setData(
                args.getSerializable(ARGUMENT_RECIPIENTS) as ArrayList<MessageRecipient>,
                args.getSerializable(ARGUMENT_LOCATION) as Constants.RecipientLocationType
            )
        }
    }

    override fun onResume() {
        super.onResume()

        val windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val params = dialog!!.window!!.attributes
        params.height = 2 * (displayMetrics.heightPixels / 3)
        dialog!!.window!!.setLayout(params.width, params.height)
    }

    override fun initUi(rootView: View?) {
        groupRecipientsAdapter =
            GroupRecipientsAdapter(context!!, groupRecipientsViewModel.getData(), this::onMembersSelectChanged)
        groupRecipientsViewModel.contactGroupResult.observe(
            this,
            Observer {
                val allMessageRecipients = ArrayList<MessageRecipient>()
                it?.let {
                    for (email in it) {
                        val recipient = MessageRecipient(email.name, email.email)
                        recipient.isSelected = email.selected
                        recipient.icon = email.pgpIcon
                        recipient.iconColor = email.pgpIconColor
                        recipient.description = email.pgpDescription
                        recipient.setIsPGP(email.isPGP)
                        recipient.group = groupRecipientsViewModel.getGroup()
                        recipient.groupIcon = groupRecipientsViewModel.getGroupIcon()
                        recipient.groupColor = groupRecipientsViewModel.getGroupColor()
                        allMessageRecipients.add(recipient)
                    }
                    groupRecipientsAdapter.setData(allMessageRecipients)
                    onMembersSelectChanged()
                }
            }
        )

        groupRecipientsViewModel.contactGroupError.observe(
            this,
            Observer { event ->
                var error: ErrorResponse? = ErrorResponse("", ErrorEnum.DEFAULT_ERROR)
                if (event != null) {
                    error = event.getContentIfNotHandled()
                }
                if (error != null) {
                    context?.showToast(error.getMessage(context!!))
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(recipientsRecyclerView) {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = groupRecipientsAdapter
        }
        title.text = groupRecipientsViewModel.getTitle()
        groupIcon.colorFilter = PorterDuffColorFilter(
            groupRecipientsViewModel.getGroupColor(),
            PorterDuff.Mode.SRC_IN
        )

        check.setOnClickListener {
            val allMessageRecipients = ArrayList<MessageRecipient>()
            if (check.isChecked) {
                for (email in groupRecipientsAdapter.getData()) {
                    email.isSelected = true
                    allMessageRecipients.add(email)
                }
            } else {
                for (email in groupRecipientsAdapter.getData()) {
                    email.isSelected = false
                    allMessageRecipients.add(email)
                }
            }
            groupRecipientsAdapter.setData(allMessageRecipients)
        }

        done.setOnClickListener {
            CANCELED = false
            dismiss()
        }
        cancel.setOnClickListener {
            CANCELED = true
            dismiss()
        }
    }

    override fun getStyleResource(): Int = R.style.AppTheme_Dialog_Labels

    override fun getFragmentKey(): String = "ProtonMail.GroupRecipientsFragment"

    interface IGroupRecipientsListener {

        fun recipientsSelected(
            recipients: ArrayList<MessageRecipient>,
            location: Constants.RecipientLocationType
        )

    }

    override fun onDismiss(dialog: DialogInterface) {
        val selected: ArrayList<MessageRecipient> = if (!CANCELED) {
            groupRecipientsAdapter.getSelected()
        } else {
            groupRecipientsViewModel.getData()
        }
        groupRecipientsListener.recipientsSelected(
            selected,
            groupRecipientsViewModel.getLocation()
        )
        dismissAllowingStateLoss()
    }

    private fun onMembersSelectChanged() {
        check.isChecked = groupRecipientsAdapter.itemCount == groupRecipientsAdapter.getSelected().size
    }

    companion object {

        private var CANCELED = false

        fun newInstance(
            recipients: ArrayList<MessageRecipient>,
            location: Constants.RecipientLocationType
        ): GroupRecipientsDialogFragment {
            val fragment = GroupRecipientsDialogFragment()
            val extras = Bundle()
            extras.putSerializable(ARGUMENT_RECIPIENTS, recipients)
            extras.putSerializable(ARGUMENT_LOCATION, location)
            fragment.arguments = extras
            return fragment
        }
    }
}

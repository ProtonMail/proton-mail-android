/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.compose.recipients

import android.content.Context
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.ErrorResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.databinding.DialogFragmentGroupRecipientsBinding
import ch.protonmail.android.utils.extensions.showToast
import dagger.hilt.android.AndroidEntryPoint
import me.proton.core.presentation.ui.view.ProtonButton
import me.proton.core.presentation.ui.view.ProtonCheckbox

// region constants
private const val ARGUMENT_RECIPIENTS = "extra_contact_group_recipients"
private const val ARGUMENT_LOCATION =
    "extra_recipient_view_location" // should be one of To/CC/BCC, please use one of Constants.RecipientLocation
// endregion

@AndroidEntryPoint
class GroupRecipientsDialogFragment : DialogFragment() {

    private val groupRecipientsViewModel: GroupRecipientsViewModel by viewModels()
    private lateinit var groupRecipientsAdapter: GroupRecipientsAdapter
    private lateinit var groupRecipientsListener: IGroupRecipientsListener

    private lateinit var groupIconImageView: ImageView
    private lateinit var groupNameTextView: TextView
    private lateinit var selectAllCheckbox: ProtonCheckbox
    private lateinit var recipientsRecyclerView: RecyclerView
    private lateinit var applyButton: ProtonButton
    private lateinit var cancelButton: ProtonButton

    override fun onAttach(context: Context) {
        super.onAttach(context)
        groupRecipientsListener = context as IGroupRecipientsListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        groupRecipientsViewModel.contactGroupError.observe(this) { event ->
            var error: ErrorResponse? = ErrorResponse("", ErrorEnum.DEFAULT_ERROR)
            if (event != null) {
                error = event.getContentIfNotHandled()
            }
            if (error != null) {
                context?.showToast(error.getMessage(context!!))
            }
        }

        val args = arguments
        args?.let {
            groupRecipientsViewModel.setData(
                args.getSerializable(ARGUMENT_RECIPIENTS) as ArrayList<MessageRecipient>,
                args.getSerializable(ARGUMENT_LOCATION) as Constants.RecipientLocationType
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogFragmentGroupRecipientsBinding.inflate(inflater)

        with(binding) {
            groupIconImageView = dialogGroupRecipientsGroupIconImageView
            groupNameTextView = dialogGroupRecipientsGroupNameTextView
            selectAllCheckbox = dialogGroupRecipientsSelectAllCheckbox
            recipientsRecyclerView = dialogGroupRecipientsRecyclerView
            applyButton = dialogGroupRecipientsApplyButton
            cancelButton = dialogGroupRecipientsCancelButton
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        with(recipientsRecyclerView) {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = groupRecipientsAdapter
        }
        groupNameTextView.text = groupRecipientsViewModel.getTitle()
        groupIconImageView.colorFilter = PorterDuffColorFilter(
            groupRecipientsViewModel.getGroupColor(),
            PorterDuff.Mode.SRC_IN
        )

        selectAllCheckbox.setOnClickListener {
            val allMessageRecipients = ArrayList<MessageRecipient>()
            if (selectAllCheckbox.isChecked) {
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

        applyButton.setOnClickListener {
            CANCELED = false
            dismiss()
        }
        cancelButton.setOnClickListener {
            CANCELED = true
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()

        val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val params = dialog!!.window!!.attributes
        params.height = 2 * (displayMetrics.heightPixels / 3)
        dialog!!.window!!.setLayout(params.width, params.height)
    }

    private fun initUi() {
        groupRecipientsAdapter =
            GroupRecipientsAdapter(groupRecipientsViewModel.getData(), this::onMembersSelectChanged)
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

        groupRecipientsViewModel.contactGroupError.observe(this) { event ->
            var error: ErrorResponse? = ErrorResponse("", ErrorEnum.DEFAULT_ERROR)
            if (event != null) {
                error = event.getContentIfNotHandled()
            }
            if (error != null) {
                context?.showToast(error.getMessage(context!!))
            }
        }
    }

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
        selectAllCheckbox.isChecked = groupRecipientsAdapter.itemCount == groupRecipientsAdapter.getSelected().size
    }

    companion object {

        private var CANCELED = false
        const val KEY = "ProtonMail.GroupRecipientsFragment"

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

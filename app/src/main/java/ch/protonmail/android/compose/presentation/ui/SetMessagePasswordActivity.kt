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

package ch.protonmail.android.compose.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.compose.presentation.model.MessagePasswordUiModel
import ch.protonmail.android.compose.presentation.model.SetMessagePasswordUiModel
import ch.protonmail.android.compose.presentation.viewmodel.SetMessagePasswordViewModel
import ch.protonmail.android.databinding.ActivitySetMessagePasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.presentation.ui.view.ProtonInput
import me.proton.core.presentation.utils.onClick
import me.proton.core.presentation.utils.onTextChange
import me.proton.core.util.kotlin.forEach
import timber.log.Timber

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val ARG_SET_MESSAGE_PASSWORD_PASSWORD = "arg.password"

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val ARG_SET_MESSAGE_PASSWORD_HINT = "arg.hint"

/**
 * Activity for set a password for a given Message
 * @see ComposeMessageKotlinActivity
 */
@AndroidEntryPoint
class SetMessagePasswordActivity : AppCompatActivity() {

    private val viewModel: SetMessagePasswordViewModel by viewModels()
    private val binding by lazy {
        ActivitySetMessagePasswordBinding.inflate(layoutInflater)
    }

    private lateinit var messagePassword: MessagePasswordUiModel
    private var initiallyHasPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.setMsgPasswordToolbar)

        val password = intent.getStringExtra(ARG_SET_MESSAGE_PASSWORD_PASSWORD)
        val hint = intent.getStringExtra(ARG_SET_MESSAGE_PASSWORD_HINT)
        initiallyHasPassword = password.isNullOrBlank().not()
        viewModel.validate(password, password, hint)

        with(binding) {
            setMsgPasswordInfoTextView.movementMethod = LinkMovementMethod.getInstance()
            forEach(
                setMsgPasswordMsgPasswordInput,
                setMsgPasswordRepeatPasswordInput,
                setMsgPasswordHintInput
            ) { it.onTextChange { validateInput() } }
            setMsgPasswordApplyButton.onClick { setResultAndFinish() }
            setMsgPasswordRemoveButton.onClick {
                messagePassword = MessagePasswordUiModel.Unset
                setResultAndFinish()
            }
        }
        viewModel.result
            .flowWithLifecycle(lifecycle)
            .onEach(::updateUi)
            .launchIn(lifecycleScope)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> cancelResultAndFinish()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun cancelResultAndFinish(): Boolean {
        finish()
        return true
    }

    private fun updateUi(model: SetMessagePasswordUiModel) {
        messagePassword = model.messagePassword
        setPasswordInput(model.passwordInput)
        setRepeatInput(model.repeatInput)
        setHintInput((messagePassword as? MessagePasswordUiModel.Set)?.hint)
        setApplyButton(model.hasErrors)
        setDeleteButton(initiallyHasPassword)
    }

    private fun setPasswordInput(input: SetMessagePasswordUiModel.Input) {
        binding.setMsgPasswordMsgPasswordInput.apply {
            setTextIfChanged(input.text)
            getErrorString(input.error)
                ?.let(::setInputError)
                ?: clearInputError()
        }
    }

    private fun setRepeatInput(input: SetMessagePasswordUiModel.Input) {
        binding.setMsgPasswordRepeatPasswordInput.apply {
            setTextIfChanged(input.text)
            getErrorString(input.error)
                ?.let(::setInputError)
                ?: clearInputError()
        }
    }

    private fun setHintInput(hint: String?) {
        binding.setMsgPasswordHintInput.setTextIfChanged(hint)
    }

    private fun setApplyButton(hasErrors: Boolean) {
        binding.setMsgPasswordApplyButton.isEnabled = hasErrors.not()
    }

    private fun setDeleteButton(enabled: Boolean) {
        binding.setMsgPasswordRemoveButton.apply {
            isEnabled = enabled
            if (!enabled) {
                setTextColor(getColor(R.color.text_disabled))
                setStrokeColorResource(R.color.interaction_weak)
            }
        }
    }

    private fun setResultAndFinish() {
        Timber.v("Set password $messagePassword")
        val messagePassword = messagePassword
        val resultIntent = Intent().apply {
            if (messagePassword is MessagePasswordUiModel.Set) {
                putExtra(ARG_SET_MESSAGE_PASSWORD_PASSWORD, messagePassword.password)
                putExtra(ARG_SET_MESSAGE_PASSWORD_HINT, messagePassword.hint)
            }
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun getErrorString(error: SetMessagePasswordUiModel.Error?): String? {
        return when (error) {
            SetMessagePasswordUiModel.Error.TooShort -> getString(R.string.set_msg_password_error_too_short)
            SetMessagePasswordUiModel.Error.TooLong -> getString(R.string.set_msg_password_error_too_long)
            SetMessagePasswordUiModel.Error.DoesNotMatch -> getString(R.string.set_msg_password_error_mismatch)
            SetMessagePasswordUiModel.Error.Empty, null -> null
        }
    }

    private fun validateInput() {
        with(binding) {
            val password = setMsgPasswordMsgPasswordInput.text
            val repeat = setMsgPasswordRepeatPasswordInput.text
            val hint = setMsgPasswordHintInput.text
            viewModel.validate(password, repeat, hint)
        }
    }

    private fun ProtonInput.setTextIfChanged(charSequence: CharSequence?) {
        if (text.toString() != charSequence.toString()) {
            text = charSequence
        }
    }

    class ResultContract : ActivityResultContract<MessagePasswordUiModel, MessagePasswordUiModel>() {

        private lateinit var input: MessagePasswordUiModel

        override fun createIntent(context: Context, input: MessagePasswordUiModel): Intent {
            this.input = input
            return Intent(context, SetMessagePasswordActivity::class.java).apply {
                if (input is MessagePasswordUiModel.Set) {
                    putExtra(ARG_SET_MESSAGE_PASSWORD_PASSWORD, input.password)
                    putExtra(ARG_SET_MESSAGE_PASSWORD_HINT, input.hint)
                }
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): MessagePasswordUiModel {
            return intent?.let {
                val password = intent.getStringExtra(ARG_SET_MESSAGE_PASSWORD_PASSWORD)
                val hint = intent.getStringExtra(ARG_SET_MESSAGE_PASSWORD_HINT)
                if (password != null) {
                    MessagePasswordUiModel.Set(password, hint)
                } else {
                    MessagePasswordUiModel.Unset
                }
            } ?: input
        }
    }
}

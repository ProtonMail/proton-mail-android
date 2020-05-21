package ch.protonmail.android.viewmodel

import androidx.lifecycle.ViewModel
import ch.protonmail.android.domain.DispatcherProvider
import studio.forface.viewstatestore.ViewStateStoreScope

/**
 * Base [ViewModel] for the App
 * EVERY [ViewModel] must inherit from this
 *
 * Implements [ViewStateStoreScope] for publish to `LockedViewStateStore`
 * Implements [DispatcherProvider] for provide `CoroutineDispatcher`s
 *
 * @author Davide Farella
 */
abstract class BaseViewModel(
    dispatcherProvider: DispatcherProvider
) : ViewModel(), ViewStateStoreScope, DispatcherProvider by dispatcherProvider

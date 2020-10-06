package ch.protonmail.android.utils.extensions

import androidx.work.WorkInfo

/**
 * Converts a list of [WorkInfo] data to just one first final state. I.e. the success or failure.
 * It is intended to be used with OneTimeWorkRequests, it only takes first meaningful value of the work info stream.
 */
fun List<WorkInfo>.reduceWorkInfoToBoolean(): Boolean =
    this.map { it.state }
        .filter { it.isFinished }
        .map { it == WorkInfo.State.SUCCEEDED }
        .first()


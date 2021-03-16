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
package ch.protonmail.android.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.protonmail.android.core.ProtonMailApplication

private const val TAG = "NetworkApiSwitchWorker"

class NetworkApiSwitchWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    @SuppressLint("LogNotTimber")
    override fun doWork(): Result {
        Log.d(TAG, "doWork started")
        if(isStopped) {
            Log.d(TAG, "worker was stopped")
            return Result.failure()
        }
        ProtonMailApplication.getApplication().changeApiProviders() // try to switch to old, don't force
        return Result.success()
    }

    override fun onStopped() {
        val workerID = this.id.toString()
        Log.d(TAG, "task with ID: $workerID stopped")
        super.onStopped()
    }
}

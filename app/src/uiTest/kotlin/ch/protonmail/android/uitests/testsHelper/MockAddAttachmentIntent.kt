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
package ch.protonmail.android.uitests.testsHelper

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import androidx.annotation.IdRes
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.intent.IntentCallback
import androidx.test.runner.intent.IntentMonitorRegistry
import org.jetbrains.annotations.Contract
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

/**
 * Created by Nikola Nolchevski on 13-May-20.
 */
object MockAddAttachmentIntent {
    private fun createImage(@IdRes resourceId: Int) {
        val icon = BitmapFactory.decodeResource(
            InstrumentationRegistry.getInstrumentation().targetContext.resources,
            resourceId)
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir, "pickImageResult.jpeg")
        try {
            val fos = FileOutputStream(file)
            icon.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Contract(" -> new")
    private fun pickImageResult(): ActivityResult {
        val bundle = Bundle()
        val parcels = ArrayList<Parcelable>()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir, "pickImageResult.jpeg")
        val resultData = Intent(Intent.ACTION_PICK, Uri.fromFile(file))
        val parcelable: Parcelable = Uri.fromFile(file)
        parcels.add(parcelable)
        bundle.putParcelableArrayList(Intent.EXTRA_STREAM, parcels)
        resultData.putExtras(bundle)
        return ActivityResult(Activity.RESULT_OK, resultData)
    }

    fun mockChooseAttachment(@IdRes objectId: Int, @IdRes imageResourceId: Int) {
        createImage(imageResourceId)
        val result = pickImageResult()
        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_CHOOSER)).respondWith(result)
        UIActions.id.clickViewWithId(objectId)
    }

    fun mockCameraImageCapture(@IdRes objectId: Int, @IdRes imageResourceId: Int) {
        val result = ActivityResult(Activity.RESULT_OK, null)
        Intents.intending(IntentMatchers.hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(result)
        val cameraCallback = intentCallback(imageResourceId)
        IntentMonitorRegistry.getInstance().addIntentCallback(cameraCallback)
        UIActions.id.clickViewWithId(objectId)
        IntentMonitorRegistry.getInstance().removeIntentCallback(cameraCallback)
    }

    @Contract(pure = true)
    private fun intentCallback(@IdRes resourceId: Int): IntentCallback {
        return IntentCallback { intent: Intent ->
            if (intent.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                try {
                    val imageUri = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
                    val image = BitmapFactory.decodeResource(
                        InstrumentationRegistry.getInstrumentation().targetContext.resources,
                        resourceId)
                    val out = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.openOutputStream(imageUri)
                    image.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    assert(out != null)
                    out!!.flush()
                    out.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}

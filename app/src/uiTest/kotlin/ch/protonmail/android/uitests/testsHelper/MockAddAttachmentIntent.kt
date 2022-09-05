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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.intent.IntentCallback
import androidx.test.runner.intent.IntentMonitorRegistry
import me.proton.fusion.Fusion
import org.jetbrains.annotations.Contract
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

object MockAddAttachmentIntent : Fusion {
    private fun createImage(assetsFilename: String) {
        val icon = BitmapFactory
            .decodeStream(InstrumentationRegistry.getInstrumentation().context.assets.open(assetsFilename))
        val file = File(InstrumentationRegistry.getInstrumentation()
            .targetContext.externalCacheDir, "pickImageResult.jpeg")
        try {
            val fos = FileOutputStream(file)
            icon.compress(Bitmap.CompressFormat.JPEG, 50, fos)
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
        val file = File(InstrumentationRegistry.getInstrumentation()
            .targetContext.externalCacheDir, "pickImageResult.jpeg")
        val resultData = Intent(Intent.ACTION_PICK, Uri.fromFile(file))
        val parcelable: Parcelable = Uri.fromFile(file)
        parcels.add(parcelable)
        bundle.putParcelableArrayList(Intent.EXTRA_STREAM, parcels)
        resultData.putExtras(bundle)
        return ActivityResult(Activity.RESULT_OK, resultData)
    }

    fun mockChooseAttachment(@IdRes objectId: Int, assetsFilename: String) {
        createImage(assetsFilename)
        val result = pickImageResult()
        intent.hasAction(Intent.ACTION_CHOOSER).respondWith(result)
        view.withId(objectId).click()
    }

    fun mockCameraImageCapture(@IdRes objectId: Int, @IdRes imageResourceId: Int) {
        val result = ActivityResult(Activity.RESULT_OK, null)
        intent.hasAction(MediaStore.ACTION_IMAGE_CAPTURE).respondWith(result)
        val cameraCallback = intentCallback(imageResourceId)
        IntentMonitorRegistry.getInstance().addIntentCallback(cameraCallback)
        view.withId(objectId).click()
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
                        resourceId
                    )
                    val out = InstrumentationRegistry.getInstrumentation()
                        .targetContext.contentResolver.openOutputStream(imageUri)
                    image.compress(Bitmap.CompressFormat.JPEG, 50, out)
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

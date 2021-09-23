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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package ch.protonmail.android.activities.messageDetails

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import ch.protonmail.android.details.presentation.model.RenderedMessage
import ch.protonmail.android.di.AttachmentsDirectory
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.utils.extensions.forEachAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.plus
import me.proton.core.util.kotlin.DispatcherProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

private const val DEBOUNCE_DELAY_MILLIS = 500L

/**
 * A class that will inline the images in the message's body.
 *
 * ## Input
 * For start the process, these functions must be called
 * * [setMessageBody]
 * * [setImagesAndStartProcess]
 *
 * ## Output
 * The results will be delivered by [results]
 *
 *
 * Implements [CoroutineScope] by the constructor scope
 *
 * @param scope [CoroutineScope] which this class inherit from, this should be our `ViewModel`s
 * scope, so when `ViewModel` is cleared all the coroutines for this class will be canceled
 */
internal class MessageRenderer(
    private val dispatchers: DispatcherProvider,
    private val documentParser: DocumentParser,
    private val bitmapImageDecoder: ImageDecoder,
    private val attachmentsDirectory: File,
    scope: CoroutineScope
) : CoroutineScope by scope + dispatchers.Comp {

    /**
     * Emits the results of Inlining process
     */
    val results: Flow<RenderedMessage> get() =
        renderedMessage.consumeAsFlow()

    /** The [String] html of the message body */
    @set:Deprecated(
        "Use 'setMessageBody' with relative messageId. This will be removed",
        ReplaceWith("setMessageBody(messageId, messageBody!!)")
    )
    var messageBody: String? = null
        set(value) {
            // Return if body is already set
            if (field != null) return

            // Update if value is not null
            if (value != null) field = value

            // Clear inlined images to ensure when messageBody changes the loading of images doesn't get blocked
            //  (messageBody changing means we're loading images for another message in the same conversation)
            inlinedImageIds.clear()
        }

    /** reference to the [Document] */
    private val document by lazy { documentParser(messageBody!!) }

    // region Actors
    /** A [Channel] for receive new [EmbeddedImage] images to inline in [document] */
    @Deprecated(
        "Use 'setImagesAndStartProcess' with relative messageId. This will be private",
        ReplaceWith("setImagesAndStartProcess(messageId, embeddedImages)")
    )
    val images = actor<List<EmbeddedImage>> {
        for (embeddedImages in channel) {
            imageCompressor.send(embeddedImages)
            // Workaround that ignore values for the next half second, since ViewModel is emitting
            // too many times
            delay(DEBOUNCE_DELAY_MILLIS)
        }
    }

    /** A [Channel] that will emits message body [String] with inlined images */
    @Deprecated(
        "Use 'results'. This will be private",
        ReplaceWith("results")
    )
    val renderedMessage = Channel<RenderedMessage>()

    /** [List] for keep track of ids of the already inlined images across the threads */
    private val inlinedImageIds = mutableListOf<String>()

    /**
     * Actor that will compress images.
     *
     * This actor will work concurrently: it will push all the [EmbeddedImage]s to be processed in
     * `imageSelector`, then it will create a pool of tasks ( which pool's size is [WORKERS_COUNT] )
     * and each task will collect and process items - concurrently - from `imageSelector` until it's
     * empty.
     */
    private val imageCompressor = actor<List<EmbeddedImage>> {
        for (embeddedImages in channel) {
            val outputs = Channel<ImageStream>(capacity = embeddedImages.size)

            /** This [Channel] works as a queue for handle [EmbeddedImage]s concurrently */
            val imageSelector = Channel<EmbeddedImage>(capacity = embeddedImages.size)

            // Queue all the embeddedImages
            for (embeddedImage in embeddedImages) {
                val contentId = embeddedImage.contentId.formatContentId()

                // Skip if we don't have a content id or already rendered
                if (contentId.isNotBlank() && contentId !in inlinedImageIds)
                    imageSelector.send(embeddedImage)
            }

            // For each worker in WORKERS_COUNT start an "async task"
            (1..WORKERS_COUNT).forEachAsync {
                // Each "task" will iterate and collect a single embeddedImage until imageSelector
                // is empty and process it asynchronously
                while (!imageSelector.isEmpty) {
                    val embeddedImage = imageSelector.receive()

                    // Process the image
                    val child = embeddedImage.localFileName ?: continue
                    val file = File(messageDirectory(embeddedImage.messageId), child)
                    // Skip if file does not exist
                    if (!file.exists() || file.length() == 0L) continue

                    val size = (MAX_IMAGES_TOTAL_SIZE / embeddedImages.size)
                        .coerceAtMost(MAX_IMAGE_SINGLE_SIZE)

                    val compressed = try {
                        ByteArrayOutputStream().also {
                            // The file could be corrupted even if exists and it's not empty
                            val bitmap = bitmapImageDecoder(file, size)
                            // Could throw `IllegalStateException` if for some reason the
                            // Bitmap is already recycled
                            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, it)
                        }
                    } catch (t: Throwable) {
                        Timber.i(t, "Skip the image")
                        // Skip the image
                        continue
                    }

                    // Add the processed image to outputs
                    outputs.send(embeddedImage to compressed)
                }
            }

            imageSelector.close()
            outputs.close()
            imageStringifier.send(outputs.toList())
        }
    }

    /** Actor that will stringify images */
    private val imageStringifier = actor<List<ImageStream>> {
        for (imageStreams in channel) {

            val imageStrings = imageStreams.map { imageStream ->
                val (embeddedImage, stream) = imageStream
                embeddedImage to Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
            }
            imageInliner.send(imageStrings)
        }
    }

    /** Actor that will inline images into [Document] */
    private val imageInliner = actor<List<ImageString>> {
        for (imageStrings in channel) {

            for (imageString in imageStrings) {

                val (embeddedImage, image64) = imageString
                val contentId = embeddedImage.contentId.formatContentId()

                // Skip if we don't have a content id or already rendered
                if (contentId.isBlank() || contentId in inlinedImageIds) continue
                idsListUpdater.send(contentId)

                val encoding = embeddedImage.encoding.formatEncoding()
                val contentType = embeddedImage.contentType.formatContentType()

                document.findImageElements(contentId)
                    ?.attr("src", "data:$contentType;$encoding,$image64")
            }

            // Extract the message ID for which embedded images are being loaded
            // to pass it back to the caller along with the rendered body
            val messageId = imageStrings.firstOrNull()?.first?.messageId ?: continue
            documentStringifier.send(messageId)
        }
    }

    /** Actor that will stringify the [document] */
    private val documentStringifier = actor<String> {
        for (messageId in channel) {
            renderedMessage.send(RenderedMessage(messageId, document.toString()))
        }
    }

    /** `CoroutineContext` for [idsListUpdater] for update [inlinedImageIds] of a single thread */
    private val idsListContext = newSingleThreadContext("idsListContext")

    /** Actor that will update [inlinedImageIds] */
    private val idsListUpdater = actor<String>(idsListContext) {
        for (id in channel) inlinedImageIds += id
    }
    // endregion

    /**
     * Set the [messageBody] for the message with the given [messageId]
     * The [messageBody] will be used when [setImagesAndStartProcess] is called for a message with the same [messageId]
     *
     * @param messageBody [String] representation of the HTML message's body
     */
    fun setMessageBody(messageId: String, messageBody: String) {

    }

    /**
     * Set [EmbeddedImage]s to be inlined in the message with the given [messageId] and start the inlining process
     * Result will be delivered through [renderedMessage]
     *
     * @throws IllegalStateException if no message body has been set for the message
     *  @see setMessageBody
     */
    fun setImagesAndStartProcess(messageId: String, images: List<EmbeddedImage>) {

    }

    /** @return [File] directory for the current message */
    private fun messageDirectory(messageId: String) = File(attachmentsDirectory, messageId)

    /**
     * A Factory for create [MessageRenderer]
     * We use this because [MessageRenderer] needs a message body that will be retrieved lazily,
     * but we still can mock [MessageRenderer] by injecting a mocked [Factory] in the `ViewModel`
     *
     * @param imageDecoder [ImageDecoder]
     */
    class Factory @Inject constructor(
        private val dispatchers: DispatcherProvider,
        @AttachmentsDirectory private val attachmentsDirectory: File,
        private val documentParser: DocumentParser = DefaultDocumentParser(),
        private val imageDecoder: ImageDecoder = DefaultImageDecoder()
    ) {

        /** @return new instance of [MessageRenderer] */
        fun create(scope: CoroutineScope) =
            MessageRenderer(dispatchers, documentParser, imageDecoder, attachmentsDirectory, scope)
    }

}

// region constants
/** A count of bytes representing the maximum total size of the images to inline */
private const val MAX_IMAGES_TOTAL_SIZE = 9_437_184 // 9 MB

/** A count of bytes representing the maximum size of a single images to inline */
private const val MAX_IMAGE_SINGLE_SIZE = 1_048_576 // 1 MB

/** Max number of concurrent workers. It represents the available processors */
private val WORKERS_COUNT get() = Runtime.getRuntime().availableProcessors()

/** Placeholder for image's id */
private const val ID_PLACEHOLDER = "%id"

/** [Array] of html attributes that could contain an image */
private val IMAGE_ATTRIBUTES =
    arrayOf("img[src=$ID_PLACEHOLDER]", "img[src=cid:$ID_PLACEHOLDER]", "img[rel=$ID_PLACEHOLDER]")
// endregion

// region typealiases
private typealias ImageStream = Pair<EmbeddedImage, ByteArrayOutputStream>
private typealias ImageString = Pair<EmbeddedImage, String>
// endregion

// region extensions
private fun String.formatEncoding() = toLowerCase()
private fun String.formatContentId() = trimStart('<').trimEnd('>')
private fun String.formatContentType() = toLowerCase()
    .replace("\r", "").replace("\n", "")
    .replaceFirst(";.*$".toRegex(), "")

/**
 * Flatten the receiver [Document] by removing the indentation and disabling prettyPrint.
 * @return [Document]
 */
private fun Document.flatten() = apply { outputSettings().indentAmount(0).prettyPrint(false) }

/** @return [Elements] matching the image attribute for the given [id] */
private fun Document.findImageElements(id: String): Elements? {
    return IMAGE_ATTRIBUTES
        .map { attr -> attr.replace(ID_PLACEHOLDER, id) }
        // with `asSequence` iteration will stop when the first usable element
        // is found and so avoid to make too many calls to document.select
        .asSequence()
        .map { select(it) }
        .find { it.isNotEmpty() }
}
// endregion

// region DocumentParser
/**
 * Parses a document as [String] and returns a [Document] model
 */
internal interface DocumentParser {
    operator fun invoke(body: String): Document
}

/**
 * Default implementation of [DocumentParser]
 */
internal class DefaultDocumentParser @Inject constructor() : DocumentParser {
    override fun invoke(body: String): Document = Jsoup.parse(body).flatten()
}
// endregion

// region ImageDecoder
/**
 * Decodes to [Bitmap] the image provided by the given [File] to fit the max size provided
 */
internal interface ImageDecoder {
    operator fun invoke(file: File, maxBytes: Int): Bitmap
}

/**
 * Default implementation of [ImageDecoder]
 */
internal class DefaultImageDecoder @Inject constructor() : ImageDecoder {
    override fun invoke(file: File, maxBytes: Int): Bitmap {
        // https://stackoverflow.com/a/8497703/6372379

        // Decode image size
        val boundOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundOptions)

        var scale = 1
        while (boundOptions.outWidth * boundOptions.outHeight * (1 / scale.toDouble().pow(2.0)) > maxBytes) {
            scale++
        }

        return if (scale > 1) {
            scale--
            // scale to max possible inSampleSize that still yields an image larger than target
            val options = BitmapFactory.Options().apply { inSampleSize = scale }
            val tempBitmap = BitmapFactory.decodeFile(file.absolutePath, options)

            // resize to desired dimensions
            val height = tempBitmap.height
            val width = tempBitmap.width

            val y = sqrt(maxBytes / (width.toDouble() / height))
            val x = (y / height) * width

            val scaledBitmap = Bitmap.createScaledBitmap(tempBitmap, x.toInt(), y.toInt(), true)
            tempBitmap.recycle()

            scaledBitmap

        } else {
            BitmapFactory.decodeFile(file.absolutePath)
        }
    }
}
// endregion

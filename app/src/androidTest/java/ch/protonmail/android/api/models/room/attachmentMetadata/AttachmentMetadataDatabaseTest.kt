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
package ch.protonmail.android.api.models.room.attachmentMetadata

import android.net.Uri
import androidx.room.Room
import androidx.test.InstrumentationRegistry
import ch.protonmail.android.testAndroidInstrumented.ReflectivePropertiesMatcher
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert
import kotlin.test.Test

class AttachmentMetadataDatabaseTest {
    private val context = InstrumentationRegistry.getTargetContext()
    private var databaseFactory = Room.inMemoryDatabaseBuilder(
        context,
        AttachmentMetadataDatabaseFactory::class.java
    ).build()
    private var database = databaseFactory.getDatabase()

    private val first = AttachmentMetadata("a", "b", 3, "c", "d", 10, Uri.parse("a"))
    private val second = AttachmentMetadata("e", "f", 5, "g", "h", 11, Uri.parse("e"))
    private val third = AttachmentMetadata("i", "j", 7, "k", "l", 8, Uri.parse("i"))
    private val standardTest = listOf(first, second, third)
    private fun AttachmentMetadataDatabase.insert(attachments: Iterable<AttachmentMetadata>) {
        attachments.forEach(this::insertAttachmentMetadataBlocking)
    }

    @Test
    fun insertGetAll() {
        val inserted = standardTest
        val expected = standardTest.map { ReflectivePropertiesMatcher(it) }
        database.insert(inserted)
        val actual = database.getAllAttachmentsMetadata().sortedBy(AttachmentMetadata::id)
        Assert.assertThat(actual, containsInAnyOrder(expected))
    }

    @Test
    fun insertDeleteGetAll() {
        val inserted = standardTest
        val expected = listOf(first, third)
        val expectedMatchers = expected.map { ReflectivePropertiesMatcher(it) }
        database.insert(inserted)
        database.deleteAttachmentMetadata(second)
        val actual = database.getAllAttachmentsMetadata().sortedBy(AttachmentMetadata::id)
        Assert.assertThat(actual, containsInAnyOrder(expectedMatchers))
    }

    @Test
    fun insertClearCacheGetAll() {
        val inserted = standardTest
        val expected = emptyList<AttachmentMetadata>()
        database.insert(inserted)
        database.clearAttachmentMetadataCache()
        val actual = database.getAllAttachmentsMetadata().sortedBy(AttachmentMetadata::id)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun insertGetAllAttachmentsSizeUsed() {
        val inserted = standardTest
        val expected = 15L
        database.insert(inserted)
        val actual = database.getAllAttachmentsSizeUsed()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun insertGetAllAttachmentsForMessage() {
        val inserted = standardTest
        val expected = listOf(second).map { ReflectivePropertiesMatcher(it) }
        database.insert(inserted)
        val actual = database.getAllAttachmentsForMessage("h")
        Assert.assertThat(actual, containsInAnyOrder(expected))
    }

    @Test
    fun insertReplaceGetAll() {
        val inserted = standardTest
        val replacement = AttachmentMetadata("e", "eee", 18, "eee", "eee", 123, Uri.parse("e"))
        val expected = listOf(first, replacement, third).map { ReflectivePropertiesMatcher(it) }
        database.insert(inserted)
        database.insertAttachmentMetadataBlocking(replacement)
        val actual = database.getAllAttachmentsMetadata().sortedBy(AttachmentMetadata::id)
        Assert.assertThat(actual, containsInAnyOrder(expected))
    }
}

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

package ch.protonmail.android.labels.data.mapper

import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelType
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.system.measureTimeMillis
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

@Suppress("PrivatePropertyName") // `_` for readability purpose on big numbers
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore("Benchmarks are useful only when observed, not needed to run for every pipeline")
class LabelOrFolderWithChildrenMapperBenchmarks {

    private val mapper = LabelOrFolderWithChildrenMapper(TestDispatcherProvider())

    private lateinit var data50parents100children: List<LabelEntity>
    private lateinit var data200parents800children: List<LabelEntity>
    private lateinit var data1_000parents10_000children: List<LabelEntity>
    private lateinit var data2_000parents18_000children: List<LabelEntity>

    @BeforeTest
    fun setupLists() {
        fun buildList(parentsCount: Int, childrenCount: Int): List<LabelEntity> {
            val parents = 1..parentsCount
            val children = parentsCount + 1..childrenCount + parentsCount
            val all = parents + children
            return parents.map { name -> buildLabelEntity(name.toString()) } +
                children.map { name -> buildLabelEntity(name.toString(), parent = all.random().toString()) }
        }

        data50parents100children = buildList(parentsCount = 50, childrenCount = 100)
        data200parents800children = buildList(parentsCount = 200, childrenCount = 800)
        data1_000parents10_000children = buildList(parentsCount = 1_000, childrenCount = 10_000)
        data2_000parents18_000children = buildList(parentsCount = 10_000, childrenCount = 18_000)
    }

    @Test
    fun test1with50parents100children() = runTest {
        runBenchmark("50-100", data50parents100children)
    }

    @Test
    fun test2with200parents800children() = runTest {
        runBenchmark("200-800", data200parents800children)
    }

    @Test
    fun test3with1_000parents10_000children() = runTest {
        runBenchmark("1.000-10.000", data1_000parents10_000children)
    }

    @Test
    fun test4with2_000parents18_000children() = runTest {
        runBenchmark("2.000-18.000", data2_000parents18_000children)
    }

    private suspend fun runBenchmark(
        @Suppress("unused") traceName: String,
        input: List<LabelEntity>,
        shouldPrintOutput: Boolean = false
    ) {
        // when
        var result: List<LabelOrFolderWithChildren>
        val time = measureTimeMillis {
            // Debug.startMethodTracing(traceName)
            result = mapper.toLabelsAndFoldersWithChildren(input)
            // Debug.stopMethodTracing()
        }

        // then
        println(time)
        if (shouldPrintOutput) println(result.prettyPrint())
    }

    private fun buildLabelEntity(
        name: String,
        type: LabelType = LabelType.FOLDER,
        parent: String = EMPTY_STRING
    ) = LabelEntity(
        id = LabelId(name),
        userId = UserId("user"),
        name = name,
        color = EMPTY_STRING,
        order = 0,
        type = type,
        path = EMPTY_STRING,
        parentId = parent,
        expanded = 0,
        sticky = 0,
        notify = 0
    )
}

fun List<LabelOrFolderWithChildren>.prettyPrint(): String =
    joinToString(separator = ",\n") { it.prettyPrint() }

private fun Collection<LabelOrFolderWithChildren>.joinToString(hasParent: Boolean): String {
    return if (isEmpty()) {
        EMPTY_STRING
    } else {
        val itemPadding = if (hasParent) "        " else "    "
        val bracketPadding = if (hasParent) "    " else ""
        " {\n${joinToString(separator = ",\n") { "$itemPadding${it.prettyPrint()}" }}\n$bracketPadding}"
    }
}

private fun LabelOrFolderWithChildren.prettyPrint(): String {
    return when (this) {
        is LabelOrFolderWithChildren.Folder -> {
            val name = "name: $name"
            val parent = if (parentId == null) EMPTY_STRING else " - parent: $parentId"
            "$name$parent${children.joinToString(parentId != null)}"
        }
        is LabelOrFolderWithChildren.Label -> name
    }
}

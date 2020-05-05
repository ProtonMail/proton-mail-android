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
@file:Suppress("PropertyName")

import java.io.File
import java.io.IOException

/*
 * This script will Inject the license header for all the source files who are not provided with it
 * @author Davide Farella
 */


/** Extension to take for apply the header */
val EXTENSIONS = arrayOf("kt", "kts", "java", "xml")

/** Find the root dir of our project. `` **./proton-mail-android `` */
val ROOT_DIR = with(StringBuilder(File("").absolutePath)) {
    // Path for CLI gradle commands is usually '/Users/<username>/.gradle/<gradle-version>', so in
    // these situations we cannot get the root of our Project
    if (".gradle" in toString()) return@with null

    if (last() == File.separatorChar) deleteCharAt(lastIndex)

    var current = ""
    while (current != "proton-mail-android") {
        val lastSeparatorIndex = indexOfLast { it == File.separatorChar }
        current = substring(lastSeparatorIndex + 1..lastIndex)
        delete(lastSeparatorIndex, length)
    }

    val path = toString() + File.separator + current
    return@with File(path)
}

val LICENSE = """
Copyright (c) 2020 Proton Technologies AG

This file is part of ProtonMail.

ProtonMail is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ProtonMail is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ProtonMail. If not, see https://www.gnu.org/licenses/.
""".trim()

val _licensesByExtension = run {
    val licenseLines = LICENSE.split("\n")
    val javaKotlinLicense = licenseLines.joinToString(prefix = "/*\n * ", postfix = "\n */", separator = "\n * ")
    val xmlLicense = licenseLines.joinToString(prefix = "<!--\n", postfix = "\n-->", separator = "\n")

    EXTENSIONS.map {
        it to when (it) {
            "kt", "kts", "java" -> javaKotlinLicense
            "xml" -> xmlLicense
            else -> throw IllegalArgumentException()
        }
    }.toMap()
}

/**
 * @return all the Files present in this directory and sub-directories
 * @return a list containing only this [File], if it's not a directory
 */
fun File.children(): List<File> {
    return when {

        // skip `build` folders
        "${File.separator}build${File.separator}" in absolutePath -> emptyList()

        isFile -> listOf(this)

        else -> (listFiles { file ->

            // Take directories or files that respect the `EXTENSIONS` constraint
            file.isDirectory || file.extension in EXTENSIONS

        } ?: arrayOf()).flatMap {

            // Take children of filtered files
            it.children()
        }
    }
}

val xmlHeader = """<?xml version="1.0" encoding="UTF-8"?>"""

/**
 * Add license to receiver [File]
 * @return `true` if license has been added, `false` if already present
 */
fun File.addLicense(): Boolean {

    val license = licenseFor(this)
    val (commentStart, commentEnd) = with(license.lineSequence()) { first() to last() }

    bufferedReader().use { reader ->
        try {
            // Mark the start of the file, we might need it if no comment is found
            reader.mark(64)

            val fistLine = reader.readLine() ?: throw IOException("File ${this.absolutePath} is empty.")
            val hasHeader = fistLine.startsWith(xmlHeader, ignoreCase = true)
            if (hasHeader) reader.mark(64) else reader.reset()

            val startsWithComment =
                (reader.readLine()?.startsWith(commentStart) ?: false).also { reader.reset() }
            val initialComment = startsWithComment.takeIf { it }?.let {
                val builder = StringBuilder()
                while (true) {
                    val line = reader.readLine()
                    builder.appendln(line)
                    if (commentEnd in line) break
                }
                builder.toString()
            }

            if (initialComment?.trim() == license.trim()) {
                return false
            }

            // Create a temp file
            val temp = File.createTempFile("tmp_", null, parentFile).apply {
                writer().buffered().use {
                    if (hasHeader) it.appendln(fistLine)
                    it.appendln(license)
                    reader.copyTo(it)
                }
            }
            // copy to original file
            temp.renameTo(this)

        } catch (e: IOException) {
            if (e.message == "File $path is empty.") {
                // Delete file if is empty
                delete()
            } else {
                throw IOException(e.message + " for file ${this.absolutePath}")
            }
        }
    }

    return true
}

/** @return [String] of formatted license, as comment, for the given [file] */
fun licenseFor(file: File) = _licensesByExtension[file.extension]
    ?: throw IllegalArgumentException("File with extension '${file.extension}' should not be here")




ROOT_DIR?.run {
    println()
    println("Using dir: ${absolutePath}")
    println("Found: ${children().size} files")
    println("Updated: ${children().filter { it.addLicense() }.size} files")
    println()
}


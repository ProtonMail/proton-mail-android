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
package ch.protonmail.tokenautocomplete.example

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.tokenautocomplete.TagTokenizer
import ch.protonmail.tokenautocomplete.TokenCompleteTextView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class TokenActivity : AppCompatActivity(), TokenCompleteTextView.TokenListener<Person> {
    @VisibleForTesting val completionView: ContactsCompletionView by lazy { searchView }
    private val people by lazy { Person.samplePeople() }
    private val adapter by lazy { PersonAdapter(this, R.layout.person_layout, people) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tabs = tabHost
        tabs.setup()
        tabs.addTab(tabs.newTabSpec("Contacts").setContent(R.id.contactsFrame).setIndicator("Contacts"))
        tabs.addTab(tabs.newTabSpec("Composer").setContent(R.id.hashtagsFrame).setIndicator("Composer"))

        completionView.setAdapter(adapter)
        completionView.threshold = 1
        completionView.setTokenListener(this)
        completionView.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.None)
        completionView.isLongClickable = true
        completionView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun afterTextChanged(editable: Editable) {
                (findViewById<View>(R.id.textValue) as TextView).text = editable.toString()
            }
        })


        if (savedInstanceState == null) {
            completionView.addObjectSync(people[0])
            completionView.addObjectSync(people[1])
        }

        val removeButton = findViewById<View>(R.id.removeButton) as Button
        removeButton.setOnClickListener {
            val people = completionView.objects
            if (people.size > 0) {
                completionView.removeObjectAsync(people[people.size - 1])
            }
        }

        val addButton = findViewById<View>(R.id.addButton) as Button
        addButton.setOnClickListener {
            val rand = Random()
            completionView.addObjectAsync(people[rand.nextInt(people.size)])
        }

        //Setup the tag composer view
        val tagView = findViewById<TagCompletionView>(R.id.composeView)
        tagView.performBestGuess(false)
        tagView.preventFreeFormText(false)
        tagView.setTokenizer(TagTokenizer(listOf('@', '#')))
        tagView.setAdapter(TagAdapter(this, R.layout.tag_layout, Tag.sampleTags()))
        tagView.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select)
        tagView.threshold = 1

        val taggedContentPreview = findViewById<TextView>(R.id.composedValue)

        tagView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun afterTextChanged(editable: Editable) {
                taggedContentPreview.text = tagView.contentText.toString()
            }
        })

        //NOTE: this is not a great general solution to the problem of setting/restoring tokenized
        //freeform text. I'm still looking for a good solution that would also allow pasting raw
        //text into the view and having tokens get processed in it
        tagView.setText("A sample ")
        tagView.addObjectSync(Tag.sampleTags()[0])
        tagView.append("tweet with ")
        tagView.addObjectSync(Tag.sampleTags()[5])
        tagView.addObjectSync(Tag.sampleTags()[10])
    }

    private fun updateTokenConfirmation() {
        val sb = StringBuilder("Current tokens:\n")
        for (token in completionView.objects) {
            sb.append(token.toString())
            sb.append("\n")
        }

        (findViewById<View>(R.id.tokens) as TextView).text = sb
    }


    override fun onTokenAdded(token: Person) {
        (findViewById<View>(R.id.lastEvent) as TextView).text = "Added: $token"
        updateTokenConfirmation()
    }

    override fun onTokenRemoved(token: Person) {
        (findViewById<View>(R.id.lastEvent) as TextView).text = "Removed: $token"
        updateTokenConfirmation()
    }

    override fun onTokenIgnored(token: Person) {
        (findViewById<View>(R.id.lastEvent) as TextView).text = "Ignored: $token"
        updateTokenConfirmation()
    }
}

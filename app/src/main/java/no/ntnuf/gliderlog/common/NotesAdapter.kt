package no.ntnuf.gliderlog.common

import android.content.Context
import android.content.SharedPreferences
import android.widget.ArrayAdapter

class NotesAdapter(context: Context, settings: SharedPreferences) {
    private val adapter: ArrayAdapter<String>

    init {
        val defaultNotes = settings.getString("default_notes", "") ?: ""
        val notesArray = if (defaultNotes.isBlank()) emptyArray() else defaultNotes.split(",").toTypedArray()
        adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, notesArray)
    }

    fun getNotesAdapter(): ArrayAdapter<String> {
        return adapter
    }
}


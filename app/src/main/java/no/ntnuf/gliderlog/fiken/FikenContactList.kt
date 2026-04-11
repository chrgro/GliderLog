package no.ntnuf.gliderlog.fiken

import org.json.JSONArray
import java.io.Serializable

class FikenContactList : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    val contacts: ArrayList<FikenContact> = arrayListOf()

    fun addToFikenContactList(jsonContacts: JSONArray): Boolean {
        for (i in 0 until jsonContacts.length()) {
            val entry = jsonContacts.getJSONObject(i)
            if (!entry.optBoolean("customer", false)) {
                continue
            }

            val contact = FikenContact.createContact(entry)
            contacts.add(contact)
        }
        return true
    }
}


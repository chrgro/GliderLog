package no.ntnuf.gliderlog.common

import java.io.Serializable
import java.util.ArrayList

class ContactList : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2L
    }

    var contacts: ArrayList<Contact> = ArrayList()

    fun clear() {
        contacts.clear()
    }

    fun findContactFromName(name: String?): Contact? {
        if (name == null) return null
        for (contact in contacts) {
            if (contact.name == name) {
                return contact
            }
        }
        return null
    }

    fun addContact(contact: Contact) {
        val existing = findContactFromName(contact.name)
        if (existing == null) {
            contacts.add(contact)
        }
    }

    fun remove(index: Int) {
        contacts.removeAt(index)
    }
}


package no.ntnuf.gliderlog.common

import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import no.ntnuf.gliderlog.fiken.FikenContactList
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OptionalDataException
import java.io.StreamCorruptedException

class ContactListManager(private val context: Context) {
    companion object {
        private const val ACCOUNT_CHECKMARK_SUFFIX = " \u2713"

        fun toSuggestionLabel(contactName: String, hasAccount: Boolean): String {
            return if (hasAccount) "$contactName$ACCOUNT_CHECKMARK_SUFFIX" else contactName
        }

        fun normalizeSuggestionLabel(value: String): String {
            val trimmed = value.trim()
            return if (trimmed.endsWith(ACCOUNT_CHECKMARK_SUFFIX)) {
                trimmed.removeSuffix(ACCOUNT_CHECKMARK_SUFFIX).trimEnd()
            } else {
                trimmed
            }
        }
    }

    private var contactlist: ContactList = ContactList()
    private val adapter: ArrayAdapter<Contact>

    private val filenameContactlist = "contactlist.serialized"

    init {
        if (!loadLocalContacts()) {
            contactlist = ContactList()
        }
        adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            contactlist.contacts
        )
    }

    fun clearList(): Boolean {
        contactlist = ContactList()
        return save()
    }

    fun findContactFromName(name: String?): Contact? {
        return contactlist.findContactFromName(name)
    }

    fun saveContact(contactname: String): Contact? {
        val found = findContactFromName(contactname)
        var newcontact: Contact? = null
        if (found == null) {
            newcontact = Contact()
            newcontact.name = contactname
            contactlist.addContact(newcontact)
        }
        save()
        return newcontact
    }

    fun saveContact(contact: Contact) {
        contactlist.addContact(contact)
        save()
    }

    private fun save(): Boolean {
        return try {
            context.openFileOutput(filenameContactlist, Context.MODE_PRIVATE).use { fos ->
                ObjectOutputStream(fos).use { os ->
                    os.writeObject(contactlist)
                }
            }
            true
        } catch (e: FileNotFoundException) {
            Log.e("ContactListManager", "While saving, file not found $filenameContactlist", e)
            false
        } catch (e: IOException) {
            Log.e("ContactListManager", "IO Exception during saving $filenameContactlist", e)
            false
        }
    }

    private fun loadLocalContacts(): Boolean {
        return try {
            context.openFileInput(filenameContactlist).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    contactlist = ois.readObject() as ContactList
                }
            }
            true
        } catch (e: ClassNotFoundException) {
            Log.e("ContactListManager", "Class not found", e)
            false
        } catch (e: OptionalDataException) {
            Log.e("ContactListManager", "Optional Data Exception", e)
            false
        } catch (e: FileNotFoundException) {
            Log.e("ContactListManager", "File not found", e)
            false
        } catch (e: StreamCorruptedException) {
            Log.e("ContactListManager", "Stream Corrupted", e)
            false
        } catch (e: IOException) {
            Log.e("ContactListManager", "IO Exception while loading", e)
            false
        }
    }

    fun getContactNameListAdapter(): ArrayAdapter<Contact> {
        return adapter
    }

    fun getContactSuggestionListAdapter(): ArrayAdapter<String> {
        val labels = contactlist.contacts.map {
            toSuggestionLabel(it.name.orEmpty(), it.hasAccount)
        }
        return ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, labels)
    }

    fun setFikenContacts(fikenContactList: FikenContactList) {
        contactlist.clear()
        for (contact in fikenContactList.contacts) {
            contactlist.addContact(contact)
        }
        save()
        adapter.notifyDataSetChanged()
    }
}


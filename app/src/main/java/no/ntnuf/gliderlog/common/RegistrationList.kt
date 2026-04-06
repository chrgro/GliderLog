package no.ntnuf.gliderlog.common

import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OptionalDataException
import java.io.StreamCorruptedException

class RegistrationList(private val context: Context) {
    private val maxRegistrations = 100
    private val filenameRegistrationList = "registrationlist.serialized"

    private var registrations: ArrayList<String> = arrayListOf()
    private val adapter: ArrayAdapter<String>

    init {
        if (!load()) {
            registrations = arrayListOf()
        }
        adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, registrations)
    }

    fun clearList(): Boolean {
        registrations = arrayListOf()
        return save()
    }

    fun addRegistration(registration: String) {
        val normalized = registration.uppercase()
        val found = registrations.indexOf(normalized)
        if (found >= 0) {
            registrations.removeAt(found)
        }
        registrations.add(0, normalized)
        if (registrations.size >= maxRegistrations) {
            registrations.removeAt(registrations.size - 1)
        }
        save()
    }

    private fun save(): Boolean {
        return try {
            context.openFileOutput(filenameRegistrationList, Context.MODE_PRIVATE).use { fos ->
                ObjectOutputStream(fos).use { os ->
                    os.writeObject(registrations)
                }
            }
            true
        } catch (e: FileNotFoundException) {
            Log.e("RegistrationList", "While saving, file not found $filenameRegistrationList", e)
            false
        } catch (e: IOException) {
            Log.e("RegistrationList", "IO Exception during saving $filenameRegistrationList", e)
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun load(): Boolean {
        return try {
            context.openFileInput(filenameRegistrationList).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    registrations = ois.readObject() as ArrayList<String>
                }
            }
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: OptionalDataException) {
            false
        } catch (_: FileNotFoundException) {
            false
        } catch (_: StreamCorruptedException) {
            false
        } catch (_: IOException) {
            false
        }
    }

    fun getRegistrationListAdapter(): ArrayAdapter<String> {
        return adapter
    }
}


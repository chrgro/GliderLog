package no.ntnuf.gliderlog.fiken

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.preference.PreferenceManager
import no.ntnuf.gliderlog.common.ContactListManager
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class FikenContactRequestTask {
    private var context: Context? = null
    private var alertDialog: AlertDialog? = null
    private var contactListManager: ContactListManager? = null

    fun setContext(context: Context) {
        this.context = context
    }

    fun setDialog(alertDialog: AlertDialog) {
        this.alertDialog = alertDialog
    }

    fun setContactListManager(contactListManager: ContactListManager) {
        this.contactListManager = contactListManager
    }

    fun execute() {
        val ctx = context ?: return
        alertDialog?.show()

        thread {
            val result = loadContacts(ctx)
            android.os.Handler(ctx.mainLooper).post {
                if (result == null) {
                    Toast.makeText(ctx, "Failed to load contacts", Toast.LENGTH_LONG).show()
                } else {
                    contactListManager?.setFikenContacts(result)
                    Toast.makeText(ctx, "Loaded contacts", Toast.LENGTH_LONG).show()
                }
                if (alertDialog?.isShowing == true) {
                    alertDialog?.dismiss()
                }
            }
        }
    }

    private fun loadContacts(context: Context): FikenContactList? {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val baseUrl = settings.getString("fiken_api_url", "") ?: ""
        val token = settings.getString("fiken_api_password", "") ?: ""
        if (baseUrl.isBlank() || token.isBlank()) {
            return null
        }

        val contacts = FikenContactList()
        var page = 0
        while (page < 999) {
            val separator = if (baseUrl.contains("?")) "&" else "?"
            val url = "$baseUrl${separator}pageSize=100&page=$page"
            val body = httpGet(url, token) ?: return null
            val pageContacts = JSONArray(body)
            if (pageContacts.length() == 0) {
                break
            }
            contacts.addToFikenContactList(pageContacts)
            page += 1
        }
        return contacts
    }

    private fun httpGet(url: String, token: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}


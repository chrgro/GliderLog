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
    companion object {
        private const val PAGE_SIZE = 100
    }

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
                    Toast.makeText(ctx, "Loaded contacts (${result.contacts.size})", Toast.LENGTH_LONG).show()
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
            val url = buildContactsUrl(baseUrl, page)
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

    private fun buildContactsUrl(baseUrl: String, page: Int): String {
        val split = baseUrl.split("?", limit = 2)
        val endpoint = split[0]
        val existingQuery = split.getOrNull(1).orEmpty()

        val retainedParams = existingQuery
            .split("&")
            .filter { it.isNotBlank() }
            .filterNot {
                it.startsWith("customer=") ||
                    it.startsWith("inactive=") ||
                    it.startsWith("page=") ||
                    it.startsWith("pageSize=")
            }

        val params = ArrayList<String>()
        params.addAll(retainedParams)
        params.add("customer=true")
        params.add("inactive=false")
        params.add("pageSize=$PAGE_SIZE")
        params.add("page=$page")

        return "$endpoint?${params.joinToString("&")}"
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

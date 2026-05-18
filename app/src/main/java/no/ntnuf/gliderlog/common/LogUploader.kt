package no.ntnuf.gliderlog.common

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread
import android.util.Log

class LogUploader(
    private val context: Context,
    private val settings: SharedPreferences,
) {
    private val filename = "pending_uploads_list"
    private var pendingUploads: PendingUploadsList = PendingUploadsList()

    init {
        if (!loadPendingUploadsList()) {
            pendingUploads = PendingUploadsList()
        }
    }

    fun addToUploadQueue(daylog: DayLog) {
        val name = daylog.getFilename()
        if (!pendingUploads.pendingFiles.contains(name)) {
            pendingUploads.pendingFiles.add(name)
            savePendingUploadsList()
        }
    }

    fun uploadPending() {
        thread {
            if (pendingUploads.pendingFiles.isEmpty()) {
                return@thread
            }
            val completed = arrayListOf<String>()
            for (pendingName in pendingUploads.pendingFiles) {
                val dayLog = loadDayLog(pendingName) ?: continue
                if (uploadOne(dayLog)) {
                    completed.add(pendingName)
                }
            }

            if (completed.isNotEmpty()) {
                pendingUploads.pendingFiles.removeAll(completed.toSet())
                savePendingUploadsList()
            }

            val msg = if (completed.isNotEmpty()) {
                "Uploaded ${completed.size} pending day logs to webserver. Remember to also send via email."
            } else {
                "No internet connection, will upload day log to webserver later. Remember to also send via email."
            }
            android.os.Handler(context.mainLooper).post {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadOne(daylog: DayLog): Boolean {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(daylog.date)
        val tmpFile = File(context.filesDir, "tmp_daylog.json")

        return try {
            tmpFile.writeText(daylog.getJSONOutput())
            val requestUrl = settings.getString("upload_log_url", "") ?: ""
            if (requestUrl.isBlank() || requestUrl == "https://") {
                return false
            }

            val username = settings.getString("upload_log_username", null)
            val password = settings.getString("upload_log_password", null)
            val credentials = if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                "$username:$password"
            } else {
                null
            }

            val multipart = MultipartUtility(requestUrl, "UTF-8", credentials)
            multipart.addFormField("date", dateStr)
            multipart.addFilePart("logfile", tmpFile)
            multipart.finish()
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun loadDayLog(fileName: String): DayLog? {
        return try {
            context.openFileInput(fileName).use { fis ->
                ObjectInputStream(fis).use { input ->
                    input.readObject() as DayLog
                }
            }
        } catch (_: ClassNotFoundException) {
            null
        } catch (_: FileNotFoundException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    private fun savePendingUploadsList() {
        try {
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
                ObjectOutputStream(fos).use { out ->
                    out.writeObject(pendingUploads)
                }
            }
        } catch (_: IOException) {
            // Keep upload queue best-effort.
        }
    }

    private fun loadPendingUploadsList(): Boolean {
        return try {
            context.openFileInput(filename).use { fis ->
                ObjectInputStream(fis).use { input ->
                    pendingUploads = input.readObject() as PendingUploadsList
                }
            }
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: FileNotFoundException) {
            false
        } catch (_: IOException) {
            false
        }
    }
}



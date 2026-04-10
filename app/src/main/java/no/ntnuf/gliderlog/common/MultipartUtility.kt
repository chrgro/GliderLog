package no.ntnuf.gliderlog.common

import android.util.Base64
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

class MultipartUtility(
    requestUrl: String,
    private val charset: String,
    userCredentialsOrNull: String?,
) {
    private val boundary = "-----${System.currentTimeMillis()}----------"
    private val httpConn: HttpURLConnection
    private val outputStream: OutputStream
    private val writer: PrintWriter

    init {
        val url = URL(requestUrl)
        httpConn = (url.openConnection() as HttpURLConnection).apply {
            useCaches = false
            doOutput = true
            doInput = true
            if (userCredentialsOrNull != null) {
                setRequestProperty(
                    "Authorization",
                    "Basic ${Base64.encodeToString(userCredentialsOrNull.toByteArray(), Base64.NO_WRAP)}",
                )
            }
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("User-Agent", "GliderLog")
        }
        outputStream = httpConn.outputStream
        writer = PrintWriter(OutputStreamWriter(outputStream, charset), true)
    }

    fun addFormField(name: String, value: String) {
        writer.append("--$boundary").append(LINE_FEED)
        writer.append("Content-Disposition: form-data; name=\"$name\"").append(LINE_FEED)
        writer.append("Content-Type: text/plain; charset=$charset").append(LINE_FEED)
        writer.append(LINE_FEED)
        writer.append(value).append(LINE_FEED)
        writer.flush()
    }

    fun addFilePart(fieldName: String, uploadFile: File) {
        val fileName = uploadFile.name
        writer.append("--$boundary").append(LINE_FEED)
        writer.append("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"")
            .append(LINE_FEED)
        writer.append("Content-Type: ${URLConnection.guessContentTypeFromName(fileName)}")
            .append(LINE_FEED)
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED)
        writer.append(LINE_FEED)
        writer.flush()

        FileInputStream(uploadFile).use { inputStream ->
            val buffer = ByteArray(4096)
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
            outputStream.flush()
        }

        writer.append(LINE_FEED)
        writer.flush()
    }

    @Throws(IOException::class)
    fun finish(): String {
        val response = StringBuilder()

        writer.append(LINE_FEED).flush()
        writer.append("--$boundary--").append(LINE_FEED)
        writer.close()

        val status = httpConn.responseCode
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(httpConn.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    response.append(line)
                    line = reader.readLine()
                }
            }
            httpConn.disconnect()
        } else {
            throw IOException("Server returned non-OK status: $status")
        }

        return response.toString()
    }

    companion object {
        private const val LINE_FEED = "\r\n"
    }
}


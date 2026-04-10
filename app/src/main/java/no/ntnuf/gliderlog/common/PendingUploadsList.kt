package no.ntnuf.gliderlog.common

import java.io.Serializable

class PendingUploadsList : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    val pendingFiles: ArrayList<String> = arrayListOf()
}


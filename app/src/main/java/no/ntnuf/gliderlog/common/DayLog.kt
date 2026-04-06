package no.ntnuf.gliderlog.common

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

class DayLog : Serializable {
    companion object {
        private const val serialVersionUID: Long = 8L
        const val dayLogFileName: String = "gliderlog_"
    }

    var headOfOperations: Contact? = null
    var airfield: String? = null
    var date: Date = Date()
    var flights: ArrayList<FlightEntry> = ArrayList()

    var logIsLocked: Boolean = false
    private var logHasBeenSent: Boolean = false
    private var logSentNumberOfTimes: Int = 0

    fun getFilename(): String {
        val outdf = SimpleDateFormat("yyyy_MM_dd")
        val daylogsuffix = outdf.format(date)
        return dayLogFileName + daylogsuffix
    }

    fun setLogHasBeenSent() {
        logHasBeenSent = true
        logIsLocked = true
        logSentNumberOfTimes += 1
    }
}


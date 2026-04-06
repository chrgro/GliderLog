package no.ntnuf.gliderlog.common

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class FlightEntry : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L

        fun hhcolonmmFromDate(date: Date): String {
            val outdf = SimpleDateFormat("HH:mm")
            return outdf.format(date)
        }
    }

    var pilot: Contact? = null
    var pilotType: PilotType? = null
    var copilot: Contact? = null
    var copilotType: PilotType? = null
    var status: FlightStatus? = null

    var takeoff: Date? = null
    var towRelease: Date? = null
    var landing: Date? = null

    var registration: String = ""
    var notes: String = ""

    private fun dateFromHourMinute(hour: Int, minute: Int, date: Date): Date {
        val now = Calendar.getInstance()
        now.time = date
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        now.set(Calendar.HOUR_OF_DAY, hour)
        now.set(Calendar.MINUTE, minute)
        return now.time
    }

    fun getTowDurationStr(): String = getDurationStr(takeoff, towRelease)

    fun getFlightDurationStr(): String = getDurationStr(takeoff, landing)

    fun getTowDurationMinutes(): Long = getDurationMinutes(takeoff, towRelease)

    fun getFlightDurationMinutes(): Long = getDurationMinutes(takeoff, landing)

    private fun getDurationMinutes(from: Date?, to: Date?): Long {
        if (from == null || to == null) return 0
        val durationMillis = to.time - from.time
        return durationMillis / (1000 * 60)
    }

    private fun getDurationStr(from: Date?, to: Date?): String {
        if (from == null || to == null) {
            return ""
        }
        val durationMillis = to.time - from.time
        val hours = durationMillis / (1000 * 60 * 60)
        val minutes = (durationMillis / (1000 * 60)) % 60
        return "${hours}h ${minutes}m"
    }

    fun setTakeoff(hour: Int, minute: Int, date: Date) {
        setTakeoff(hour, minute, date, false)
    }

    fun setTakeoff(hour: Int, minute: Int, date: Date, keepStatus: Boolean) {
        takeoff = dateFromHourMinute(hour, minute, date)
        if (!keepStatus) {
            status = FlightStatus.DEPARTED
        }
    }

    fun setTowRelease(hour: Int, minute: Int, date: Date) {
        setTowRelease(hour, minute, date, false)
    }

    fun setTowRelease(hour: Int, minute: Int, date: Date, keepStatus: Boolean) {
        towRelease = dateFromHourMinute(hour, minute, date)
        if (!keepStatus) {
            status = FlightStatus.RELEASED
        }
    }

    fun setLanding(hour: Int, minute: Int, date: Date) {
        setLanding(hour, minute, date, false)
    }

    fun setLanding(hour: Int, minute: Int, date: Date, keepStatus: Boolean) {
        landing = dateFromHourMinute(hour, minute, date)
        if (!keepStatus) {
            status = FlightStatus.LANDED
        }
    }
}


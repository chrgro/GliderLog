package no.ntnuf.gliderlog.common

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

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
        val outdf = SimpleDateFormat("yyyy_MM_dd", Locale.ENGLISH)
        val daylogsuffix = outdf.format(date)
        return dayLogFileName + daylogsuffix
    }

    fun setLogHasBeenSent() {
        logHasBeenSent = true
        logIsLocked = true
        logSentNumberOfTimes += 1
    }

    fun sortOnTakeoff() {
        flights.sortWith(compareBy<FlightEntry> { it.takeoff == null }.thenBy { it.takeoff })
    }

    fun isLogComplete(): Boolean {
        for (flight in flights) {
            if (flight.takeoff == null || flight.towRelease == null || flight.landing == null) {
                return false
            }
            if (flight.status != FlightStatus.LANDED) {
                return false
            }
        }
        return true
    }

    fun deleteLogLine(line: Int) {
        if (line in flights.indices) {
            flights.removeAt(line)
        }
    }

    fun getMarkdownOutput(roundToNearest: Int): String {
        val outdf = SimpleDateFormat("EEEE yyyy-MM-dd", Locale.ENGLISH)
        val hourmin = SimpleDateFormat("HH:mm", Locale.ENGLISH)

        val ret = StringBuilder()
        ret.append("Glider Log\n")
        ret.append("=======\n\n")
        ret.append(outdf.format(date)).append("\n")
        ret.append("-------\n\n")

        ret.append("Head of Operations:\n  ").append(headOfOperations?.name.orEmpty()).append("\n\n")
        ret.append("Airfield:\n  ").append(airfield.orEmpty()).append("\n\n")

        ret.append("Flights\n")
        ret.append("-------\n\n")

        var n = 1
        for (flight in flights) {
            ret.append(n).append(". ").append(flight.registration).append("\n")
            ret.append("   ").append(flight.pilot?.name.orEmpty()).append(" (")
                .append(flight.pilotType).append(")")
            if (flight.copilot != null) {
                ret.append(", ").append(flight.copilot?.name.orEmpty()).append(" (")
                    .append(flight.copilotType).append(")")
            }
            ret.append("\n")

            ret.append("   Takeoff ").append(formatTime(hourmin, flight.takeoff))
            ret.append(", Tow Release ").append(formatTime(hourmin, flight.towRelease))
            ret.append(", Landing ").append(formatTime(hourmin, flight.landing)).append("\n")

            ret.append("   Tow Duration ").append(flight.getTowDurationStr())
            ret.append(", Flight Duration ").append(flight.getFlightDurationStr()).append("\n")

            if (flight.notes.isNotBlank()) {
                ret.append("   ").append(flight.notes).append("\n")
            }
            n += 1
        }

        val flightTimeList = FlightTimeList()
        for (flight in flights) {
            flightTimeList.add(flight)
        }
        flightTimeList.round(roundToNearest)

        ret.append("\nPlane Summary\n")
        ret.append("-------\n\n")
        ret.append("Summary durations rounded to nearest ").append(roundToNearest).append("m\n\n")
        for ((key, value) in flightTimeList.sortedPlaneSums()) {
            ret.append("* ").append(key.registration).append("\n")
            ret.append("  Flights: ").append(value.flights).append("\n")
            ret.append("  Flight Duration ").append(minutesToDurationStr(value.flightMinutes)).append("\n")
        }

        ret.append("\nPilot Summary\n")
        ret.append("-------\n")
        ret.append("Summary durations rounded to nearest ").append(roundToNearest).append("m\n\n")
        for ((key, value) in flightTimeList.sortedPilotSums()) {
            ret.append("* ").append(key.name.orEmpty()).append(", ")
                .append(key.role).append(" ").append(key.registration).append("\n")
            ret.append("  Flights: ").append(value.flights).append("\n")
            ret.append("  Tow Duration ").append(minutesToDurationStr(value.towMinutes))
            ret.append(", Flight Duration ").append(minutesToDurationStr(value.flightMinutes)).append("\n")
        }

        if (logHasBeenSent) {
            ret.append("\nNote\n-------\n")
            ret.append("Log previously sent ").append(logSentNumberOfTimes)
                .append(" time").append(if (logSentNumberOfTimes > 1) "s" else "").append(".")
        }

        return ret.toString()
    }

    fun getHTMLtableoutput(roundToNearest: Int): String {
        val outdf = SimpleDateFormat("EEEE yyyy-MM-dd", Locale.ENGLISH)
        val hourmin = SimpleDateFormat("HH:mm", Locale.ENGLISH)
        val ret = StringBuilder()

        ret.append("<html><head><meta charset=\"UTF-8\"></head><body>")
        ret.append("<h1>Glider Log</h1>")
        ret.append("<h2>").append(outdf.format(date)).append("</h2>")
        ret.append("<p>Head of Operations:<br/>&nbsp;&nbsp;").append(headOfOperations?.name.orEmpty()).append("</p>")
        ret.append("<p>Airfield:<br/>&nbsp;&nbsp;").append(airfield.orEmpty()).append("</p>")

        ret.append("<h2>Flights</h2><table border=\"1\" cellspacing=\"0\" cellpadding=\"5\">")
        ret.append("<tr><th>#</th><th>Registration</th><th>PIC / Instructor</th><th>Student / Passenger</th><th>Takeoff</th><th>Release</th><th>Landing</th></tr>")
        ret.append("<tr><th></th><th colspan=\"4\">Notes</th><th>Tow Duration</th><th>Flight Duration</th></tr>")

        var c = 0
        for (flight in flights) {
            c += 1
            ret.append("<tr>")
            ret.append("<td>").append(c).append("</td>")
            ret.append("<td>").append(flight.registration).append("</td>")
            ret.append("<td>").append(flight.pilot?.name.orEmpty()).append(" (").append(flight.pilotType).append(")</td>")
            ret.append("<td>")
            if (flight.copilot != null) {
                ret.append(flight.copilot?.name.orEmpty()).append(" (").append(flight.copilotType).append(")")
            }
            ret.append("</td>")
            ret.append("<td>").append(formatTime(hourmin, flight.takeoff)).append("</td>")
            ret.append("<td>").append(formatTime(hourmin, flight.towRelease)).append("</td>")
            ret.append("<td>").append(formatTime(hourmin, flight.landing)).append("</td>")
            ret.append("</tr>")

            ret.append("<tr>")
            ret.append("<td></td><td colspan=\"4\">").append(flight.notes).append("</td>")
            ret.append("<td>").append(flight.getTowDurationStr()).append("</td>")
            ret.append("<td>").append(flight.getFlightDurationStr()).append("</td>")
            ret.append("</tr>")
        }
        ret.append("</table>")

        val flightTimeList = FlightTimeList()
        for (flight in flights) {
            flightTimeList.add(flight)
        }
        flightTimeList.round(roundToNearest)

        ret.append("<h2>Plane Summary</h2>")
        ret.append("<p>Summary durations rounded to nearest ").append(roundToNearest).append("m</p>")
        ret.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\">")
        ret.append("<tr><th>Registration</th><th>Flights</th><th>Flight Duration</th></tr>")
        for ((key, value) in flightTimeList.sortedPlaneSums()) {
            ret.append("<tr><td>").append(key.registration).append("</td><td>")
                .append(value.flights).append("</td><td>")
                .append(minutesToDurationStr(value.flightMinutes)).append("</td></tr>")
        }
        ret.append("</table>")

        ret.append("<h2>Pilot Summary</h2>")
        ret.append("<p>Summary durations rounded to nearest ").append(roundToNearest).append("m</p>")
        ret.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\">")
        ret.append("<tr><th>Name</th><th>Role</th><th>Registration</th><th>Flights</th><th>Tow Duration</th><th>Flight Duration</th></tr>")
        for ((key, value) in flightTimeList.sortedPilotSums()) {
            ret.append("<tr><td>").append(key.name.orEmpty()).append("</td><td>")
                .append(key.role).append("</td><td>")
                .append(key.registration).append("</td><td>")
                .append(value.flights).append("</td><td>")
                .append(minutesToDurationStr(value.towMinutes)).append("</td><td>")
                .append(minutesToDurationStr(value.flightMinutes)).append("</td></tr>")
        }
        ret.append("</table>")

        if (logHasBeenSent) {
            ret.append("<h2>Note</h2><p>Log previously sent ").append(logSentNumberOfTimes)
                .append(" time").append(if (logSentNumberOfTimes > 1) "s" else "")
                .append(".</p>")
        }

        ret.append("</body></html>")
        return ret.toString()
    }

    fun getJSONOutput(): String {
        val outdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        val flightsArray = JSONArray()

        for ((index, flight) in flights.withIndex()) {
            val flightObject = JSONObject()
                .put("flightnum", index + 1)
                .put("registration", flight.registration)
                .put("pilot", flight.pilot?.name.orEmpty())
                .put("pilotRole", flight.pilotType.toString())
                .put("takeoff", formatTime(time, flight.takeoff))
                .put("tow_release", formatTime(time, flight.towRelease))
                .put("landing", formatTime(time, flight.landing))
                .put("towDuration", flight.getTowDurationStr())
                .put("flightDuration", flight.getFlightDurationStr())
                .put("notes", flight.notes)
            flightsArray.put(flightObject)
        }

        val payload = JSONObject()
            .put("headOfOperations", headOfOperations?.name.orEmpty())
            .put("airfield", airfield.orEmpty())
            .put("date", outdf.format(date))
            .put("sent_times", logSentNumberOfTimes.toString())
            .put("flights", flightsArray)

        return payload.toString()
    }

    private fun formatTime(format: SimpleDateFormat, value: Date?): String {
        return if (value == null) "" else format.format(value)
    }

    private fun minutesToDurationStr(minutes: Long): String {
        val hours = minutes / 60
        val minutesInHour = minutes % 60
        return "${hours}h ${minutesInHour}m"
    }
}

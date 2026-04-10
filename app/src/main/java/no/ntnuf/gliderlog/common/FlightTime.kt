package no.ntnuf.gliderlog.common

class FlightTime {
    var landed: Boolean = true
    var flights: Int = 0
    var flightMinutes: Long = 0
    var studentMinutes: Long = 0
    var instructorMinutes: Long = 0
    var towMinutes: Long = 0

    private fun round(value: Long, roundToNearest: Long): Long {
        val isOdd = roundToNearest % 2L == 1L
        var roundUpIfGteq = roundToNearest / 2L
        if (isOdd) {
            roundUpIfGteq += 1L
        }

        val remainder = value % roundToNearest
        var rounded = value / roundToNearest
        if (remainder >= roundUpIfGteq) {
            rounded += 1L
        }
        return rounded * roundToNearest
    }

    fun roundAll(roundToNearest: Int) {
        val step = roundToNearest.toLong()
        flightMinutes = round(flightMinutes, step)
        studentMinutes = round(studentMinutes, step)
        instructorMinutes = round(instructorMinutes, step)
        towMinutes = round(towMinutes, step)
    }
}


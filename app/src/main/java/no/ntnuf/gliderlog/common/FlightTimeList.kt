package no.ntnuf.gliderlog.common

class FlightTimeList {
    val pilotSums: LinkedHashMap<NameRegistrationRole, FlightTime> = linkedMapOf()
    val planeSums: LinkedHashMap<NameRegistrationRole, FlightTime> = linkedMapOf()

    fun add(flight: FlightEntry) {
        val pilotName = flight.pilot?.name ?: return
        val pilotRole = flight.pilotType ?: return

        val pilotKey = NameRegistrationRole(pilotName, flight.registration, pilotRole, false)
        val pilotTotal = pilotSums.getOrPut(pilotKey) { FlightTime() }
        updateTotals(pilotTotal, flight, pilotRole)

        val copilotName = flight.copilot?.name
        val copilotRole = flight.copilotType
        if (copilotName != null && copilotRole != null) {
            val copilotKey = NameRegistrationRole(copilotName, flight.registration, copilotRole, false)
            val copilotTotal = pilotSums.getOrPut(copilotKey) { FlightTime() }
            updateTotals(copilotTotal, flight, copilotRole)
        }

        val planeKey = NameRegistrationRole(null, flight.registration, null, true)
        val planeTotal = planeSums.getOrPut(planeKey) { FlightTime() }
        if (flight.status == FlightStatus.LANDED) {
            planeTotal.flights += 1
            planeTotal.flightMinutes += flight.getFlightDurationMinutes()
            planeTotal.towMinutes += flight.getTowDurationMinutes()
        } else {
            planeTotal.landed = false
        }
    }

    fun sortedPilotSums(): List<Pair<NameRegistrationRole, FlightTime>> {
        return pilotSums.entries
            .sortedBy { it.key.name ?: "" }
            .map { it.key to it.value }
    }

    fun sortedPlaneSums(): List<Pair<NameRegistrationRole, FlightTime>> {
        return planeSums.entries
            .sortedBy { it.key.registration }
            .map { it.key to it.value }
    }

    fun round(roundToNearest: Int) {
        pilotSums.values.forEach { it.roundAll(roundToNearest) }
        planeSums.values.forEach { it.roundAll(roundToNearest) }
    }

    private fun updateTotals(total: FlightTime, flight: FlightEntry, role: PilotType) {
        if (flight.status == FlightStatus.LANDED) {
            total.flights += 1
            total.flightMinutes += flight.getFlightDurationMinutes()
            total.towMinutes += flight.getTowDurationMinutes()
            if (role == PilotType.INSTRUCTOR) {
                total.instructorMinutes += flight.getFlightDurationMinutes()
            }
            if (role == PilotType.STUDENT) {
                total.studentMinutes += flight.getFlightDurationMinutes()
            }
        } else {
            total.landed = false
        }
    }
}


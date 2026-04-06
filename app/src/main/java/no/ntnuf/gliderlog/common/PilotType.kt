package no.ntnuf.gliderlog.common

enum class PilotType {
    PIC,
    INSTRUCTOR,
    STUDENT,
    PASSENGER;

    override fun toString(): String {
        return when (this) {
            PIC -> "PIC"
            INSTRUCTOR -> "Instructor"
            STUDENT -> "Student"
            PASSENGER -> "Passenger"
        }
    }
}


package com.example.a4bus_motorista_x3


class DriverTripConfig(
    val tripId: String? = null,

    val vehicleId: String? = null,

    var vehicleLocation: Waypoint.Point? = null,

    val waypoints: List<Waypoint> = listOf(),
) {
    fun getWaypoint(index: Int): Waypoint? {
        return if (index < 0 || index == waypoints.size) {
            null
        } else {
            waypoints[index]
        }
    }
}

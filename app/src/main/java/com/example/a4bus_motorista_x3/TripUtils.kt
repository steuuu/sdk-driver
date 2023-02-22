package com.example.a4bus_motorista_x3

object TripUtils {
    private const val PICKUP_WAYPOINT_TYPE = "PICKUP_WAYPOINT_TYPE"
    const val INTERMEDIATE_DESTINATION_WAYPOINT_TYPE = "INTERMEDIATE_DESTINATION_WAYPOINT_TYPE"
    private const val DROP_OFF_WAYPOINT_TYPE = "DROP_OFF_WAYPOINT_TYPE"
    const val SHARED_TRIP_TYPE = "SHARED"
    const val EXCLUSIVE_TRIP_TYPE = "EXCLUSIVE"

    private val ENROUTE_TRIP_STATUSES =
        listOf(
            TripStatus.ENROUTE_TO_PICKUP,
            TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
            TripStatus.ENROUTE_TO_DROPOFF,
        )

    private val ARRIVED_TRIP_STATUSES =
        listOf(
            TripStatus.ARRIVED_AT_PICKUP,
            TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION,
            TripStatus.COMPLETE
        )

    fun getInitialTripState(tripId: String): TripState = TripState(tripId, TripStatus.NEW)

    fun getNextTripState(tripState: TripState, nextWaypointOfTrip: Waypoint?): TripState {
        val nextStatus = getNextTripStatus(tripState, nextWaypointOfTrip)

        val intermediateDestinationIndex =
            if (nextStatus == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION) {
                tripState.intermediateDestinationIndex + 1
            } else tripState.intermediateDestinationIndex

        return TripState(tripState.tripId, nextStatus, intermediateDestinationIndex)
    }

    fun getEnrouteStateForWaypoint(currentState: TripState, nextWaypoint: Waypoint): TripState =
        if (currentState.tripStatus == TripStatus.COMPLETE) currentState
        else {
            when (nextWaypoint.waypointType) {
                PICKUP_WAYPOINT_TYPE -> TripState(currentState.tripId, TripStatus.ENROUTE_TO_PICKUP)
                INTERMEDIATE_DESTINATION_WAYPOINT_TYPE ->
                    TripState(
                        currentState.tripId,
                        TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
                        currentState.intermediateDestinationIndex + 1
                    )
                DROP_OFF_WAYPOINT_TYPE -> TripState(
                    currentState.tripId,
                    TripStatus.ENROUTE_TO_DROPOFF
                )
                else -> throw IllegalStateException("Invalid waypoint type")
            }
        }

    fun isTripStatusEnroute(tripStatus: TripStatus) = tripStatus in ENROUTE_TRIP_STATUSES

    fun isTripStatusArrived(tripStatus: TripStatus) = tripStatus in ARRIVED_TRIP_STATUSES

    private fun getNextTripStatus(tripState: TripState, nextWaypointOfTrip: Waypoint?): TripStatus =
        when (tripState.tripStatus) {
            TripStatus.NEW -> TripStatus.ENROUTE_TO_PICKUP
            TripStatus.ENROUTE_TO_PICKUP -> TripStatus.ARRIVED_AT_PICKUP
            TripStatus.ARRIVED_AT_PICKUP -> {
                if (nextWaypointOfTrip?.waypointType == INTERMEDIATE_DESTINATION_WAYPOINT_TYPE) {
                    TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION
                } else {
                    TripStatus.ENROUTE_TO_DROPOFF
                }
            }
            TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION ->
                TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION
            TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION -> {
                if (nextWaypointOfTrip?.waypointType == DROP_OFF_WAYPOINT_TYPE) {
                    TripStatus.ENROUTE_TO_DROPOFF
                } else {
                    TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION
                }
            }
            TripStatus.ENROUTE_TO_DROPOFF -> TripStatus.COMPLETE
            else -> TripStatus.UNKNOWN_TRIP_STATUS
        }
}

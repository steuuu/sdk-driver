package com.example.a4bus_motorista_x3

enum class TripStatus {
    UNKNOWN_TRIP_STATUS,
    NEW,
    ENROUTE_TO_PICKUP,
    ARRIVED_AT_PICKUP,
    ARRIVED_AT_INTERMEDIATE_DESTINATION,
    ENROUTE_TO_INTERMEDIATE_DESTINATION,
    ENROUTE_TO_DROPOFF,
    COMPLETE,
    CANCELED;
}

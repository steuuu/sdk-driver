package com.example.a4bus_motorista_x3

data class TripState(
    val tripId: String,
    val tripStatus: TripStatus,
    val intermediateDestinationIndex: Int = - 1,
)

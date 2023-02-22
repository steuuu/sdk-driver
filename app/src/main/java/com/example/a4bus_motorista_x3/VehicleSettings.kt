package com.example.a4bus_motorista_x3

import com.example.a4bus_motorista_x3.TripUtils.EXCLUSIVE_TRIP_TYPE

data class VehicleSettings(
    val vehicleId: String = "",
    val backToBackEnabled: Boolean = false,
    val maximumCapacity: Int = 5,
    val supportedTripTypes: List<String> = listOf(EXCLUSIVE_TRIP_TYPE),
)

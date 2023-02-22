package com.example.a4bus_motorista_x3

import com.google.gson.annotations.SerializedName

class VehicleModel(
    @SerializedName("name") val name: String = "",
    @SerializedName("vehicleState") val vehicleState: String = "",
    @SerializedName("waypoints") val waypoints: List<Waypoint> = listOf(),
    @SerializedName("currentTripsIds") val currentTripsIds: List<String> = listOf(),
    @SerializedName("backToBackEnabled") val backToBackEnabled: Boolean = false,
    @SerializedName("supportedTripTypes") val supportedTripTypes: List<String> = listOf(),
    @SerializedName("maximumCapacity") val maximumCapacity: Int = 5,
)

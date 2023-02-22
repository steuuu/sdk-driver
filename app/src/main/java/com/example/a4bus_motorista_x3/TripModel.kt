package com.example.a4bus_motorista_x3

import com.google.gson.annotations.SerializedName

class TripModel(
    @SerializedName("name") val name: String? = null,
    @SerializedName("tripStatus") private val tripStatus: String? = null,
    @SerializedName("waypoints") val waypoints: List<Waypoint>
)

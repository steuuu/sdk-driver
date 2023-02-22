package com.example.a4bus_motorista_x3

import com.google.gson.annotations.SerializedName

class Waypoint(

    @SerializedName("tripId") val tripId: String = "",
    @SerializedName("location") val location: Location? = null,
    @SerializedName("waypointType") val waypointType: String = "",
) {
    class Location(
        @SerializedName(value = "point") val point: Point? = null
    )

    class Point(
        @SerializedName(value = "latitude") val latitude: Double = 0.0,
        @SerializedName(value = "longitude") val longitude: Double = 0.0,
    )
}

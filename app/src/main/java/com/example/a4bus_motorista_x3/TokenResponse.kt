package com.example.a4bus_motorista_x3

import com.google.gson.annotations.SerializedName
import org.joda.time.Instant

class TokenResponse(
    @SerializedName("jwt") val token: String? = null,
    @SerializedName("creationTimestamp") private val creationTimestampMs: Long = 0,
    @SerializedName("expirationTimestamp") private val expirationTimestampMs: Long = 0
) {
    val creationTimestamp: Instant
        get() = Instant(creationTimestampMs)
    val expirationTimestamp: Instant
        get() = Instant(expirationTimestampMs)
}
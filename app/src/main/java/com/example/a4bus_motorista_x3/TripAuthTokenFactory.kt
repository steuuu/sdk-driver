package com.example.a4bus_motorista_x3

import kotlinx.coroutines.runBlocking

internal class TripAuthTokenFactory(private val providerService: LocalProviderService) :
    AuthTokenContext.AuthTokenFactory {
    private var token: String? = null
    private var expiryTimeMs: Long = 0
    private var vehicleId: String? = null

    override fun getToken(context: AuthTokenContext): String {
        val vehicleId = context.vehicleId !!
        if (System.currentTimeMillis() > expiryTimeMs || vehicleId != this.vehicleId) {
            fetchNewToken(vehicleId)
        }
        return token !!
    }

    private fun fetchNewToken(vehicleId: String) = runBlocking {
        try {
            val tokenResponse = providerService.fetchAuthToken(vehicleId)
            token = tokenResponse.token !!

            val tenMinutesInMillis = (10 * 60 * 1000).toLong()
            expiryTimeMs = tokenResponse.expirationTimestamp.millis - tenMinutesInMillis
            this@TripAuthTokenFactory.vehicleId = vehicleId
        } catch (e: Exception) {
            throw RuntimeException("Could not get auth token", e)
        }
    }
}

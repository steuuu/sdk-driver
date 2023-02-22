package com.example.a4bus_motorista_x3

import android.util.Log
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.guava.GuavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@Suppress("UnstableApiUsage")
class LocalProviderService(
    private val restProvider: RestProvider,
) {
    suspend fun fetchAuthToken(vehicleId: String): TokenResponse =
        restProvider.getAuthToken(vehicleId)

    suspend fun updateTripStatus(tripState: TripState): TripModel {
        val (tripId, tripStatus, intermediateDestinationIndex) = tripState
        val tripStatusString = tripStatus.toString()

        return if (tripStatus == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION) {
            updateTrip(tripId, TripUpdateBody(tripStatusString, intermediateDestinationIndex))
        } else {
            updateTrip(tripId, TripUpdateBody(tripStatusString))
        }
    }

    suspend fun registerVehicle(vehicleId: String): VehicleModel {
        return try {
            restProvider.getVehicle(vehicleId)
        } catch (httpException: HttpException) {
            if (isNotFoundHttpException(httpException)) {
                restProvider.createVehicle(
                    VehicleSettings(
                        vehicleId = vehicleId,
                    )
                )
            } else {
                throw httpException
            }
        }
    }

    suspend fun createOrUpdateVehicle(vehicleSettings: VehicleSettings): VehicleModel {
        return try {
            restProvider.updateVehicle(vehicleSettings.vehicleId, vehicleSettings)
        } catch (httpException: HttpException) {
            if (isNotFoundHttpException(httpException)) {
                restProvider.createVehicle(vehicleSettings)
            } else {
                throw httpException
            }
        }
    }

    fun getVehicleModelFlow(vehicleId: String): Flow<VehicleModel> =
        flow {
            while (true) {
                emit(restProvider.getVehicle(vehicleId))
                delay(3.seconds)
            }
        }
            .onStart { Log.i(TAG, "Iniciando votação para $vehicleId") }
            .onCompletion { Log.i(TAG, "Encerrando votação para $vehicleId") }

    private suspend fun updateTrip(tripId: String, updateBody: TripUpdateBody): TripModel =
        restProvider.updateTrip(tripId, updateBody).also {
            Log.i(TAG, "Viajem atualizada com sucesso $tripId com $updateBody.")
        }

    companion object {
        private const val TAG = "LocalProviderService"

        private fun isNotFoundHttpException(httpException: HttpException) =
            httpException.code() == 404

        fun createRestProvider(baseUrl: String): RestProvider {
            val retrofit =
                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addCallAdapterFactory(GuavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            return retrofit.create(RestProvider::class.java)
        }
    }
}

package com.example.a4bus_motorista_x3

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.MoreExecutors
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Suppress("UnstableApiUsage")
class VehicleController(
    private val navigator: Navigator,
    private val executor: ExecutorService,
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val providerService: LocalProviderService,
    private val localSettings: LocalSettings,
) {
    private val authTokenFactory: TripAuthTokenFactory = TripAuthTokenFactory(providerService)
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    private val sequentialExecutor: Executor = MoreExecutors.newSequentialExecutor(executor)
    private val vehicleSimulator: VehicleSimulator =
        VehicleSimulator(navigator.simulator, localSettings)
    private var presenterRef = WeakReference<Presenter>(null)
    private lateinit var vehicleReporter: RidesharingVehicleReporter
    private var tripState: TripState? = null
    private var vehicleStateJob: Job? = null
    lateinit var vehicleSettings: VehicleSettings

    private var currentWaypoint: Waypoint? = null
    private var nextWaypoint: Waypoint? = null
    private var nextWaypointOfCurrentTrip: Waypoint? = null

    private var waypoints: List<Waypoint> = emptyList()
    private var matchedTripIds: List<String> = emptyList()

    private val tripStates: MutableMap<String, TripState> = mutableMapOf()

    fun onVehicleStateUpdate(vehicleModel: VehicleModel) {
        Log.i(TAG, "onVehicleStateUpdate")

        waypoints = vehicleModel.waypoints
        matchedTripIds = vehicleModel.currentTripsIds
        currentWaypoint = null
        nextWaypoint = null
        nextWaypointOfCurrentTrip = null

        if (! waypoints.isEmpty()) {
            var updatedNextWaypointOfCurrentTrip: Waypoint? = null

            waypoints.forEachIndexed { index, waypoint ->
                Log.i(TAG, "$waypoint.tripId $waypoint.waypointType")

                val isTripForWaypointAccepted = tripStates.containsKey(waypoint.tripId)

                if (! isTripForWaypointAccepted) {
                    acceptTrip(waypoint)

                    if (currentWaypoint == null) {
                        setWaypointDestination(waypoint)
                    }
                }

                if (index == 0) {
                    currentWaypoint = waypoint
                } else {
                    if (index == 1) {
                        nextWaypoint = waypoint
                    }

                    if (
                        updatedNextWaypointOfCurrentTrip == null && currentWaypoint?.tripId == waypoint.tripId
                    ) {
                        updatedNextWaypointOfCurrentTrip = waypoint
                    }
                }
            }

            nextWaypointOfCurrentTrip = updatedNextWaypointOfCurrentTrip
        } else {
            stopJourneySharing()
        }

        updateUiForWaypoint(currentWaypoint)
    }

    suspend fun initVehicleAndReporter(application: Application) {
        val vehicleModel = providerService.registerVehicle(localSettings.getVehicleId())

        initializeApi(application, vehicleModel)
    }

    suspend fun updateVehicleSettings(application: Application, vehicleSettings: VehicleSettings) {
        val vehicleModel = providerService.createOrUpdateVehicle(vehicleSettings)
        localSettings.saveVehicleId(extractVehicleId(vehicleModel.name))

        initializeApi(application, vehicleModel)
    }


    @Suppress("UnstableApiUsage")
    fun initializeApi(application: Application, vehicleModel: VehicleModel) {

        if (RidesharingDriverApi.getInstance() != null) {
            RidesharingDriverApi.clearInstance()
        }

        vehicleReporter =
            RidesharingDriverApi.createInstance(
                DriverContext.builder(application)
                    .setNavigator(navigator)
                    .setProviderId(ProviderUtils.getProviderId(application))
                    .setVehicleId(localSettings.getVehicleId())
                    .setAuthTokenFactory(authTokenFactory)
                    .setRoadSnappedLocationProvider(
                        NavigationApi.getRoadSnappedLocationProvider(application)
                    )
                    .setStatusListener { statusLevel: StatusLevel, statusCode: StatusCode, statusMsg: String
                        ->
                        logLocationUpdate(statusLevel, statusCode, statusMsg)
                    }
                    .build()
            )
                .ridesharingVehicleReporter

        vehicleSettings =
            vehicleModel.let {
                VehicleSettings(
                    vehicleId = extractVehicleId(it.name) !!,
                    backToBackEnabled = it.backToBackEnabled,
                    supportedTripTypes = it.supportedTripTypes,
                    maximumCapacity = it.maximumCapacity,
                )
            }

        setVehicleOnline()
        startVehiclePeriodicUpdate()
    }

    private fun acceptTrip(waypoint: Waypoint) =
        coroutineScope.launch {
            updateTripStatusInServer(TripUtils.getInitialTripState(waypoint.tripId))
        }

    fun cleanUp() {
        stopVehiclePeriodicUpdate()

        RidesharingDriverApi.clearInstance()
    }

    private fun startVehiclePeriodicUpdate() {
        stopVehiclePeriodicUpdate()

        vehicleStateJob =
            providerService
                .getVehicleModelFlow(localSettings.getVehicleId())
                .onEach(::onVehicleStateUpdate)
                .launchIn(coroutineScope)
    }

    private fun stopVehiclePeriodicUpdate() {
        coroutineScope.launch {
            vehicleStateJob?.cancelAndJoin()
            vehicleStateJob = null
        }
    }

    private fun setVehicleOnline() {
        vehicleReporter.setLocationReportingInterval(
            DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        )

        vehicleReporter.enableLocationTracking()
        vehicleReporter.setVehicleState(VehicleState.ONLINE)
    }

    fun processNextState() {
        currentWaypoint?.let {

            sequentialExecutor.execute {
                val previousTripState = tripStates[it.tripId] ?: return@execute

                val updatedTripState =
                    TripUtils.getNextTripState(previousTripState, nextWaypointOfCurrentTrip)

                coroutineScope.launch {
                    updateTripStatusInServer(updatedTripState)

                    if (TripUtils.isTripStatusArrived(updatedTripState.tripStatus)) {
                        advanceNextWaypointOnArrival()
                    }
                }

                updateNavigationForWaypoints(updatedTripState)

                Log.i(
                    TAG,
                    "Estado da viagem anterior: $previousTripState.tripStatus " +
                            "Status atual da viagem: $updatedTripState.tripStatus",
                )
            }
        }
    }

    private fun updateNavigationForWaypoints(tripState: TripState) {
        when (tripState.tripStatus) {
            TripStatus.ENROUTE_TO_PICKUP -> {
                currentWaypoint?.let {
                    startJourneySharing()
                    navigateToWaypoint(it)
                }
            }
            TripStatus.ARRIVED_AT_PICKUP,
            TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION,
            TripStatus.COMPLETE -> nextWaypoint?.let { navigateToWaypoint(it) }
            else -> {}
        }
    }

    private suspend fun advanceNextWaypointOnArrival() {
        nextWaypoint?.let {
            val updatedStateForNextWaypoint =
                TripUtils.getEnrouteStateForWaypoint(tripStates[it.tripId] ?: return@let, it)

            updateTripStatusInServer(updatedStateForNextWaypoint)
        }
    }

    private fun updateUiForWaypoint(waypoint: Waypoint?) {
        val presenter = presenterRef.get() ?: return

        mainExecutor.execute {
            if (waypoint == null) {
                presenter.showTripId(NO_TRIP_ID)
                presenter.showTripStatus(TripStatus.UNKNOWN_TRIP_STATUS)
                presenter.showMatchedTripIds(emptyList())
            } else {
                tripStates[waypoint.tripId]?.let {
                    presenter.showTripId(it.tripId)
                    presenter.showTripStatus(it.tripStatus)
                    presenter.showMatchedTripIds(matchedTripIds)
                }
            }
        }
    }

    private suspend fun updateTripStatusInServer(updatedState: TripState) {
        tripStates[updatedState.tripId] = updatedState

        if (updatedState.tripStatus == TripStatus.UNKNOWN_TRIP_STATUS) {
            return
        }

        providerService.updateTripStatus(updatedState)
    }

    fun setPresenter(presenter: Presenter?) {
        presenterRef = WeakReference(presenter)
    }

    private fun startJourneySharing() {
        vehicleReporter.setLocationReportingInterval(
            JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        )
    }

    private fun stopJourneySharing() {
        vehicleReporter.setLocationReportingInterval(
            DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        )

        navigator.stopGuidance()
        navigator.clearDestinations()
    }


    private fun setWaypointDestination(waypoint: Waypoint) {
        val locationPoint = waypoint.location?.point ?: return

        val destinationWaypoint: NavigationWaypoint =
            NavigationWaypoint.builder()
                .setLatLng(locationPoint.latitude, locationPoint.longitude)
                .setTitle(waypoint.waypointType)
                .build()


        navigator.setDestination(destinationWaypoint)
    }

    private fun navigateToWaypoint(waypoint: Waypoint?) {
        if (waypoint == null) {
            navigator.stopGuidance()
            return
        }

        val locationPoint = waypoint.location?.point ?: return

        val pendingRoute =
            navigator.setDestination(
                NavigationWaypoint.builder()
                    .setLatLng(locationPoint.latitude, locationPoint.longitude)
                    .setTitle(waypoint.waypointType)
                    .build()
            )

        pendingRoute.setOnResultListener(
            object : ListenableResultFuture.OnResultListener<Navigator.RouteStatus> {
                override fun onResult(code: Navigator.RouteStatus) {
                    when (code) {
                        Navigator.RouteStatus.OK -> {
                            navigator.startGuidance()
                            vehicleSimulator.start(SIMULATOR_SPEED_MULTIPLIER)
                        }
                        Navigator.RouteStatus.NO_ROUTE_FOUND,
                        Navigator.RouteStatus.NETWORK_ERROR,
                        Navigator.RouteStatus.ROUTE_CANCELED -> {
                            Log.e(TAG, "Failed to set a route to next waypoint")
                        }
                        else -> {}
                    }
                }
            }
        )
    }

    fun isNextCurrentTripWaypointIntermediate() =
        nextWaypointOfCurrentTrip?.waypointType == TripUtils.INTERMEDIATE_DESTINATION_WAYPOINT_TYPE

    companion object {

        private const val SIMULATOR_SPEED_MULTIPLIER = 5.0f

        private const val JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS: Long = 1

        private val VEHICLE_NAME_FORMAT = Pattern.compile("providers/(.*)/vehicles/(.*)")
        const val NO_TRIP_ID = ""

        private const val VEHICLE_ID_INDEX = 2
        private const val ON_TRIP_FINISHED_DELAY_SECONDS = 5
        private const val TAG = "VehicleController"

        private const val DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS: Long = 10
        private fun logLocationUpdate(
            statusLevel: StatusLevel,
            statusCode: StatusCode,
            statusMsg: String
        ) {
            val message = "Location update: $statusLevel $statusCode: $statusMsg"

            if (statusLevel == StatusLevel.ERROR) {
                Log.e(TAG, message)
            } else {
                Log.i(TAG, message)
            }
        }

        private fun extractVehicleId(vehicleName: String): String? {
            val matches = VEHICLE_NAME_FORMAT.matcher(vehicleName)
            return if (matches.matches()) {
                matches.group(VEHICLE_ID_INDEX)
            } else vehicleName
        }
    }
}

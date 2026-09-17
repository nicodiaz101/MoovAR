package com.moovar.android.core.network.gtfsrt

import com.google.transit.realtime.GtfsRealtime.FeedMessage
import com.moovar.android.core.domain.model.SubteArrival
import com.moovar.android.core.domain.model.VehicleLocation

object GtfsRtMapper {

    fun mapTripUpdates(feed: FeedMessage): Map<String, List<SubteArrival>> =
        feed.entityList
            .filter { it.hasTripUpdate() }
            .flatMap { entity ->
                val tripId = entity.tripUpdate.trip.tripId
                entity.tripUpdate.stopTimeUpdateList.map { update ->
                    SubteArrival(
                        stopId = update.stopId,
                        tripId = tripId,
                        arrivalDelay = update.arrival.delay,
                        arrivalTime = update.arrival.time,
                        departureDelay = update.departure.delay,
                        scheduleRelationship = update.scheduleRelationship.name
                    )
                }
            }
            .groupBy { it.stopId }

    fun mapVehiclePositions(feed: FeedMessage): List<VehicleLocation> =
        feed.entityList
            .filter { it.hasVehicle() }
            .map { entity ->
                val v = entity.vehicle
                VehicleLocation(
                    vehicleId = v.vehicle.id,
                    tripId = v.trip.tripId,
                    stopId = v.stopId,
                    currentStatus = v.currentStatus.name,
                    hasCoordinates = false,
                    latitude = null,
                    longitude = null
                )
            }
}

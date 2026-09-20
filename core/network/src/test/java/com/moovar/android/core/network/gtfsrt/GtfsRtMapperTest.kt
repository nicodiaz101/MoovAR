package com.moovar.android.core.network.gtfsrt

import com.google.transit.realtime.GtfsRealtime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GtfsRtMapperTest {

    @Test
    fun `empty entities in FeedMessage do not crash mapper`() {
        val feed = GtfsRealtime.FeedMessage.newBuilder()
            .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
            .build()

        val tripUpdates = GtfsRtMapper.mapTripUpdates(feed)
        val vehiclePositions = GtfsRtMapper.mapVehiclePositions(feed)

        assertTrue(tripUpdates.isEmpty())
        assertTrue(vehiclePositions.isEmpty())
    }

    @Test
    fun `mapVehiclePositions always sets hasCoordinates to false and coordinates to null`() {
        val vehicle = GtfsRealtime.VehiclePosition.newBuilder()
            .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("train_101"))
            .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("trip_500"))
            .setStopId("stn_constitucion")
            .setCurrentStatus(GtfsRealtime.VehiclePosition.VehicleStopStatus.INCOMING_AT)
            .build()

        val entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("entity_1")
            .setVehicle(vehicle)
            .build()

        val feed = GtfsRealtime.FeedMessage.newBuilder()
            .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
            .addEntity(entity)
            .build()

        val locations = GtfsRtMapper.mapVehiclePositions(feed)

        assertEquals(1, locations.size)
        val loc = locations[0]
        assertEquals("train_101", loc.vehicleId)
        assertEquals("trip_500", loc.tripId)
        assertEquals("stn_constitucion", loc.stopId)
        assertFalse(loc.hasCoordinates)
        assertEquals(null, loc.latitude)
        assertEquals(null, loc.longitude)
    }

    @Test
    fun `mapTripUpdates groups correctly by stopId`() {
        val stopUpdate1 = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
            .setStopId("stn_peru")
            .setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(1000L).setDelay(30))
            .build()

        val stopUpdate2 = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
            .setStopId("stn_peru")
            .setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(1500L).setDelay(60))
            .build()

        val stopUpdate3 = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
            .setStopId("stn_pza_mayo")
            .setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(2000L).setDelay(0))
            .build()

        val tripUpdate = GtfsRealtime.TripUpdate.newBuilder()
            .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("trip_A1"))
            .addStopTimeUpdate(stopUpdate1)
            .addStopTimeUpdate(stopUpdate2)
            .addStopTimeUpdate(stopUpdate3)
            .build()

        val entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("entity_tu")
            .setTripUpdate(tripUpdate)
            .build()

        val feed = GtfsRealtime.FeedMessage.newBuilder()
            .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
            .addEntity(entity)
            .build()

        val grouped = GtfsRtMapper.mapTripUpdates(feed)

        assertEquals(2, grouped.keys.size)
        assertTrue(grouped.containsKey("stn_peru"))
        assertTrue(grouped.containsKey("stn_pza_mayo"))
        assertEquals(2, grouped["stn_peru"]?.size)
        assertEquals(1, grouped["stn_pza_mayo"]?.size)
    }
}

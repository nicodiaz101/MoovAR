package com.moovar.android.core.network.gtfsrt

import com.google.transit.realtime.GtfsRealtime.FeedMessage
import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.SubteArrival
import com.moovar.android.core.domain.model.VehicleLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SubteRtRepository @Inject constructor(
    private val dataSource: GtfsRtDataSource
) {
    fun observeSubteData(): Flow<Result<Pair<Map<String, List<SubteArrival>>, List<VehicleLocation>>>> = flow {
        while (true) {
            try {
                val feed = dataSource.fetchFeed()
                val tripUpdates = GtfsRtMapper.mapTripUpdates(feed)
                val vehiclePositions = GtfsRtMapper.mapVehiclePositions(feed)
                emit(Result.Success(tripUpdates to vehiclePositions))
            } catch (e: Exception) {
                emit(Result.Error(e.message ?: "Unknown error"))
            }
            delay(30_000)
        }
    }
}

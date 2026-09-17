package com.moovar.android.core.network.gtfsrt

import com.google.transit.realtime.GtfsRealtime.FeedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class GtfsRtDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @GtfsRtEndpointUrl private val endpointUrl: String
) {
    suspend fun fetchFeed(): FeedMessage = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(endpointUrl)
            .header("Accept", "application/x-protobuf")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("NetworkException: ${response.code}")
            val bytes = response.body?.bytes()
                ?: throw IllegalStateException("EmptyBodyException: GTFS-RT feed vacío")
            FeedMessage.parseFrom(bytes)
        }
    }
}

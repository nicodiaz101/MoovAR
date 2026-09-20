package com.moovar.android.data.repository

import com.moovar.android.core.domain.model.JourneyDetails
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyDetailsCache @Inject constructor() {
    private val cache = ConcurrentHashMap<String, JourneyDetails>()

    fun put(key: String, details: JourneyDetails) {
        cache[key] = details
    }

    fun get(key: String): JourneyDetails? = cache[key]

    fun clear() {
        cache.clear()
    }
}

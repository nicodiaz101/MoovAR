package com.moovar.android.data.repository

import android.util.LruCache
import com.moovar.android.core.domain.model.JourneyDetails
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyDetailsCache @Inject constructor() {
    private val cache = LruCache<String, JourneyDetails>(50)

    fun put(key: String, details: JourneyDetails) {
        cache.put(key, details)
    }

    fun get(key: String): JourneyDetails? = cache.get(key)

    fun clear() {
        cache.evictAll()
    }
}

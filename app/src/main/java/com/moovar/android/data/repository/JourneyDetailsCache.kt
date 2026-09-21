package com.moovar.android.data.repository

import com.moovar.android.core.domain.model.JourneyDetails
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyDetailsCache @Inject constructor() {
    private val maxSize = 50
    private val cache: MutableMap<String, JourneyDetails> = Collections.synchronizedMap(
        object : LinkedHashMap<String, JourneyDetails>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, JourneyDetails>?): Boolean {
                return size > maxSize
            }
        }
    )

    fun put(key: String, details: JourneyDetails) {
        cache[key] = details
    }

    fun get(key: String): JourneyDetails? = cache[key]

    fun clear() {
        cache.clear()
    }
}

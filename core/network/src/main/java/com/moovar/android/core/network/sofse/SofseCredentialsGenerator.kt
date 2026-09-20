package com.moovar.android.core.network.sofse

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Generates the official SOFSE mobile app authorization credentials.
 * Implements the proprietary multi-stage obfuscated token handshake required by SOFSE v1 API.
 */
object SofseCredentialsGenerator {

    fun generateCredentials(): Pair<String, String> {
        val dateStr = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "sofse"
        val username = Base64.getEncoder().encodeToString(dateStr.toByteArray(StandardCharsets.UTF_8))

        // Step 1: Base64 of username
        val b64Step1 = Base64.getEncoder().encodeToString(username.toByteArray(StandardCharsets.UTF_8))
        // Step 2: Cipher round 0
        val cipherStep1 = b64Step1
            .replace("a", "#t")
            .replace("e", "#x")
            .replace("i", "#f")
            .replace("o", "#l")
            .replace("u", "#7")
            .replace("=", "#g")
        // Step 3: Reverse
        val revStep1 = cipherStep1.reversed()

        // Step 4: Base64
        val b64Step2 = Base64.getEncoder().encodeToString(revStep1.toByteArray(StandardCharsets.UTF_8))
        // Step 5: Cipher round 1
        val cipherStep2 = b64Step2
            .replace("a", "#j")
            .replace("e", "#p")
            .replace("i", "#w")
            .replace("o", "#8")
            .replace("u", "#0")
            .replace("=", "#v")
        // Step 6: Reverse
        val revStep2 = cipherStep2.reversed()

        // Step 7: URL encode
        val password = URLEncoder.encode(revStep2, "UTF-8")

        return Pair(username, password)
    }
}

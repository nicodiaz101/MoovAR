package com.moovar.android.core.network.sofse

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SofseCredentialsGeneratorTest {

    @Test
    fun generateCredentials_returnsValidNonEmptyCredentials() {
        val (username, password) = SofseCredentialsGenerator.generateCredentials()
        assertNotNull(username)
        assertNotNull(password)
        assertTrue(username.isNotEmpty())
        assertTrue(password.isNotEmpty())
        // Decoded username must end with "sofse"
        val decodedUsername = String(java.util.Base64.getDecoder().decode(username))
        assertTrue(decodedUsername.endsWith("sofse"))
    }
}

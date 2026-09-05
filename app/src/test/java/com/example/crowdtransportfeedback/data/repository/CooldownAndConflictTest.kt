package com.example.crowdtransportfeedback.data.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CooldownAndConflictTest {
    @Test fun canonicalLineCollapsesCaseAndWhitespace() = assertEquals("M 5", normalizeLine("  m   5 "))
    @Test fun safeErrorCodeDistinguishesCooldownFromIdConflict() {
        assertEquals("feedback_cooldown", conflict("feedback_cooldown").safeApiCode())
        assertEquals("feedback_id_conflict", conflict("feedback_id_conflict").safeApiCode())
    }
    @Test fun malformedConflictHasNoInventedReason() = assertNull(HttpException(Response.error<String>(409, "not-json".toResponseBody("application/json".toMediaType()))).safeApiCode())
    private fun conflict(code: String) = HttpException(Response.error<String>(409, "{\"code\":\"$code\"}".toResponseBody("application/json".toMediaType())))
}

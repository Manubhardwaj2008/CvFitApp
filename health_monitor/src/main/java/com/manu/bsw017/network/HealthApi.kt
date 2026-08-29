package com.manu.bsw017.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Mirrors backend/main.py's HealthSample pydantic model exactly.
 * Field names must match the JSON keys FastAPI expects.
 */
data class HealthSample(
    val device_id: String = "BSW017",
    val heart_rate: Int? = null,
    val battery: Int? = null,
    val steps: Int? = null,
    val calories: Int? = null,
    val distance: Int? = null,
    val timestamp: String? = null // ISO-8601; leave null and let the backend stamp it
)

data class SampleResponse(
    val accepted: Boolean,
    val sample: HealthSample
)

data class BackendStatus(
    val backend: String,
    val device: String
)

interface HealthApi {

    @POST("/health/sample")
    suspend fun postSample(@Body sample: HealthSample): Response<SampleResponse>

    @GET("/health/status")
    suspend fun getStatus(): Response<BackendStatus>
}

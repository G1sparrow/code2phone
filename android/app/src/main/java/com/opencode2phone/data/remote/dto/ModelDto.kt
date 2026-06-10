package com.opencode2phone.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ModelListResponse(
    @Json(name = "models") val models: List<ModelDto>
)

@JsonClass(generateAdapter = true)
data class ModelDto(
    @Json(name = "id") val id: String,
    @Json(name = "providerID") val providerID: String
)

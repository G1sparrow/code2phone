package com.opencode2phone.domain.model

data class OpencodeModel(
    val id: String,
    val providerID: String
) {
    val displayName: String get() = "$providerID/$id"
}

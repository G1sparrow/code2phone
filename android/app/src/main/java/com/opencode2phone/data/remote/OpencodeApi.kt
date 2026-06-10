package com.opencode2phone.data.remote

import com.opencode2phone.data.remote.dto.DirectoryListResponse
import com.opencode2phone.data.remote.dto.ModelListResponse
import com.opencode2phone.data.remote.dto.SessionDetailResponse
import com.opencode2phone.data.remote.dto.SessionListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpencodeApi {
    @GET("api/models")
    suspend fun listModels(): ModelListResponse

    @GET("api/directories")
    suspend fun listDirectories(): DirectoryListResponse

    @GET("api/sessions")
    suspend fun listSessions(@Query("directory") directory: String? = null): SessionListResponse

    @GET("api/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): SessionDetailResponse
}

package com.opencode2phone.data.repository

import com.opencode2phone.data.remote.OpencodeApi
import com.opencode2phone.domain.model.OpencodeModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    private val api: OpencodeApi
) {
    private var cachedModels: List<OpencodeModel>? = null

    suspend fun getModels(forceRefresh: Boolean = false): Result<List<OpencodeModel>> {
        if (!forceRefresh && cachedModels != null) {
            return Result.success(cachedModels!!)
        }
        return try {
            val response = api.listModels()
            val models = response.models.map { OpencodeModel(id = it.id, providerID = it.providerID) }
            cachedModels = models
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

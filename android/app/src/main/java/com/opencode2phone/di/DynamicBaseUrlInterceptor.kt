package com.opencode2phone.di

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val serverConfig: ServerConfig
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val newUrl = original.url.newBuilder()
            .host(serverConfig.host)
            .port(serverConfig.port)
            .build()
        val newRequest = original.newBuilder()
            .url(newUrl)
            .build()
        return chain.proceed(newRequest)
    }
}

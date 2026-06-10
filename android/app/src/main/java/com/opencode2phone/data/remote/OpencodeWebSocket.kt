package com.opencode2phone.data.remote

import com.opencode2phone.data.remote.dto.StreamEventDto
import com.opencode2phone.data.remote.dto.WSInitMessage
import com.opencode2phone.data.remote.dto.WSUserMessage
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpencodeWebSocket @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    private var webSocket: WebSocket? = null

    fun connect(
        host: String,
        port: Int,
        sessionId: String? = null,
        firstMessage: String? = null,
        model: String? = null
    ): Flow<StreamEventDto> = callbackFlow {
        val url = "ws://$host:$port/api/chat"
        val request = Request.Builder()
            .url(url)
            .build()

        val eventAdapter = moshi.adapter(StreamEventDto::class.java)
        val initAdapter = moshi.adapter(WSInitMessage::class.java)
        val msgAdapter = moshi.adapter(WSUserMessage::class.java)

        var connected = false

        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                webSocket = ws
                ws.send(initAdapter.toJson(WSInitMessage(sessionId = sessionId)))
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val event = eventAdapter.fromJson(text) ?: return
                    if (event.type == "init" && !connected) {
                        connected = true
                        firstMessage?.let {
                            ws.send(msgAdapter.toJson(WSUserMessage(message = it, model = model)))
                        }
                    }
                    trySend(event)
                } catch (e: Exception) {
                    android.util.Log.e("OpencodeWS", "Failed to parse message", e)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                webSocket = null
                close()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("OpencodeWS", "WebSocket failure", t)
                webSocket = null
                close(t)
            }
        }

        okHttpClient.newWebSocket(request, listener)

        awaitClose { webSocket?.close(1000, "Client closing") }
    }

    fun sendMessage(message: String, model: String? = null) {
        val adapter = moshi.adapter(WSUserMessage::class.java)
        webSocket?.send(adapter.toJson(WSUserMessage(message = message, model = model)))
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}

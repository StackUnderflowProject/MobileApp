package com.example.spotter

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class WebSocketManager {
    private lateinit var socket: Socket

    fun connectWebSocket(token: String, listener: WebsocketListener) {
        try {
            val options = IO.Options()
            options.query = "auth_token=$token" // Optional query parameters
            socket = IO.socket("http://77.38.76.152:3001", options)

            // Listen for events
            socket.on(Socket.EVENT_CONNECT) {
                Log.i("Socket", "connected successfully to websocket")
            }

            socket.on("new-event") {
                listener.onChange()
            }

            socket.on("delete-event") {
                listener.onChange()
            }

            socket.on("error") {
                Log.i("Socket","Websocket Error: ${it[0]}")
            }

            socket.connect()
        } catch (e: Exception) {
            Log.i("Socket","Websocket Error: $e")
        }
    }

    fun emitCreateEvent(token: String) {
        //val data = JSONObject()
        //data.put("token", token)
        socket.emit("create-event", token)
    }

    fun emitDeleteEvent() {
        socket.emit("delete-event")
    }

    fun disconnectWebSocket() {
        socket.disconnect()
    }
}
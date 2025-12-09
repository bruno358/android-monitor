package com.monitor.android

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * Gerenciador de conexão WebSocket
 * Mantém conexão com o servidor e envia dados do dispositivo
 */
class WebSocketManager(
    private val serverUrl: String = "wss://seu-replit-url.replit.dev:8000",
    private val onConnected: () -> Unit = {},
    private val onDisconnected: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    
    companion object {
        private const val TAG = "WebSocketManager"
        private const val RECONNECT_INTERVAL = 5000L // 5 segundos
        private const val CONNECTION_TIMEOUT = 10000 // 10 segundos
    }
    
    private var websocket: WebSocket? = null
    private val gson = Gson()
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    fun connect() {
        scope.launch {
            try {
                Log.d(TAG, "Conectando a $serverUrl")
                
                val factory = WebSocketFactory()
                    .setConnectionTimeout(CONNECTION_TIMEOUT)
                
                websocket = factory.createSocket(serverUrl)
                websocket?.addListener(object : WebSocketAdapter() {
                    override fun onConnected(websocket: WebSocket?, headers: MutableMap<String, MutableList<String>>?) {
                        Log.d(TAG, "Conectado ao servidor")
                        onConnected()
                        sendDeviceRegistration()
                    }
                    
                    override fun onDisconnected(
                        websocket: WebSocket?,
                        serverCloseFrame: com.neovisionaries.ws.client.WebSocketFrame?,
                        clientCloseFrame: com.neovisionaries.ws.client.WebSocketFrame?,
                        closedByServer: Boolean
                    ) {
                        Log.d(TAG, "Desconectado do servidor")
                        onDisconnected()
                        scheduleReconnect()
                    }
                    
                    override fun onTextMessage(websocket: WebSocket?, text: String?) {
                        Log.d(TAG, "Mensagem recebida: $text")
                        handleMessage(text)
                    }
                    
                    override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
                        Log.e(TAG, "Erro WebSocket: ${cause?.message}")
                        onError(cause?.message ?: "Erro desconhecido")
                    }
                })
                
                websocket?.connectAsynchronously()
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao conectar: ${e.message}")
                onError(e.message ?: "Erro ao conectar")
                scheduleReconnect()
            }
        }
    }
    
    private fun sendDeviceRegistration() {
        try {
            val deviceInfo = JsonObject().apply {
                addProperty("type", "register")
                addProperty("device_id", android.os.Build.DEVICE + "_" + android.os.Build.SERIAL)
                add("info", JsonObject().apply {
                    addProperty("device_name", android.os.Build.MODEL)
                    addProperty("model", android.os.Build.MODEL)
                    addProperty("manufacturer", android.os.Build.MANUFACTURER)
                    addProperty("android_version", android.os.Build.VERSION.RELEASE)
                    addProperty("sdk_version", android.os.Build.VERSION.SDK_INT)
                })
            }
            
            websocket?.sendText(deviceInfo.toString())
            Log.d(TAG, "Dispositivo registrado")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar dispositivo: ${e.message}")
        }
    }
    
    fun sendLog(level: String, message: String) {
        try {
            val logData = JsonObject().apply {
                addProperty("type", "log")
                addProperty("level", level)
                addProperty("message", message)
            }
            
            websocket?.sendText(logData.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar log: ${e.message}")
        }
    }
    
    fun sendEvent(eventType: String, data: Map<String, Any>) {
        try {
            val eventData = JsonObject().apply {
                addProperty("type", "event")
                addProperty("event_type", eventType)
                add("data", gson.toJsonTree(data))
            }
            
            websocket?.sendText(eventData.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar evento: ${e.message}")
        }
    }
    
    fun sendData(payload: Map<String, Any>) {
        try {
            val data = JsonObject().apply {
                addProperty("type", "data")
                add("payload", gson.toJsonTree(payload))
            }
            
            websocket?.sendText(data.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar dados: ${e.message}")
        }
    }
    
    private fun handleMessage(text: String?) {
        try {
            if (text == null) return
            
            val json = gson.fromJson(text, JsonObject::class.java)
            val messageType = json.get("type")?.asString
            
            when (messageType) {
                "registered" -> {
                    Log.d(TAG, "Dispositivo registrado com sucesso")
                }
                "command" -> {
                    val command = json.get("command")?.asString
                    Log.d(TAG, "Comando recebido: $command")
                    // Processar comando
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar mensagem: ${e.message}")
        }
    }
    
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_INTERVAL)
            connect()
        }
    }
    
    fun disconnect() {
        reconnectJob?.cancel()
        websocket?.disconnect()
        scope.cancel()
    }
    
    fun isConnected(): Boolean = websocket?.isOpen ?: false
}

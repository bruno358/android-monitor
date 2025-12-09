package com.monitor.android

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

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
        private const val RECONNECT_INTERVAL = 5000L
    }
    
    private var websocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private val deviceId = UUID.randomUUID().toString()
    private var isConnecting = false
    
    fun connect() {
        if (isConnecting || websocket?.isOpen == true) {
            return
        }
        
        isConnecting = true
        scope.launch {
            try {
                val factory = WebSocketFactory()
                factory.setConnectionTimeout(5000)
                
                websocket = factory.createSocket(serverUrl)
                websocket?.addListener(object : WebSocketAdapter() {
                    override fun onConnected(websocket: WebSocket?, headers: MutableMap<String, MutableList<String>>?) {
                        Log.d(TAG, "✅ Conectado ao servidor")
                        isConnecting = false
                        
                        // Enviar registro
                        sendRegister()
                        
                        onConnected()
                    }
                    
                    override fun onDisconnected(
                        websocket: WebSocket?,
                        serverCloseFrame: com.neovisionaries.ws.client.WebSocketFrame?,
                        clientCloseFrame: com.neovisionaries.ws.client.WebSocketFrame?,
                        closedByServer: Boolean
                    ) {
                        Log.d(TAG, "❌ Desconectado do servidor")
                        isConnecting = false
                        onDisconnected()
                        
                        // Tentar reconectar
                        reconnect()
                    }
                    
                    override fun onTextMessage(websocket: WebSocket?, text: String?) {
                        if (text != null) {
                            try {
                                val json = gson.fromJson(text, JsonObject::class.java)
                                val messageType = json.get("type")?.asString
                                
                                when (messageType) {
                                    "registered" -> {
                                        Log.d(TAG, "✅ Dispositivo registrado")
                                    }
                                    "command" -> {
                                        val command = json.get("command")?.asString
                                        Log.d(TAG, "📨 Comando recebido: $command")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Erro ao processar mensagem: ${e.message}")
                            }
                        }
                    }
                    
                    override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
                        Log.e(TAG, "❌ Erro: ${cause?.message}")
                        onError(cause?.message ?: "Erro desconhecido")
                        isConnecting = false
                        reconnect()
                    }
                })
                
                websocket?.connectAsynchronously()
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao conectar: ${e.message}")
                onError(e.message ?: "Erro ao conectar")
                isConnecting = false
                reconnect()
            }
        }
    }
    
    private fun reconnect() {
        scope.launch {
            kotlinx.coroutines.delay(RECONNECT_INTERVAL)
            Log.d(TAG, "🔄 Tentando reconectar...")
            connect()
        }
    }
    
    private fun sendRegister() {
        try {
            val json = JsonObject()
            json.addProperty("type", "register")
            json.addProperty("device_id", deviceId)
            
            val info = JsonObject()
            info.addProperty("device_name", android.os.Build.MODEL)
            info.addProperty("model", android.os.Build.MODEL)
            info.addProperty("manufacturer", android.os.Build.MANUFACTURER)
            info.addProperty("android_version", android.os.Build.VERSION.RELEASE)
            info.addProperty("sdk_version", android.os.Build.VERSION.SDK_INT)
            
            json.add("info", info)
            
            websocket?.sendText(json.toString())
            Log.d(TAG, "📤 Registro enviado")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar registro: ${e.message}")
        }
    }
    
    fun sendData(data: Map<String, Any>) {
        try {
            val json = JsonObject()
            json.addProperty("type", "data")
            
            val payload = JsonObject()
            data.forEach { (key, value) ->
                when (value) {
                    is String -> payload.addProperty(key, value)
                    is Int -> payload.addProperty(key, value)
                    is Long -> payload.addProperty(key, value)
                    is Double -> payload.addProperty(key, value)
                    is Boolean -> payload.addProperty(key, value)
                    else -> payload.addProperty(key, value.toString())
                }
            }
            
            json.add("payload", payload)
            
            websocket?.sendText(json.toString())
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar dados: ${e.message}")
        }
    }
    
    fun sendLog(level: String, message: String) {
        try {
            val json = JsonObject()
            json.addProperty("type", "log")
            json.addProperty("level", level)
            json.addProperty("message", message)
            
            websocket?.sendText(json.toString())
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar log: ${e.message}")
        }
    }
    
    fun sendEvent(eventType: String, data: Map<String, Any> = emptyMap()) {
        try {
            val json = JsonObject()
            json.addProperty("type", "event")
            json.addProperty("event_type", eventType)
            
            val eventData = JsonObject()
            data.forEach { (key, value) ->
                when (value) {
                    is String -> eventData.addProperty(key, value)
                    is Int -> eventData.addProperty(key, value)
                    is Long -> eventData.addProperty(key, value)
                    is Double -> eventData.addProperty(key, value)
                    is Boolean -> eventData.addProperty(key, value)
                    else -> eventData.addProperty(key, value.toString())
                }
            }
            
            json.add("data", eventData)
            
            websocket?.sendText(json.toString())
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar evento: ${e.message}")
        }
    }
    
    fun disconnect() {
        try {
            websocket?.disconnect()
            websocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desconectar: ${e.message}")
        }
    }
    
    fun isConnected(): Boolean = websocket?.isOpen == true
}

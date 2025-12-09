package com.monitor.android

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

/**
 * Serviço que roda em background para monitorar o dispositivo
 * Mantém conexão WebSocket ativa e captura dados continuamente
 */
class MonitoringService : Service() {
    
    companion object {
        private const val TAG = "MonitoringService"
    }
    
    private val binder = LocalBinder()
    private var wsManager: WebSocketManager? = null
    private var deviceMonitor: DeviceMonitor? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    inner class LocalBinder : Binder() {
        fun getService(): MonitoringService = this@MonitoringService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Serviço criado")
        
        // Inicializar WebSocket Manager
        wsManager = WebSocketManager(
            serverUrl = getServerUrl(),
            onConnected = {
                Log.d(TAG, "Conectado ao servidor")
                // Iniciar monitoramento quando conectar
                startDeviceMonitoring()
            },
            onDisconnected = {
                Log.d(TAG, "Desconectado do servidor")
            },
            onError = { error ->
                Log.e(TAG, "Erro: $error")
            }
        )
        
        // Conectar ao servidor
        wsManager?.connect()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Serviço iniciado")
        return START_STICKY // Reiniciar se for morto
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
    
    private fun startDeviceMonitoring() {
        deviceMonitor = DeviceMonitor(this, wsManager!!)
        deviceMonitor?.startMonitoring()
        
        // Capturar logs do sistema a cada 30 segundos
        scope.launch {
            while (isActive) {
                deviceMonitor?.captureLogcat()
                delay(30000)
            }
        }
        
        Log.d(TAG, "Monitoramento iniciado")
    }
    
    private fun getServerUrl(): String {
        // Tentar ler do arquivo de configuração ou usar padrão
        return try {
            val prefs = getSharedPreferences("monitor_config", MODE_PRIVATE)
            prefs.getString("server_url", "ws://192.168.1.100:8765") ?: "ws://192.168.1.100:8765"
        } catch (e: Exception) {
            "ws://192.168.1.100:8765"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Serviço destruído")
        
        deviceMonitor?.stopMonitoring()
        wsManager?.disconnect()
        scope.cancel()
    }
}

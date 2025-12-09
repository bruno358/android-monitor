package com.monitor.android

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Monitor de dados do dispositivo
 * Captura informações de bateria, rede, processos, etc.
 */
class DeviceMonitor(
    private val context: Context,
    private val wsManager: WebSocketManager
) {
    
    companion object {
        private const val TAG = "DeviceMonitor"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var monitoringJob: Job? = null
    
    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    // Capturar dados a cada 5 segundos
                    val deviceData = mutableMapOf<String, Any>()
                    
                    // Bateria
                    val batteryInfo = getBatteryInfo()
                    deviceData.putAll(batteryInfo)
                    
                    // Rede
                    val networkInfo = getNetworkInfo()
                    deviceData.putAll(networkInfo)
                    
                    // Memória
                    val memoryInfo = getMemoryInfo()
                    deviceData.putAll(memoryInfo)
                    
                    // Processos
                    val processInfo = getProcessInfo()
                    deviceData.putAll(processInfo)
                    
                    // Enviar dados
                    wsManager.sendData(deviceData)
                    
                    delay(5000) // Aguardar 5 segundos
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao monitorar: ${e.message}")
                    delay(5000)
                }
            }
        }
    }
    
    fun stopMonitoring() {
        monitoringJob?.cancel()
        scope.cancel()
    }
    
    private fun getBatteryInfo(): Map<String, Any> {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        
        val batteryPct = if (scale > 0) (level * 100) / scale else -1
        
        return mapOf(
            "battery_level" to batteryPct,
            "battery_temperature" to temp,
            "battery_voltage" to voltage,
            "battery_health" to health,
            "battery_plugged" to plugged,
            "battery_status" to status
        )
    }
    
    private fun getNetworkInfo(): Map<String, Any> {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            val wifiConnected = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ?: false
            val cellularConnected = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
            
            mapOf(
                "wifi_connected" to wifiConnected,
                "cellular_connected" to cellularConnected,
                "internet_connected" to (wifiConnected || cellularConnected)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter info de rede: ${e.message}")
            mapOf(
                "wifi_connected" to false,
                "cellular_connected" to false,
                "internet_connected" to false
            )
        }
    }
    
    private fun getMemoryInfo(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        return mapOf(
            "memory_total" to totalMemory,
            "memory_used" to usedMemory,
            "memory_free" to freeMemory,
            "memory_max" to maxMemory,
            "memory_percent" to if (maxMemory > 0) (usedMemory * 100) / maxMemory else 0
        )
    }
    
    private fun getProcessInfo(): Map<String, Any> {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = activityManager.runningAppProcesses
            
            mapOf(
                "running_processes" to (processes?.size ?: 0),
                "device_model" to Build.MODEL,
                "device_manufacturer" to Build.MANUFACTURER,
                "android_version" to Build.VERSION.RELEASE,
                "sdk_version" to Build.VERSION.SDK_INT
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter info de processos: ${e.message}")
            mapOf(
                "running_processes" to 0,
                "device_model" to Build.MODEL,
                "device_manufacturer" to Build.MANUFACTURER,
                "android_version" to Build.VERSION.RELEASE,
                "sdk_version" to Build.VERSION.SDK_INT
            )
        }
    }
    
    fun captureLogcat() {
        scope.launch {
            try {
                val process = Runtime.getRuntime().exec("logcat -d")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val logs = mutableListOf<String>()
                
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (logs.size < 100) { // Manter últimas 100 linhas
                        logs.add(line!!)
                    } else {
                        logs.removeAt(0)
                        logs.add(line!!)
                    }
                }
                
                reader.close()
                
                // Enviar logs
                logs.forEach { logLine ->
                    wsManager.sendLog("INFO", logLine)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao capturar logcat: ${e.message}")
            }
        }
    }
}

package com.monitor.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.view.ViewGroup
import android.widget.ScrollView

/**
 * Activity principal do aplicativo
 * Interface para iniciar/parar monitoramento e configurar servidor
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private var monitoringService: MonitoringService? = null
    private var isBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MonitoringService.LocalBinder
            monitoringService = binder.getService()
            isBound = true
            updateUI()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            monitoringService = null
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Criar interface programaticamente
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Título
        val titleView = TextView(this).apply {
            text = "Android Monitor"
            textSize = 24f
            setPadding(20, 20, 20, 20)
        }
        contentLayout.addView(titleView)
        
        // Status
        val statusView = TextView(this).apply {
            text = "Status: Desconectado"
            textSize = 16f
            setPadding(20, 10, 20, 10)
            tag = "status"
        }
        contentLayout.addView(statusView)
        
        // Informações do dispositivo
        val deviceInfoView = TextView(this).apply {
            text = """
                Modelo: ${Build.MODEL}
                Fabricante: ${Build.MANUFACTURER}
                Android: ${Build.VERSION.RELEASE}
                SDK: ${Build.VERSION.SDK_INT}
            """.trimIndent()
            textSize = 14f
            setPadding(20, 10, 20, 10)
        }
        contentLayout.addView(deviceInfoView)
        
        // Campo de URL do servidor
        val urlLabel = TextView(this).apply {
            text = "URL do Servidor:"
            textSize = 14f
            setPadding(20, 20, 20, 5)
        }
        contentLayout.addView(urlLabel)
        
        val urlInput = EditText(this).apply {
            hint = "wss://seu-replit-url.replit.dev:8000"
            setText(getServerUrl())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 0, 20, 10)
            }
        }
        contentLayout.addView(urlInput)
        
        // Botão para salvar URL
        val saveUrlButton = Button(this).apply {
            text = "💾 Salvar URL"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 5, 20, 10)
            }
            setOnClickListener {
                saveServerUrl(urlInput.text.toString())
                Toast.makeText(
                    this@MainActivity,
                    "URL salva. Reinicie o app.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        contentLayout.addView(saveUrlButton)
        
        // Botão para iniciar
        val startButton = Button(this).apply {
            text = "▶️ Iniciar Monitoramento"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 20, 20, 10)
            }
            setOnClickListener {
                startMonitoring()
            }
            tag = "start_button"
        }
        contentLayout.addView(startButton)
        
        // Botão para parar
        val stopButton = Button(this).apply {
            text = "⏹️ Parar Monitoramento"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 5, 20, 10)
            }
            setOnClickListener {
                stopMonitoring()
            }
            tag = "stop_button"
            isEnabled = false
        }
        contentLayout.addView(stopButton)
        
        // Log de eventos
        val logLabel = TextView(this).apply {
            text = "Log de Eventos:"
            textSize = 14f
            setPadding(20, 20, 20, 5)
        }
        contentLayout.addView(logLabel)
        
        val logView = TextView(this).apply {
            text = "Aguardando eventos...\n"
            textSize = 12f
            setPadding(20, 10, 20, 10)
            setBackgroundColor(0xFFF5F5F5.toInt())
            tag = "log_view"
        }
        contentLayout.addView(logView)
        
        scrollView.addView(contentLayout)
        mainLayout.addView(scrollView)
        
        setContentView(mainLayout)
        
        // Pedir permissões (Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.INTERNET,
                    android.Manifest.permission.ACCESS_NETWORK_STATE
                ),
                1
            )
        }
        
        // Iniciar serviço
        val serviceIntent = Intent(this, MonitoringService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        Log.d(TAG, "MainActivity criada")
    }
    
    private fun startMonitoring() {
        Log.d(TAG, "Iniciando monitoramento")
        addLog("Monitoramento iniciado")
        updateUI()
    }
    
    private fun stopMonitoring() {
        Log.d(TAG, "Parando monitoramento")
        addLog("Monitoramento parado")
        updateUI()
    }
    
    private fun updateUI() {
        val statusView = findViewWithTag<TextView>("status")
        val startButton = findViewWithTag<Button>("start_button")
        val stopButton = findViewWithTag<Button>("stop_button")
        
        if (isBound && monitoringService != null) {
            statusView?.text = "Status: Conectado ✓"
            startButton?.isEnabled = false
            stopButton?.isEnabled = true
        } else {
            statusView?.text = "Status: Desconectado ✗"
            startButton?.isEnabled = true
            stopButton?.isEnabled = false
        }
    }
    
    private fun addLog(message: String) {
        val logView = findViewWithTag<TextView>("log_view")
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logView?.append("[$timestamp] $message\n")
    }
    
    private fun getServerUrl(): String {
        val prefs = getSharedPreferences("monitor_config", MODE_PRIVATE)
        return prefs.getString("server_url", "wss://seu-replit-url.replit.dev:8000") ?: "wss://seu-replit-url.replit.dev:8000"
    }
    
    private fun saveServerUrl(url: String) {
        val prefs = getSharedPreferences("monitor_config", MODE_PRIVATE)
        prefs.edit().putString("server_url", url).apply()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}

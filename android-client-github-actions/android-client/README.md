# Android Monitor - Cliente APK

Aplicativo Android que se conecta ao servidor WebSocket e envia dados do dispositivo em tempo real.

## Recursos

- ✅ Conexão WebSocket automática
- ✅ Monitoramento de bateria em tempo real
- ✅ Informações de rede (WiFi, dados móveis)
- ✅ Uso de memória e processos
- ✅ Captura de logs do sistema
- ✅ Interface simples e intuitiva
- ✅ Reconexão automática
- ✅ Funciona em background

## Compilação

### Requisitos
- Android Studio 2022.1+
- Android SDK 24+
- Kotlin 1.8+

### Passos

1. **Abrir no Android Studio:**
```bash
# Copiar os arquivos para um projeto Android Studio
# Estrutura esperada:
# app/src/main/java/com/monitor/android/
#   ├── MainActivity.kt
#   ├── MonitoringService.kt
#   ├── WebSocketManager.kt
#   └── DeviceMonitor.kt
# app/src/main/AndroidManifest.xml
# app/build.gradle.kts
```

2. **Configurar o servidor:**
   - Abra `WebSocketManager.kt`
   - Altere a URL padrão:
   ```kotlin
   private val serverUrl: String = "ws://SEU_IP:8765"
   ```

3. **Compilar:**
```bash
./gradlew build
```

4. **Gerar APK:**
```bash
./gradlew assembleRelease
```

O APK estará em: `app/build/outputs/apk/release/app-release.apk`

## Instalação no Dispositivo

### Via ADB
```bash
adb install app-release.apk
```

### Via arquivo
- Copiar o APK para o dispositivo
- Abrir o arquivo
- Permitir instalação de fontes desconhecidas
- Instalar

## Uso

1. **Abrir o aplicativo**
2. **Configurar URL do servidor** (se necessário)
3. **Clicar em "Iniciar Monitoramento"**
4. **O aplicativo começará a enviar dados**

## Permissões Necessárias

- `INTERNET` - Para conexão WebSocket
- `ACCESS_NETWORK_STATE` - Para verificar rede
- `BATTERY_STATS` - Para informações de bateria
- `ACCESS_WIFI_STATE` - Para status WiFi
- `READ_LOGS` - Para capturar logcat

## Estrutura de Dados Enviados

### Registro Inicial
```json
{
  "type": "register",
  "device_id": "device_123",
  "info": {
    "device_name": "Pixel 7",
    "model": "Pixel 7",
    "manufacturer": "Google",
    "android_version": "14",
    "sdk_version": 34
  }
}
```

### Dados Periódicos
```json
{
  "type": "data",
  "payload": {
    "battery_level": 85,
    "battery_temperature": 35,
    "battery_voltage": 4200,
    "wifi_connected": true,
    "cellular_connected": false,
    "memory_used": 2048000000,
    "memory_free": 1024000000,
    "running_processes": 150
  }
}
```

### Logs
```json
{
  "type": "log",
  "level": "INFO",
  "message": "Application started"
}
```

### Eventos
```json
{
  "type": "event",
  "event_type": "screen_touch",
  "data": {
    "x": 100,
    "y": 200
  }
}
```

## Troubleshooting

### Não conecta ao servidor
- Verifique se o servidor está rodando
- Verifique se o IP/porta estão corretos
- Verifique se o firewall permite conexão
- Tente usar o IP da máquina em vez de localhost

### Não captura logs
- Verifique permissões do aplicativo
- Tente reiniciar o dispositivo
- Verifique se logcat está disponível

### Alto consumo de bateria
- Aumente o intervalo de monitoramento em `DeviceMonitor.kt`
- Reduza a frequência de captura de logcat

## Desenvolvimento

### Modificar intervalo de monitoramento
Em `DeviceMonitor.kt`:
```kotlin
delay(5000) // Alterar para o intervalo desejado (em ms)
```

### Adicionar novos dados
1. Criar função em `DeviceMonitor.kt`
2. Chamar `wsManager.sendData()` com os dados
3. Recompilar

## Licença
MIT

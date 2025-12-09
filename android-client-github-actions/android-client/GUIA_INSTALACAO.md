# 📱 Android Monitor - Guia Completo de Instalação

Sistema de monitoramento em tempo real para dispositivos Android com servidor Python e painel GUI.

---

## 📦 Arquivos Fornecidos

1. **android-monitor-painel.zip** - Servidor WebSocket + Painel GUI em Python
2. **android-client-apk.zip** - Código-fonte do APK Android em Kotlin

---

## 🔧 Instalação do Painel (Python)

### Requisitos
- Python 3.8 ou superior
- pip (gerenciador de pacotes Python)
- Sistema: Windows, macOS ou Linux

### Passos

#### 1. Extrair o arquivo
```bash
unzip android-monitor-painel.zip
cd android-monitor
```

#### 2. Instalar dependências
```bash
pip install -r requirements.txt
```

#### 3. Configurar IP/Porta (opcional)
Edite `server.py` para alterar a porta (padrão: 8765 para dispositivos, 8766 para GUI):

```python
# Linha ~300
async with websockets.serve(handle_device_connection, "0.0.0.0", 8765):
```

#### 4. Executar o servidor
```bash
python3 server.py
```

Você verá:
```
Iniciando servidor WebSocket...
Servidor de dispositivos aguardando conexões em ws://0.0.0.0:8765
Servidor de GUI aguardando conexões em ws://0.0.0.0:8766
```

#### 5. Em outro terminal, executar o painel GUI
```bash
python3 gui.py
```

Uma janela gráfica será aberta mostrando "Desconectado" até que dispositivos se conectem.

---

## 📱 Compilação do APK Android

### Requisitos
- Android Studio 2022.1 ou superior
- Android SDK 24+
- Kotlin 1.8+

### Passos

#### 1. Extrair o arquivo
```bash
unzip android-client-apk.zip
```

#### 2. Abrir no Android Studio
- Abra Android Studio
- Clique em "Open"
- Selecione a pasta `android-client`

#### 3. Configurar o IP do servidor
Abra `WebSocketManager.kt` e altere:

```kotlin
private val serverUrl: String = "ws://SEU_IP_AQUI:8765"
```

**Exemplos:**
- Mesma máquina: `ws://localhost:8765`
- Rede local: `ws://192.168.1.100:8765`
- Internet: `ws://seu-dominio.com:8765`

#### 4. Compilar o APK
```bash
./gradlew assembleRelease
```

O APK será gerado em:
```
app/build/outputs/apk/release/app-release.apk
```

#### 5. Instalar no dispositivo
```bash
adb install app-release.apk
```

Ou copie o APK para o dispositivo e instale manualmente.

---

## 🚀 Uso

### No Servidor Python

1. Certifique-se que o servidor está rodando:
```bash
python3 server.py
```

2. Abra o painel GUI em outro terminal:
```bash
python3 gui.py
```

### No Dispositivo Android

1. Abra o aplicativo "Android Monitor"
2. (Opcional) Configure a URL do servidor se diferente do padrão
3. Clique em "▶️ Iniciar Monitoramento"
4. O dispositivo aparecerá no painel GUI

### No Painel GUI

- **Lista de Dispositivos**: Mostra todos os dispositivos conectados
- **Clique em um dispositivo**: Vê detalhes, logs e eventos
- **Aba Logs**: Mostra logs do sistema em tempo real
- **Aba Eventos**: Histórico de eventos do dispositivo
- **Aba Status**: Informações atuais (bateria, rede, memória, etc)

---

## 📊 Dados Capturados

O APK envia automaticamente:

- ✅ **Bateria**: Nível, temperatura, voltagem, status
- ✅ **Rede**: WiFi, dados móveis, conectividade
- ✅ **Memória**: Total, usada, livre, percentual
- ✅ **Processos**: Número de processos em execução
- ✅ **Dispositivo**: Modelo, fabricante, versão Android
- ✅ **Logs**: Logcat do sistema
- ✅ **Eventos**: Eventos do sistema

---

## 🌐 Acesso Remoto

### Pela Internet

Se quiser acessar de fora da rede local:

#### Opção 1: Usar ngrok (recomendado)
```bash
# Instalar ngrok
# Depois executar:
ngrok tcp 8765
```

Você receberá uma URL como: `tcp://X.ngrok.io:PORTA`

Use essa URL no APK.

#### Opção 2: Port Forwarding no roteador
- Acesse o painel do roteador
- Configure port forwarding para a porta 8765
- Use o IP público do seu roteador

#### Opção 3: VPN
- Configure uma VPN no servidor
- Conecte o dispositivo à mesma VPN
- Use o IP interno

---

## 🔧 Troubleshooting

### Erro: "Porta já em uso"

**Linux/macOS:**
```bash
lsof -i :8765
kill -9 <PID>
```

**Windows:**
```bash
netstat -ano | findstr :8765
taskkill /PID <PID> /F
```

### Erro: "Módulo não encontrado" (Python)

```bash
pip install --upgrade -r requirements.txt
```

### APK não conecta ao servidor

1. Verifique se o servidor está rodando
2. Verifique se a URL está correta
3. Teste a conexão: `ping SEU_IP`
4. Verifique o firewall

### GUI não mostra dispositivos

1. Verifique se o servidor está rodando
2. Verifique se há dispositivos conectados
3. Tente reiniciar o painel GUI

---

## 📝 Configurações Avançadas

### Alterar intervalo de monitoramento

Edite `DeviceMonitor.kt`:
```kotlin
delay(5000) // Alterar para o intervalo desejado (em ms)
```

### Alterar porta do servidor

Edite `server.py`:
```python
async with websockets.serve(handle_device_connection, "0.0.0.0", 9999):
```

### Aumentar limite de logs

Edite `server.py`:
```python
device["logs"] = device["logs"][-200:]  # Aumentar de 100 para 200
```

---

## 📞 Suporte

Se encontrar problemas:

1. Verifique os logs do servidor
2. Verifique os logs do painel GUI
3. Verifique os logs do dispositivo Android
4. Tente reiniciar tudo

---

## 📄 Licença

MIT

---

## 🎯 Próximos Passos

Após a instalação, você pode:

1. **Adicionar mais dados**: Modifique `DeviceMonitor.kt` para capturar mais informações
2. **Criar alertas**: Implemente notificações quando valores críticos forem atingidos
3. **Armazenar dados**: Salve os dados em um banco de dados
4. **Criar dashboard web**: Adicione uma interface web além do painel Python
5. **Integrar com APIs**: Envie dados para serviços externos

---

Aproveite! 🚀

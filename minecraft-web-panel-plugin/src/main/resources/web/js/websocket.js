/* filepath: c:\Users\cagan\Desktop\MCplugin\minecraft-web-panel-plugin\src\main\resources\web\js\websocket.js */
// WebSocket bağlantı yönetimi
let socket = null;
let connectionStatus = null;
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;
const reconnectDelay = 2000; // 2 saniye

function connectWebSocket() {
    const sessionId = localStorage.getItem('webpanel_session');
    if (!sessionId) {
        console.log('🔌 No session ID found, skipping WebSocket connection');
        return;
    }
    
    console.log('🔌 Attempting WebSocket connection...');
    
    try {
        // WebSocket URL - wsPort ile aynı olmalı
        const wsPort = 8081; // Backend'deki wsPort ile aynı olmalı
        let wsHost = window.location.hostname;
        let wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        // Sadece port ile bağlan (path olmadan)
        let wsUrl = `${wsProtocol}://${wsHost}:${wsPort}`;
        socket = new WebSocket(wsUrl);
        window.socket = socket; // Global erişim için

        socket.onopen = function(event) {
            console.log('✅ WebSocket connection established');
            updateConnectionStatus(true);
            reconnectAttempts = 0;
            
            // Authentication mesajı gönder
            const authMessage = {
                type: 'auth',
                sessionId: sessionId
            };
            
            console.log('🔐 Sending authentication message:', authMessage);
            socket.send(JSON.stringify(authMessage));
            
            if (typeof window.showNotification === 'function') {
                window.showNotification('WebSocket bağlantısı kuruldu', 'success');
            }
        };

        socket.onmessage = function(event) {
            try {
                const data = JSON.parse(event.data);
                console.log('📨 WebSocket message received:', data);
                handleWebSocketMessage(data);
            } catch (error) {
                console.error('❌ Error parsing WebSocket message:', error);
                console.error('Raw message:', event.data);
            }
        };

        socket.onclose = function(event) {
            console.log('🔌 WebSocket connection closed:', event);
            updateConnectionStatus(false);
            window.socket = null;
            
            // Otomatik yeniden bağlanma
            if (reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++;
                const delay = reconnectDelay * Math.pow(1.5, reconnectAttempts - 1);
                console.log(`🔄 Reconnecting in ${delay}ms (attempt ${reconnectAttempts}/${maxReconnectAttempts})`);
                
                setTimeout(() => {
                    connectWebSocket();
                }, delay);
                
                if (typeof window.showNotification === 'function') {
                    window.showNotification(`Yeniden bağlanıyor... (${reconnectAttempts}/${maxReconnectAttempts})`, 'warning');
                }
            } else {
                console.error('❌ Max reconnection attempts reached');
                if (typeof window.showNotification === 'function') {
                    window.showNotification('WebSocket bağlantısı kesildi ve yeniden bağlanılamadı', 'error');
                }
            }
        };

        socket.onerror = function(error) {
            console.error('❌ WebSocket error:', error);
            updateConnectionStatus(false);
            
            if (typeof window.showNotification === 'function') {
                window.showNotification('WebSocket bağlantı hatası', 'error');
            }
        };
        
    } catch (error) {
        console.error('❌ Failed to create WebSocket connection:', error);
        updateConnectionStatus(false);
    }
}

function updateConnectionStatus(isConnected) {
    // Connection status elementini bul (DOM yüklenmeden önce çağrılabilir)
    connectionStatus = connectionStatus || document.getElementById('connectionStatus');
    
    if (connectionStatus) {
        if (isConnected) {
            connectionStatus.innerHTML = '<span class="status-dot online"></span> 🟢 Bağlandı';
            connectionStatus.className = 'connection-status online';
        } else {
            connectionStatus.innerHTML = '<span class="status-dot offline"></span> 🔴 Bağlantı kesildi';
            connectionStatus.className = 'connection-status offline';
        }
        console.log('🔌 Connection status updated:', isConnected ? 'Online' : 'Offline');
    } else {
        console.warn('⚠️ Connection status element not found');
    }
}

function handleWebSocketMessage(data) {
    console.log('📨 Processing WebSocket message type:', data.type);
    
    switch(data.type) {
        case 'auth_success':
            console.log('✅ WebSocket authentication successful');
            if (typeof window.showNotification === 'function') {
                window.showNotification('WebSocket kimlik doğrulandı', 'success');
            }
            break;
            
        case 'auth_failed':
            console.error('❌ WebSocket authentication failed:', data.message);
            if (typeof window.showNotification === 'function') {
                window.showNotification('WebSocket kimlik doğrulama başarısız: ' + (data.message || ''), 'error');
            }
            if (socket) {
                socket.close();
            }
            break;
            
        case 'playerList':
        case 'player_list':
            console.log('👥 Received player list update');
            if (typeof updatePlayerList === 'function') {
                updatePlayerList(data.players || data.data);
            } else {
                console.warn('⚠️ updatePlayerList function not available');
            }
            break;
            
        case 'game_chat':
        case 'chat_message':
            console.log('💬 Received chat message:', data.message, 'from:', data.player);
            if (typeof window.displayChatMessage === 'function') {
                window.displayChatMessage(data.message, data.player || data.sender);
            } else {
                console.warn('⚠️ displayChatMessage function not available');
            }
            break;
            
        case 'player_join':
            console.log('👋 Player joined:', data.player);
            if (typeof window.displayChatMessage === 'function') {
                window.displayChatMessage(`${data.player} sunucuya katıldı`, 'Server');
            }
            if (typeof window.showNotification === 'function') {
                window.showNotification(`${data.player} sunucuya katıldı`, 'info');
            }
            // Oyuncu listesini güncelle
            setTimeout(() => {
                if (typeof window.fetchPlayerList === 'function') {
                    window.fetchPlayerList();
                }
            }, 1000);
            break;
            
        case 'player_leave':
        case 'player_quit':
            console.log('👋 Player left:', data.player);
            if (typeof window.displayChatMessage === 'function') {
                window.displayChatMessage(`${data.player} sunucudan ayrıldı`, 'Server');
            }
            if (typeof window.showNotification === 'function') {
                window.showNotification(`${data.player} sunucudan ayrıldı`, 'info');
            }
            // Oyuncu listesini güncelle
            setTimeout(() => {
                if (typeof window.fetchPlayerList === 'function') {
                    window.fetchPlayerList();
                }
            }, 1000);
            break;
            
        case 'console_output':
        case 'server_log':
            console.log('🖥️ Console output:', data.message);
            if (typeof window.displayConsoleOutput === 'function') {
                window.displayConsoleOutput(data.message, data.level || data.type || 'info');
            } else {
                console.warn('⚠️ displayConsoleOutput function not available');
            }
            break;
            
        case 'server_stats':
            console.log('📊 Received server stats update');
            if (typeof updateServerStats === 'function') {
                updateServerStats(data.data || data.stats);
            } else {
                console.warn('⚠️ updateServerStats function not available');
            }
            break;
            
        case 'player_kicked':
            console.log('👢 Player kicked:', data.player, 'reason:', data.reason);
            if (typeof window.displayConsoleOutput === 'function') {
                window.displayConsoleOutput(`${data.player} atıldı: ${data.reason || 'Sebep belirtilmedi'}`, 'warning');
            }
            if (typeof window.showNotification === 'function') {
                window.showNotification(`${data.player} sunucudan atıldı`, 'warning');
            }
            // Oyuncu listesini güncelle
            setTimeout(() => {
                if (typeof window.fetchPlayerList === 'function') {
                    window.fetchPlayerList();
                }
            }, 1000);
            break;
            
        case 'player_banned':
            console.log('🚫 Player banned:', data.player, 'reason:', data.reason);
            if (typeof window.displayConsoleOutput === 'function') {
                window.displayConsoleOutput(`${data.player} yasaklandı: ${data.reason || 'Sebep belirtilmedi'}`, 'warning');
            }
            if (typeof window.showNotification === 'function') {
                window.showNotification(`${data.player} yasaklandı`, 'warning');
            }
            // Listeleri güncelle
            setTimeout(() => {
                if (typeof window.fetchPlayerList === 'function') {
                    window.fetchPlayerList();
                }
                if (typeof window.fetchBannedPlayers === 'function') {
                    window.fetchBannedPlayers();
                }
            }, 1000);
            break;
            
        case 'command_executed':
        case 'command_result':
            console.log('🖥️ Command executed:', data.command);
            if (typeof window.displayConsoleOutput === 'function') {
                const output = data.output || `Komut çalıştırıldı: ${data.command}`;
                window.displayConsoleOutput(output, 'success');
            }
            break;
            
        case 'server_status':
        case 'serverStatus':
            console.log('🖥️ Server status change:', data.status);
            if (data.status === 'stopping') {
                if (typeof window.showNotification === 'function') {
                    window.showNotification('Sunucu kapatılıyor...', 'warning');
                }
                if (typeof window.displayConsoleOutput === 'function') {
                    window.displayConsoleOutput('Sunucu kapatılıyor...', 'warning');
                }
                updateConnectionStatus(false);
            } else if (data.status === 'starting') {
                if (typeof window.showNotification === 'function') {
                    window.showNotification('Sunucu başlatılıyor...', 'info');
                }
                if (typeof window.displayConsoleOutput === 'function') {
                    window.displayConsoleOutput('Sunucu başlatılıyor...', 'info');
                }
            } else if (data.status === 'online') {
                if (typeof window.showNotification === 'function') {
                    window.showNotification('Sunucu online', 'success');
                }
                updateConnectionStatus(true);
            }
            break;
            
        case 'ping':
            // Ping mesajına pong ile cevap ver
            if (socket && socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({ type: 'pong' }));
            }
            break;
            
        case 'error':
            console.error('❌ WebSocket error message:', data.message);
            if (typeof window.showNotification === 'function') {
                window.showNotification('WebSocket hatası: ' + (data.message || 'Bilinmeyen hata'), 'error');
            }
            break;
            
        default:
            console.log('❓ Unknown WebSocket message type:', data.type);
            console.log('Full message:', data);
    }
}

function sendWebSocketMessage(type, data = {}) {
    if (socket && socket.readyState === WebSocket.OPEN) {
        const message = { type: type, ...data };
        console.log('📤 Sending WebSocket message:', message);
        socket.send(JSON.stringify(message));
        return true;
    } else {
        console.error('❌ WebSocket is not connected. Ready state:', socket ? socket.readyState : 'null');
        if (typeof window.showNotification === 'function') {
            window.showNotification('WebSocket bağlantısı yok', 'error');
        }
        return false;
    }
}

// Manuel olarak bağlantıyı yeniden başlat
function reconnectWebSocket() {
    console.log('🔄 Manual WebSocket reconnection initiated');
    reconnectAttempts = 0;
    
    if (socket) {
        socket.close();
    }
    
    setTimeout(() => {
        connectWebSocket();
    }, 1000);
}

// WebSocket durumunu kontrol et
function getWebSocketStatus() {
    if (!socket) {
        return 'not_connected';
    }
    
    switch (socket.readyState) {
        case WebSocket.CONNECTING:
            return 'connecting';
        case WebSocket.OPEN:
            return 'open';
        case WebSocket.CLOSING:
            return 'closing';
        case WebSocket.CLOSED:
            return 'closed';
        default:
            return 'unknown';
    }
}

// Global fonksiyonlar
window.connectWebSocket = connectWebSocket;
window.sendWebSocketMessage = sendWebSocketMessage;
window.reconnectWebSocket = reconnectWebSocket;
window.getWebSocketStatus = getWebSocketStatus;

// Sayfa kapatılırken WebSocket bağlantısını kapat
window.addEventListener('beforeunload', function() {
    console.log('🔌 Page unloading, closing WebSocket connection');
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close(1000, 'Page unload');
    }
});

// Visibility API - sayfa gizlendiğinde/gösterildiğinde
document.addEventListener('visibilitychange', function() {
    if (document.hidden) {
        console.log('👁️ Page hidden');
    } else {
        console.log('👁️ Page visible');
        // Sayfa geri geldiğinde bağlantıyı kontrol et
        if (getWebSocketStatus() !== 'open') {
            console.log('🔄 Reconnecting WebSocket after page visibility change');
            setTimeout(() => {
                connectWebSocket();
            }, 1000);
        }
    }
});

console.log('🔌 WebSocket.js loaded successfully');
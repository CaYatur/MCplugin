/* filepath: c:\Users\cagan\Desktop\MCplugin\minecraft-web-panel-plugin\src\main\resources\web\js\main.js */
// Global değişkenler - sadece burada tanımla
window.currentUser = null;
window.socket = null;

// DOMContentLoaded - Ana initialization
document.addEventListener("DOMContentLoaded", async function() {
    console.log('🚀 Main.js DOMContentLoaded fired');
    
    // Theme yükle
    loadTheme();
    
    // Authentication kontrol et
    console.log('🔐 Checking authentication...');
    const isAuthenticated = await checkAuth();
    if (!isAuthenticated) {
        console.log('❌ Authentication failed, stopping initialization');
        return;
    }
    
    console.log('✅ Authentication successful, user:', window.currentUser);
    
    // Sistem başlat
    initializeEventListeners();
    loadInitialData();
    startPeriodicUpdates();
    
    // WebSocket bağlantısını başlat (kısa delay ile)
    setTimeout(() => {
        console.log('🔌 Starting WebSocket connection...');
        connectWebSocket();
    }, 1000);
    
    console.log('🎉 Initialization completed');
});

function initializeEventListeners() {
    console.log('📡 Initializing event listeners...');
    
    // Chat input - Enter tuşu
    const chatInput = document.getElementById("chatInput");
    if (chatInput) {
        chatInput.addEventListener("keypress", function(e) {
            if (e.key === "Enter") {
                e.preventDefault();
                window.sendChatMessage();
            }
        });
        console.log('💬 Chat input listener added');
    }

    // Console input - Enter tuşu
    const consoleInput = document.getElementById("consoleInput");
    if (consoleInput) {
        consoleInput.addEventListener("keypress", function(e) {
            if (e.key === "Enter") {
                e.preventDefault();
                window.sendCommand();
            }
        });
        console.log('🖥️ Console input listener added');
    }
    
    // Logout button
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", logout);
        console.log('🚪 Logout button listener added');
    }
    
    // Theme toggle button
    const themeBtn = document.getElementById("themeToggle");
    if (themeBtn) {
        themeBtn.addEventListener("click", toggleTheme);
        console.log('🎨 Theme toggle listener added');
    }
}

function loadInitialData() {
    console.log('📊 Loading initial data...');
    
    // İlk veri yüklemeleri
    window.fetchPlayerList();
    window.fetchServerStats();
    window.fetchBannedPlayers();
    window.fetchBannedIPs();
    window.fetchAnnouncements();
    
    console.log('📊 Initial data load requests sent');
}

function startPeriodicUpdates() {
    console.log('⏰ Starting periodic updates...');
    
    // Her 5 saniyede bir güncel veriler
    setInterval(() => {
        window.fetchPlayerList();
        window.fetchServerStats();
    }, 5000);
    
    // Her dakika banned listeler
    setInterval(() => {
        window.fetchBannedPlayers();
        window.fetchBannedIPs();
    }, 60000);
    
    console.log('⏰ Periodic update timers started');
}

// ============ API FUNCTIONS ============

// Authenticated request helper
window.makeAuthenticatedRequest = function(url, options = {}) {
    const sessionId = localStorage.getItem('webpanel_session');
    
    console.log('🌐 Making request to:', url);
    
    if (!options.headers) {
        options.headers = {};
    }
    
    if (sessionId) {
        options.headers['Authorization'] = 'Bearer ' + sessionId;
        console.log('🔑 Auth header added');
    } else {
        console.warn('⚠️ No session ID found for request');
    }
    
    return fetch(url, options);
}

// ============ CHAT FUNCTIONS ============

window.sendChatMessage = function() {
    console.log('💬 sendChatMessage called');
    const chatInput = document.getElementById("chatInput");
    if (!chatInput) {
        console.error('❌ Chat input element not found');
        return;
    }
    const message = chatInput.value.trim();
    if (!message) {
        console.log('💬 Empty message, ignoring');
        return;
    }
    const sender = window.currentUser || "Web Panel";
    console.log('💬 Sending chat message:', message, 'sender:', sender);
    window.makeAuthenticatedRequest("/api/chat/send", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ message: message, sender: sender })
    })
    .then(response => response.json())
    .then(data => {
        console.log('💬 Chat response:', data);
        if (data.status === 'success') {
            chatInput.value = "";
            window.displayChatMessage(message, sender, true);
            window.showNotification('Mesaj gönderildi', 'success');
        } else {
            window.showNotification('Mesaj gönderilemedi: ' + (data.message || 'Bilinmeyen hata'), 'error');
        }
    })
    .catch(error => {
        console.error('💬 Chat error:', error);
        window.showNotification('Mesaj gönderme hatası', 'error');
    });
}

window.displayChatMessage = function(message, playerName, isWebPanel = false) {
    console.log('💬 Displaying chat message:', message, 'from:', playerName);
    const chatMessages = document.getElementById("chatMessages");
    if (!chatMessages) {
        console.error('❌ Chat messages element not found');
        return;
    }
    const msgDiv = document.createElement("div");
    msgDiv.className = isWebPanel ? "chat-message webpanel" : "chat-message";
    msgDiv.innerHTML = `<span class="chat-player">${playerName}:</span> <span class="chat-text">${message}</span>`;
    chatMessages.appendChild(msgDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// ============ CONSOLE FUNCTIONS ============

window.sendCommand = function() {
    console.log('🖥️ sendCommand called');
    const consoleInput = document.getElementById("consoleInput");
    if (!consoleInput) {
        console.error('❌ Console input element not found');
        return;
    }
    
    const command = consoleInput.value.trim();
    if (!command) {
        console.log('🖥️ Empty command, ignoring');
        return;
    }
    
    console.log('🖥️ Executing command:', command);
    
    window.makeAuthenticatedRequest("/api/console/execute", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ command: command })
    })
    .then(response => response.json())
    .then(data => {
        console.log('🖥️ Command response:', data);
        if (data.status === 'success') {
            addToCommandHistory(command);
            consoleInput.value = "";
            window.displayConsoleOutput(`> ${command}`, 'command');
            window.displayConsoleOutput("Komut başarıyla çalıştırıldı", 'success');
            window.showNotification('Komut çalıştırıldı', 'success');
        } else {
            window.displayConsoleOutput(`Hata: ${data.message || 'Bilinmeyen hata'}`, 'error');
            window.showNotification('Komut hatası: ' + (data.message || 'Bilinmeyen hata'), 'error');
        }
    })
    .catch(error => {
        console.error('🖥️ Command error:', error);
        window.displayConsoleOutput('Bağlantı hatası', 'error');
        window.showNotification('Komut gönderme hatası', 'error');
    });
}

window.displayConsoleOutput = function(message, type = 'info') {
    console.log('🖥️ Console output:', message, 'type:', type);
    const consoleOutput = document.getElementById("consoleOutput");
    if (!consoleOutput) {
        console.error('❌ Console output element not found');
        return;
    }

    const outputElement = document.createElement("div");
    outputElement.className = "console-line";
    
    let colorClass = '';
    switch(type) {
        case 'error':
            colorClass = 'style="color: #f44336;"';
            break;
        case 'warning':
            colorClass = 'style="color: #ff9800;"';
            break;
        case 'success':
            colorClass = 'style="color: #4CAF50;"';
            break;
        case 'command':
            colorClass = 'style="color: #2196F3; font-weight: bold;"';
            break;
        default:
            colorClass = 'style="color: #ffffff;"';
    }
    
    outputElement.innerHTML = `
        <span class="console-time">[${new Date().toLocaleTimeString()}]</span>
        <span class="console-text" ${colorClass}>${message}</span>
    `;
    
    consoleOutput.appendChild(outputElement);
    consoleOutput.scrollTop = consoleOutput.scrollHeight;
    
    // Maksimum 50 satır tut
    if (consoleOutput.children.length > 50) {
        consoleOutput.removeChild(consoleOutput.firstChild);
    }
}

// ============ DATA FETCH FUNCTIONS ============

window.fetchPlayerList = function() {
    console.log('👥 Fetching player list...');
    window.makeAuthenticatedRequest('/api/players')
        .then(response => response.json())
        .then(data => {
            console.log('👥 Player list response:', data);
            if (data.status === 'success') {
                updatePlayerList(data.data);
            } else {
                console.error('👥 Player list error:', data.message);
            }
        })
        .catch(error => {
            console.error('👥 Player list fetch error:', error);
        });
}

window.fetchBannedPlayers = function() {
    console.log('🚫 Fetching banned players...');
    window.makeAuthenticatedRequest("/api/players/banned")
        .then(response => response.json())
        .then(data => {
            console.log('🚫 Banned players response:', data);
            if (data.status === 'success') {
                updateBannedPlayersList(data.data);
            } else {
                console.error('🚫 Banned players error:', data.message);
            }
        })
        .catch(error => {
            console.error('🚫 Banned players fetch error:', error);
        });
}

window.fetchBannedIPs = function() {
    console.log('🌐 Fetching banned IPs...');
    window.makeAuthenticatedRequest("/api/players/banned-ips")
        .then(response => response.json())
        .then(data => {
            console.log('🌐 Banned IPs response:', data);
            if (data.status === 'success') {
                updateBannedIPsList(data.data);
            } else {
                console.error('🌐 Banned IPs error:', data.message);
            }
        })
        .catch(error => {
            console.error('🌐 Banned IPs fetch error:', error);
        });
}

window.fetchServerStats = function() {
    console.log('📊 Fetching server stats...');
    window.makeAuthenticatedRequest('/api/server/stats')
        .then(response => response.json())
        .then(data => {
            console.log('📊 Server stats response:', data);
            if (data.status === 'success') {
                updateServerStats(data.data);
            } else {
                console.error('📊 Server stats error:', data.message);
            }
        })
        .catch(error => {
            console.error('📊 Server stats fetch error:', error);
        });
}

// ============ UI UPDATE FUNCTIONS ============

function updatePlayerList(players) {
    console.log('👥 Updating player list with', players ? players.length : 0, 'players');
    const playersList = document.getElementById("playersList");
    if (!playersList) {
        console.error('❌ playersList element not found');
        return;
    }

    function formatCoords(x, y, z) {
        return `X:${x ?? 0} Y:${y ?? 0} Z:${z ?? 0}`;
    }

    if (players && players.length > 0) {
        playersList.innerHTML = players.map(player => `
            <div class="player-item">
                <div class="player-info">
                    <img src="${player.headUrl || 'https://crafatar.com/avatars/' + (player.uuid || 'steve') + '?size=32'}" 
                         alt="${player.name}" class="player-head" 
                         onerror="this.src='https://crafatar.com/avatars/steve?size=32'">
                    <div class="player-details">
                        <span class="player-name">${player.name}</span>
                        <span class="player-ping">Ping: ${player.ping || 'N/A'}ms</span>
                        <span class="player-location">${player.world || 'N/A'} (${formatCoords(player.x, player.y, player.z)})</span>
                    </div>
                </div>
                <div class="player-actions">
                    <button class="action-btn info" onclick="showPlayerInfo('${player.name}')" title="Bilgi">
                        ℹ️
                    </button>
                    <button class="action-btn kick" onclick="kickPlayer('${player.name}')" title="At">
                        👋
                    </button>
                    <button class="action-btn ban" onclick="banPlayer('${player.name}')" title="Yasakla">
                        🚫
                    </button>
                </div>
            </div>
        `).join('');
        console.log('👥 Player list updated successfully');
    } else {
        playersList.innerHTML = '<div class="no-data">Çevrimiçi oyuncu bulunmuyor</div>';
        console.log('👥 No players online');
    }
}

function updateBannedPlayersList(bannedPlayers) {
    console.log('🚫 Updating banned players list with', bannedPlayers ? bannedPlayers.length : 0, 'entries');
    const bannedList = document.getElementById("bannedPlayersList");
    if (!bannedList) {
        console.error('❌ bannedPlayersList element not found');
        return;
    }

    if (bannedPlayers && bannedPlayers.length > 0) {
        bannedList.innerHTML = bannedPlayers.map(ban => `
            <div class="ban-item">
                <div class="ban-info">
                    <span class="ban-name">${ban.name}</span>
                    <span class="ban-reason">Sebep: ${ban.reason || 'Belirtilmemiş'}</span>
                    <span class="ban-date">Tarih: ${ban.created || 'Bilinmiyor'}</span>
                </div>
                <button class="unban-btn" onclick="unbanPlayer('${ban.name}')">
                    Yasağı Kaldır
                </button>
            </div>
        `).join('');
        console.log('🚫 Banned players list updated successfully');
    } else {
        bannedList.innerHTML = '<div class="no-data">Yasaklı oyuncu bulunmuyor</div>';
        console.log('🚫 No banned players');
    }
}

function updateBannedIPsList(bannedIPs) {
    console.log('🌐 Updating banned IPs list with', bannedIPs ? bannedIPs.length : 0, 'entries');
    const bannedIPsList = document.getElementById("bannedIpsList");
    if (!bannedIPsList) {
        console.error('❌ bannedIpsList element not found');
        return;
    }

    if (bannedIPs && bannedIPs.length > 0) {
        bannedIPsList.innerHTML = bannedIPs.map(ban => `
            <div class="ban-item">
                <div class="ban-info">
                    <span class="ban-name">${ban.ip}</span>
                    <span class="ban-reason">Sebep: ${ban.reason || 'Belirtilmemiş'}</span>
                    <span class="ban-date">Tarih: ${ban.created || 'Bilinmiyor'}</span>
                </div>
                <button class="unban-btn" onclick="unbanIP('${ban.ip}')">
                    Yasağı Kaldır
                </button>
            </div>
        `).join('');
        console.log('🌐 Banned IPs list updated successfully');
    } else {
        bannedIPsList.innerHTML = '<div class="no-data">Yasaklı IP bulunmuyor</div>';
        console.log('🌐 No banned IPs');
    }
}

function updateServerStats(stats) {
    console.log('📊 Updating server stats:', stats);
    
    const elements = {
        onlineCount: document.getElementById("onlineCount"),
        maxPlayers: document.getElementById("maxPlayers"),
        serverVersion: document.getElementById("serverVersion"),
        serverTps: document.getElementById("serverTps"),
        ramUsage: document.getElementById("ramUsage"),
        uptime: document.getElementById("uptime")
    };

    // Online oyuncu sayısı
    if (elements.onlineCount && stats.onlineCount !== undefined) {
        elements.onlineCount.textContent = stats.onlineCount;
    }
    
    // Maksimum oyuncu sayısı
    if (elements.maxPlayers && stats.maxPlayers !== undefined) {
        elements.maxPlayers.textContent = stats.maxPlayers;
    }
    
    // Sunucu versiyonu
    if (elements.serverVersion && stats.version) {
        elements.serverVersion.textContent = stats.version.split('-')[0];
    }
    
    // TPS
    if (elements.serverTps && stats.tps !== undefined) {
        const tps = Number(stats.tps).toFixed(1);
        elements.serverTps.textContent = tps;
        
        // TPS rengini ayarla
        if (stats.tps >= 18) {
            elements.serverTps.style.color = '#4CAF50';
        } else if (stats.tps >= 15) {
            elements.serverTps.style.color = '#ff9800';
        } else {
            elements.serverTps.style.color = '#f44336';
        }
    }
    
    // RAM kullanımı
    if (elements.ramUsage && stats.memoryUsage) {
        const usedMB = Math.round(stats.memoryUsage.used / 1024 / 1024);
        const maxMB = Math.round(stats.memoryUsage.max / 1024 / 1024);
        elements.ramUsage.textContent = `${usedMB}MB / ${maxMB}MB`;
    }
    
    // Uptime
    if (elements.uptime && stats.uptime) {
        elements.uptime.textContent = formatUptime(stats.uptime);
    }
    
    console.log('📊 Server stats updated successfully');
}

function formatUptime(milliseconds) {
    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 0) {
        return `${days}g ${hours % 24}s ${minutes % 60}d`;
    } else if (hours > 0) {
        return `${hours}s ${minutes % 60}d ${seconds % 60}sn`;
    } else if (minutes > 0) {
        return `${minutes}d ${seconds % 60}sn`;
    } else {
        return `${seconds}sn`;
    }
}

// ============ UTILITY FUNCTIONS ============

function addToCommandHistory(command) {
    let history = JSON.parse(localStorage.getItem('webpanel_command_history') || '[]');
    history.unshift(command);
    if (history.length > 50) history = history.slice(0, 50);
    localStorage.setItem('webpanel_command_history', JSON.stringify(history));
    console.log('🖥️ Command added to history:', command);
}

// Notification sistem
window.showNotification = function(message, type = 'info') {
    console.log('🔔 Showing notification:', message, 'type:', type);
    
    // Notification container oluştur (yoksa)
    let container = document.getElementById('notification-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'notification-container';
        container.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            pointer-events: none;
        `;
        document.body.appendChild(container);
    }
    
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.style.cssText = `
        background: ${type === 'success' ? '#4CAF50' : type === 'error' ? '#f44336' : type === 'warning' ? '#ff9800' : '#2196F3'};
        color: white;
        padding: 12px 20px;
        border-radius: 4px;
        margin-bottom: 10px;
        box-shadow: 0 2px 8px rgba(0,0,0,0.2);
        transform: translateX(100%);
        transition: transform 0.3s ease;
        pointer-events: auto;
        cursor: pointer;
        max-width: 300px;
        word-wrap: break-word;
    `;
    notification.textContent = message;
    
    container.appendChild(notification);
    
    // Animasyon
    setTimeout(() => {
        notification.style.transform = 'translateX(0)';
    }, 100);
    
    // Otomatik kapanma
    setTimeout(() => {
        notification.style.transform = 'translateX(100%)';
        setTimeout(() => {
            if (notification.parentNode) {
                container.removeChild(notification);
            }
        }, 300);
    }, 4000);
    
    // Manuel kapanma
    notification.onclick = function() {
        notification.style.transform = 'translateX(100%)';
        setTimeout(() => {
            if (notification.parentNode) {
                container.removeChild(notification);
            }
        }, 300);
    };
}

// GLOBAL UI FONKSİYONLARI (eksik olanlar)
window.switchTab = function(tabId) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(tc => tc.classList.remove('active'));
    document.querySelectorAll(`[onclick^="switchTab('"]`).forEach(btn => {
        if (btn.getAttribute('onclick').includes(tabId)) btn.classList.add('active');
    });
    const tabContent = document.getElementById(tabId);
    if (tabContent) tabContent.classList.add('active');
};

window.switchConsoleTab = function(tabId) {
    document.querySelectorAll('.panel .tab').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.panel .tab-content').forEach(tc => tc.classList.remove('active'));
    document.querySelectorAll(`[onclick^="switchConsoleTab('"]`).forEach(btn => {
        if (btn.getAttribute('onclick').includes(tabId)) btn.classList.add('active');
    });
    const tabContent = document.getElementById(tabId);
    if (tabContent) tabContent.classList.add('active');
};

window.kickPlayer = function(playerName) {
    if (!confirm(playerName + ' oyuncusunu atmak istiyor musun?')) return;
    window.makeAuthenticatedRequest('/api/players/kick', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ player: playerName })
    })
    .then(r => r.json())
    .then(data => window.showNotification(data.message, data.status === 'success' ? 'success' : 'error'));
};

window.kickAllPlayers = function() {
    if (!confirm('Tüm oyuncuları atmak istiyor musun?')) return;
    window.makeAuthenticatedRequest('/api/players/kickall', { method: 'POST' })
    .then(r => r.json())
    .then(data => window.showNotification(data.message, data.status === 'success' ? 'success' : 'error'));
};

window.banPlayer = function(playerName) {
    if (!confirm(playerName + ' oyuncusunu yasaklamak istiyor musun?')) return;
    window.makeAuthenticatedRequest('/api/players/ban', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ player: playerName })
    })
    .then(r => r.json())
    .then(data => {
        window.showNotification(data.message, data.status === 'success' ? 'success' : 'error');
        if (data.status === 'success') window.fetchBannedPlayers();
    });
};

window.saveAll = function() {
    window.makeAuthenticatedRequest('/api/server/saveall', { method: 'POST' })
    .then(r => r.json())
    .then(data => window.showNotification(data.message, data.status === 'success' ? 'success' : 'error'));
};

window.showPlayerInfo = function(playerName) {
    window.makeAuthenticatedRequest(`/api/players/info/${playerName}`)
    .then(r => r.json())
    .then(data => {
        if (data.status === 'success' && data.data) {
            const p = data.data;
            function formatDate(ts) {
                if (!ts || ts < 1000000000000) return '-';
                const d = new Date(Number(ts));
                return d.toLocaleString('tr-TR');
            }
            let html = `<h3>${p.name} (${p.uuid})</h3>`;
            html += `<ul style='text-align:left'>`;
            html += `<li><b>IP:</b> ${p.ip || '-'}</li>`;
            html += `<li><b>World:</b> ${p.world}</li>`;
            html += `<li><b>Koordinat:</b> X:${p.x} Y:${p.y} Z:${p.z}</li>`;
            html += `<li><b>Health:</b> ${p.health}</li>`;
            html += `<li><b>Hunger:</b> ${p.hunger}</li>`;
            html += `<li><b>Level:</b> ${p.level}</li>`;
            html += `<li><b>Ping:</b> ${p.ping}</li>`;
            html += `<li><b>GameMode:</b> ${p.gameMode}</li>`;
            html += `<li><b>OP:</b> ${p.isOp ? 'Evet' : 'Hayır'}</li>`;
            html += `<li><b>Whitelisted:</b> ${p.isWhitelisted ? 'Evet' : 'Hayır'}</li>`;
            html += `<li><b>İlk Giriş:</b> ${formatDate(p.firstPlayed)}</li>`;
            html += `<li><b>Son Giriş:</b> ${formatDate(p.lastPlayed)}</li>`;
            html += `<li><b>DisplayName:</b> ${p.displayName}</li>`;
            if (p.altHesaplar && p.altHesaplar.length > 0) {
                html += `<li><b>Alt Hesaplar (Aynı IP):</b> ${Array.from(p.altHesaplar).join(', ')}</li>`;
            }
            html += `</ul>`;
            // Modalı #playerModal ile göster
            const modal = document.getElementById('playerModal');
            if (modal) {
                document.getElementById('modalTitle').innerText = 'Oyuncu Bilgisi';
                document.getElementById('modalContent').innerHTML = html;
                modal.style.display = 'block';
                modal.querySelector('.close').onclick = () => { modal.style.display = 'none'; };
                window.onclick = function(event) {
                    if (event.target === modal) modal.style.display = 'none';
                };
            } else {
                window.showNotification('Modal bulunamadı', 'error');
            }
        } else {
            window.showNotification('Oyuncu bilgisi alınamadı', 'error');
        }
    });
};

// Basit modal fonksiyonu
typeof showModal === 'function' || (window.showModal = function(title, html) {
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `<div class='modal-content'><h2>${title}</h2>${html}<br><button onclick='this.parentNode.parentNode.remove()'>Kapat</button></div>`;
    document.body.appendChild(modal);
});

window.reloadServer = function() {
    window.makeAuthenticatedRequest('/api/server/reload', { method: 'POST' })
    .then(r => r.json())
    .then(data => window.showNotification(data.message, data.status === 'success' ? 'success' : 'error'));
};
window.stopServer = function() {
    if (!confirm('Sunucuyu durdurmak istiyor musun?')) return;
    window.makeAuthenticatedRequest('/api/server/stop', { method: 'POST' })
    .then(r => r.json())
    .then(data => window.showNotification(data.message, data.status === 'success' ? 'success' : 'error'));
};
window.unbanPlayer = function(playerName) {
    window.makeAuthenticatedRequest('/api/players/unban', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ player: playerName })
    })
    .then(r => r.json())
    .then(data => {
        window.showNotification(data.message, data.status === 'success' ? 'success' : 'error');
        if (data.status === 'success') window.fetchBannedPlayers();
    });
};
window.unbanIP = function(ip) {
    window.makeAuthenticatedRequest('/api/players/unban-ip', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ip })
    })
    .then(r => r.json())
    .then(data => window.showNotification(data.message, data.status === 'success' ? 'success' : 'error'));
};
// Tema fonksiyonunu class yerine attribute ile güncelle
window.loadTheme = function() {
    const savedTheme = localStorage.getItem('webpanel_theme') || 'dark';
    document.body.setAttribute('data-theme', savedTheme);
    console.log('Theme loaded:', savedTheme);
};
window.toggleTheme = function() {
    const body = document.body;
    const currentTheme = body.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    body.setAttribute('data-theme', newTheme);
    localStorage.setItem('webpanel_theme', newTheme);
    console.log('Theme toggled to:', newTheme);
};

// === DUYURULAR ===
window.fetchAnnouncements = function() {
    fetch('/api/announcements')
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('announcementsList');
            if (!list) return;
            if (data.status !== 'success' || !data.data || data.data.length === 0) {
                list.innerHTML = '<p>Henüz duyuru yok.</p>';
                return;
            }
            list.innerHTML = data.data.map((a, i) =>
                `<div class="announcement ${a.type.toLowerCase()}">
                    <b>${a.title}</b> <span style="float:right">${formatAnnTime(a)}</span><br>
                    <span>${a.message}</span><br>
                    <span class="ann-type">${annTypeLabel(a.type)}</span>
                    <button onclick="removeAnnouncement(${i})" class="action-btn danger" style="float:right;">Sil</button>
                </div>`
            ).join('');
        });
};
window.removeAnnouncement = function(idx) {
    fetch('/api/announcements', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'remove', index: idx })
    }).then(() => fetchAnnouncements());
};
window.showAnnouncementModal = function() {
    document.getElementById('announcementModal').style.display = 'block';
};
window.closeAnnouncementModal = function() {
    document.getElementById('announcementModal').style.display = 'none';
};
window.toggleAnnTiming = function() {
    const val = document.getElementById('annTiming').value;
    document.getElementById('annTimeFields').style.display = (val === 'once' || val === 'repeat') ? 'block' : 'none';
};
window.submitAnnouncement = function(e) {
    e.preventDefault();
    const title = document.getElementById('annTitle').value;
    const message = document.getElementById('annMessage').value;
    const type = document.getElementById('annType').value;
    const timing = document.getElementById('annTiming').value;
    let time = Date.now();
    let interval = 0;
    if (timing === 'once') {
        const dt = document.getElementById('annDateTime').value;
        if (dt) time = new Date(dt).getTime();
    } else if (timing === 'repeat') {
        const dt = document.getElementById('annDateTime').value;
        if (dt) time = new Date(dt).getTime();
        interval = Number(document.getElementById('annInterval').value) * 60000;
    }
    const ann = { title, message, type, time, interval, active: true };
    fetch('/api/announcements', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'add', announcement: ann })
    }).then(() => {
        closeAnnouncementModal();
        fetchAnnouncements();
    });
};
function formatAnnTime(a) {
    if (a.interval > 0) return `Her ${a.interval/60000} dk`; 
    const d = new Date(a.time); return d.toLocaleString('tr-TR');
}
function annTypeLabel(type) {
    switch(type) {
        case 'SUCCESS': return 'Başarı';
        case 'WARNING': return 'Uyarı';
        case 'ERROR': return 'Hata';
        default: return 'Bilgi';
    }
}
// WebSocket ile ekranda göster
window.displayAnnouncement = function(title, message, level) {
    const div = document.createElement('div');
    div.className = `announcement-popup ${level}`;
    div.innerHTML = `<b>${title}</b><br>${message}`;
    document.body.appendChild(div);
    setTimeout(() => div.remove(), 8000);
};
// Sayfa yüklenince duyuruları çek
window.addEventListener('DOMContentLoaded', fetchAnnouncements);

// WebSocket mesajı ile ekranda göster
if (window.socket) {
    window.socket.addEventListener('message', function(event) {
        try {
            const data = JSON.parse(event.data);
            if (data.type === 'announcement') {
                window.displayAnnouncement(data.title, data.message, data.level);
            }
        } catch(e) {}
    });
}

// === HİLE UYARILARI ===
window.cheatAlerts = [];
window.displayCheatAlert = function(player, message, cheatType, timestamp) {
    window.cheatAlerts.unshift({ player, message, cheatType, timestamp });
    if (window.cheatAlerts.length > 30) window.cheatAlerts.length = 30;
    renderCheatAlerts();
    showPopup(`🚨 Hile Uyarısı: ${player}`, message, 'warning');
};
function renderCheatAlerts() {
    const list = document.getElementById('cheatAlertsList');
    if (!list) return;
    if (window.cheatAlerts.length === 0) { list.innerHTML = '<p>Hiç uyarı yok.</p>'; return; }
    list.innerHTML = window.cheatAlerts.map(a =>
        `<div class="cheat-alert ${a.cheatType}"><b>${a.player}</b>: ${a.message}<br><span class="cheat-type">${a.cheatType}</span> <span class="cheat-time">${new Date(a.timestamp).toLocaleTimeString('tr-TR')}</span></div>`
    ).join('');
}
// === RAPORLAR ===
window.fetchReports = function() {
    fetch('/api/reports')
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('reportsList');
            if (!list) return;
            if (data.status !== 'success' || !data.data || data.data.length === 0) {
                list.innerHTML = '<p>Hiç rapor yok.</p>';
                return;
            }
            list.innerHTML = data.data.map((r, i) =>
                `<div class="report ${r.resolved ? 'resolved' : ''}">
                    <b>${r.reported}</b> <span style="float:right">${new Date(r.time).toLocaleString('tr-TR')}</span><br>
                    <span>Raporlayan: ${r.reporter}</span><br>
                    <span>Sebep: ${r.reason}</span><br>
                    <button onclick="kickPlayer('${r.reported}')" class="action-btn kick">Kick</button>
                    <button onclick="banPlayer('${r.reported}')" class="action-btn ban">Ban</button>
                    <button onclick="resolveReport(${i})" class="action-btn success">Çözüldü</button>
                </div>`
            ).join('');
        });
};
window.resolveReport = function(idx) {
    fetch('/api/reports', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'resolve', index: idx })
    }).then(() => fetchReports());
};
// === POPUP ===
function showPopup(title, message, level) {
    const div = document.createElement('div');
    div.className = `announcement-popup ${level}`;
    div.innerHTML = `<b>${title}</b><br>${message}`;
    document.body.appendChild(div);
    setTimeout(() => div.remove(), 8000);
}
// === WEBSOCKET ===
if (window.socket) {
    window.socket.addEventListener('message', function(event) {
        try {
            const data = JSON.parse(event.data);
            if (data.type === 'cheat_alert') {
                window.displayCheatAlert(data.player, data.message, data.cheatType, data.timestamp);
            }
            if (data.type === 'player_report') {
                fetchReports();
                showPopup('📝 Yeni Rapor', `${data.reporter} → ${data.reported}<br>${data.reason}`, 'warning');
            }
        } catch(e) {}
    });
}
window.addEventListener('DOMContentLoaded', function() {
    fetchReports();
    renderCheatAlerts();
});

// === HİLE AYARLARI ===
window.cheatSettings = {
    speed: true,
    fly: true,
    fastbreak: true
};
window.saveCheatSettings = function(e) {
    e.preventDefault();
    window.cheatSettings.speed = document.getElementById('cheatDetectSpeed').checked;
    window.cheatSettings.fly = document.getElementById('cheatDetectFly').checked;
    window.cheatSettings.fastbreak = document.getElementById('cheatDetectFastBreak').checked;
    localStorage.setItem('cheatSettings', JSON.stringify(window.cheatSettings));
    showPopup('Ayarlar kaydedildi', '', 'success');
    // WebSocket ile backend'e gönder
    if (window.socket && window.socket.readyState === WebSocket.OPEN) {
        window.socket.send(JSON.stringify({ type: 'cheat_settings', settings: window.cheatSettings }));
    }
};
window.addEventListener('DOMContentLoaded', function() {
    const saved = localStorage.getItem('cheatSettings');
    if (saved) {
        window.cheatSettings = JSON.parse(saved);
        document.getElementById('cheatDetectSpeed').checked = window.cheatSettings.speed;
        document.getElementById('cheatDetectFly').checked = window.cheatSettings.fly;
        document.getElementById('cheatDetectFastBreak').checked = window.cheatSettings.fastbreak;
    }
});

// Rapor gönderme fonksiyonu
window.reportPlayer = function() {
    const reported = prompt('Reportlamak istediğiniz oyuncunun adını girin:');
    if (!reported) return;
    const reason = prompt('Sebep (zorunlu):');
    if (!reason || reason.trim().length < 3) {
        showPopup('Hata', 'Sebep zorunlu ve en az 3 karakter olmalı.', 'error');
        return;
    }
    const reporter = window.currentUser;
    if (!reporter) {
        showPopup('Hata', 'Kullanıcı bilgisi alınamadı.', 'error');
        return;
    }
    fetch('/api/reports', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'add', report: { reporter, reported, reason, time: Date.now(), resolved: false } })
    })
    .then(r => r.json())
    .then(data => {
        if (data.status === 'success') {
            showPopup('Başarılı', 'Rapor gönderildi.', 'success');
            fetchReports();
        } else {
            showPopup('Hata', data.message || 'Rapor gönderilemedi.', 'error');
        }
    })
    .catch(() => showPopup('Hata', 'Rapor gönderilemedi.', 'error'));
};

// Oyuncu yönetimi paneline buton eklemek için örnek:
// <button onclick="reportPlayer()">Rapor Et</button>

console.log('🎯 Main.js loaded successfully');
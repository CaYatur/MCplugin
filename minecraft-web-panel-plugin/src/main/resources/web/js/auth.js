/* filepath: c:\Users\cagan\Desktop\MCplugin\minecraft-web-panel-plugin\src\main\resources\web\js\auth.js */
// Session kontrolü - currentUser tanımı KALDIRILDI (main.js'de var)

async function checkAuth() {
    const sessionId = localStorage.getItem('webpanel_session');
    const username = localStorage.getItem('webpanel_username');
    
    console.log('Auth check - SessionID:', sessionId, 'Username:', username);
    
    if (!sessionId || !username) {
        console.log('No session found, redirecting to login');
        redirectToLogin();
        return false;
    }
    
    try {
        const response = await fetch('/auth/check', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ sessionId })
        });
        
        const data = await response.json();
        console.log('Auth check response:', data);
        
        if (data.status === 'success' && data.valid) {
            window.currentUser = data.username || username; // Global window objesine ata
            updateUserDisplay(window.currentUser);
            console.log('Authentication successful for:', window.currentUser);
            return true;
        } else {
            console.log('Session invalid, clearing and redirecting');
            localStorage.removeItem('webpanel_session');
            localStorage.removeItem('webpanel_username');
            redirectToLogin();
            return false;
        }
    } catch (error) {
        console.error('Auth check failed:', error);
        redirectToLogin();
        return false;
    }
}

function redirectToLogin() {
    if (window.location.pathname !== '/login.html') {
        console.log('Redirecting to login page');
        window.location.href = '/login.html';
    }
}

function updateUserDisplay(username) {
    console.log('Updating user display for:', username);
    const userDisplays = document.querySelectorAll('.user-name, #currentUser, .current-user');
    userDisplays.forEach(element => {
        if (element) {
            element.textContent = username;
        }
    });
    
    // Header'daki kullanıcı adını güncelle
    const userDisplay = document.querySelector('.user-info span');
    if (userDisplay) {
        userDisplay.textContent = username;
    }
}

async function logout() {
    console.log('Logout initiated');
    const sessionId = localStorage.getItem('webpanel_session');
    if (sessionId) {
        try {
            await fetch('/auth/logout', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ sessionId })
            });
            console.log('Logout request sent');
        } catch (error) {
            console.error('Logout error:', error);
        }
    }
    
    // Local storage temizle
    localStorage.removeItem('webpanel_session');
    localStorage.removeItem('webpanel_username');
    window.currentUser = null;
    
    console.log('Logout completed, redirecting to login');
    window.location.href = '/login.html';
}

// Utility function - theme yönetimi
function loadTheme() {
    const savedTheme = localStorage.getItem('webpanel_theme') || 'dark';
    document.body.setAttribute('data-theme', savedTheme);
    console.log('Theme loaded:', savedTheme);
}

function toggleTheme() {
    const body = document.body;
    const currentTheme = body.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    
    body.setAttribute('data-theme', newTheme);
    localStorage.setItem('webpanel_theme', newTheme);
    console.log('Theme toggled to:', newTheme);
}
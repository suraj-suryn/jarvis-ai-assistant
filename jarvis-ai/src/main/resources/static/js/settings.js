/* ============================================================
   JARUS — settings.js
   ============================================================ */
const Settings = (() => {

  async function init() {
    await loadSettings();
    await checkKeyStatus();
  }

  async function loadSettings() {
    try {
      const res = await fetch('/api/settings');
      if (!res.ok) return;
      const data = await res.json();
      const kw = document.getElementById('jobKeywords');
      const loc = document.getElementById('jobLocation');
      const hour = document.getElementById('scanHour');
      if (kw && data.jobKeywords) kw.value = data.jobKeywords;
      if (loc && data.location) loc.value = data.location;
      if (hour && data.scanTimeHour != null) hour.value = data.scanTimeHour;
    } catch (e) { console.error(e); }
  }

  async function checkKeyStatus() {
    try {
      const res = await fetch('/api/settings/gemini-key/status');
      if (!res.ok) return;
      const data = await res.json();
      const statusEl = document.getElementById('keyStatus');
      if (statusEl) {
        statusEl.textContent = data.configured ? '✓ Configured' : '⚠ Not configured';
        statusEl.className = 'key-status ' + (data.configured ? 'key-status-ok' : 'key-status-warn');
      }
    } catch (e) { console.error(e); }
  }

  async function saveKey() {
    const keyInput = document.getElementById('geminiKeyInput');
    const apiKey = keyInput ? keyInput.value.trim() : '';
    const statusEl = document.getElementById('keyStatus');
    if (!apiKey) { alert('Please enter your Gemini API key'); return; }

    if (statusEl) {
      statusEl.textContent = '⏳ Validating…';
      statusEl.className = 'key-status key-status-pending';
    }

    try {
      const res = await fetch('/api/settings/gemini-key', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ apiKey })
      });
      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        // 400 = invalid key
        if (statusEl) {
          statusEl.textContent = '❌ ' + (data.message || 'Invalid API key — not saved');
          statusEl.className = 'key-status key-status-error';
        }
        return;
      }

      keyInput.value = '';
      const status = data.status || 'SAVED';
      if (statusEl) {
        if (status === 'VERIFIED') {
          statusEl.textContent = '✓ ' + data.message;
          statusEl.className = 'key-status key-status-ok';
        } else if (status === 'RATE_LIMITED') {
          statusEl.textContent = '⚠ ' + data.message;
          statusEl.className = 'key-status key-status-warn';
        } else {
          statusEl.textContent = '✓ ' + data.message;
          statusEl.className = 'key-status key-status-ok';
        }
      }
    } catch (e) {
      if (statusEl) {
        statusEl.textContent = '✗ Failed to save key';
        statusEl.className = 'key-status key-status-error';
      }
    }
  }

  async function save() {
    const keywords = document.getElementById('jobKeywords').value.trim();
    const location = document.getElementById('jobLocation').value.trim();
    const scanTimeHour = parseInt(document.getElementById('scanHour').value, 10);

    if (!keywords) {
      const kwEl = document.getElementById('jobKeywords');
      const req = document.getElementById('kwRequired');
      if (kwEl) { kwEl.focus(); kwEl.style.borderColor = 'var(--warning)'; }
      if (req) req.classList.remove('hidden');
      alert('Please enter at least one job keyword (e.g. "Software Engineer, React")');
      return;
    }

    // Clear any warning highlight
    const kwEl = document.getElementById('jobKeywords');
    const req = document.getElementById('kwRequired');
    if (kwEl) kwEl.style.borderColor = '';
    if (req) req.classList.add('hidden');

    const res = await fetch('/api/settings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ jobKeywords: keywords, location, scanTimeHour })
    });
    if (res.ok) {
      alert('Preferences saved! You can now scan for jobs from the Dashboard.');
    } else {
      alert('Failed to save preferences');
    }
  }

  async function subscribePush() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      alert('Push notifications are not supported in this browser.');
      return;
    }
    const permissionResult = await Notification.requestPermission();
    if (permissionResult !== 'granted') {
      alert('Push notification permission denied.');
      return;
    }
    try {
      const keyRes = await fetch('/api/push/vapid-public-key');
      const { publicKey } = await keyRes.json();
      if (!publicKey) { alert('VAPID key not configured on server.'); return; }

      const reg = await navigator.serviceWorker.ready;
      const sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey)
      });
      const subJson = sub.toJSON();
      await fetch('/api/push/subscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          endpoint: subJson.endpoint,
          p256dh: subJson.keys.p256dh,
          auth: subJson.keys.auth
        })
      });
      const pushStatus = document.getElementById('pushStatus');
      if (pushStatus) pushStatus.textContent = '✓ Push notifications enabled';
      alert('Push notifications enabled!');
    } catch (e) {
      alert('Failed to subscribe: ' + e.message);
    }
  }

  async function deleteAccount() {
    if (!confirm('This will permanently delete ALL your data. Are you sure?')) return;
    if (!confirm('Last chance — this cannot be undone!')) return;
    const res = await fetch('/api/account', { method: 'DELETE' });
    if (res.ok) {
      alert('All data deleted. Logging out...');
      window.location.href = '/logout';
    } else {
      alert('Delete failed. Please try again.');
    }
  }

  function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    return Uint8Array.from([...rawData].map(c => c.charCodeAt(0)));
  }

  return { init, save, saveKey, subscribePush, deleteAccount };
})();

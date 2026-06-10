/* ============================================================
   JARUS — main.js — Tab switching, user info, PWA, speech
   ============================================================ */
const App = (() => {
  let currentTab = 'dashboard';
  let userInfo = null;

  function init() {
    // Register service worker
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js').catch(console.error);
    }

    // Tab navigation
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.addEventListener('click', () => switchTab(btn.dataset.tab));
    });

    // Load user settings and populate header
    loadUserInfo();

    // Dashboard stats
    loadDashboardStats();

    // Voice greeting (once)
    setTimeout(greet, 800);
  }

  function switchTab(tabName) {
    currentTab = tabName;
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.add('hidden'));
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    const panel = document.getElementById('tab-' + tabName);
    if (panel) panel.classList.remove('hidden');
    const btn = document.querySelector(`.tab-btn[data-tab="${tabName}"]`);
    if (btn) btn.classList.add('active');

    // Lazy-load tab content
    if (tabName === 'resume') Resume.init();
    if (tabName === 'jobs') Jobs.load();
    if (tabName === 'pipeline') Pipeline.load();
    if (tabName === 'email') Email.load();
    if (tabName === 'settings') Settings.init();
    if (tabName === 'admin') Admin.load();
  }

  async function loadUserInfo() {
    try {
      const res = await fetch('/api/settings');
      if (!res.ok) { window.location.href = '/login'; return; }
      userInfo = await res.json();
      const avatar = document.getElementById('userAvatar');
      const name = document.getElementById('userName');
      if (avatar && userInfo.picture) avatar.src = userInfo.picture;
      if (name) name.textContent = userInfo.name || userInfo.email;

      // Show admin tab if applicable
      if (userInfo.isAdmin) {
        document.querySelectorAll('.admin-only').forEach(el => el.classList.remove('hidden'));
      }
    } catch (e) {
      console.error('Failed to load user info', e);
    }
  }

  async function loadDashboardStats() {
    try {
      const res = await fetch('/api/jobs');
      if (!res.ok) return;
      const jobs = await res.json();
      document.getElementById('numTotal').textContent = jobs.length;
      document.getElementById('numNew').textContent = jobs.filter(j => j.newToday).length;
      document.getElementById('numApplied').textContent = jobs.filter(j => j.status === 'APPLIED').length;
      document.getElementById('numInterview').textContent = jobs.filter(j => j.status === 'INTERVIEW').length;

      // Recent 5 jobs
      const recentList = document.getElementById('recentJobsList');
      if (recentList) {
        const recent = jobs.slice(0, 5);
        recentList.innerHTML = recent.map(j => `
          <div class="job-card">
            <div class="job-card-body">
              <div class="job-title">${esc(j.title)}</div>
              <div class="job-company">${esc(j.company)} &middot; <span class="badge badge-status">${j.status}</span></div>
            </div>
            ${scoreBadge(j.matchScore)}
          </div>
        `).join('');
      }
    } catch (e) {
      console.error('Failed to load dashboard stats', e);
    }
  }

  async function scanJobs() {
    const el = document.getElementById('scanResult');
    if (el) { el.classList.remove('hidden'); el.textContent = 'Scanning…'; }
    try {
      const res = await fetch('/api/jobs/scan', { method: 'POST' });
      const data = await res.json();
      if (el) el.textContent = data.message + ' (' + data.count + ' jobs)';
      loadDashboardStats();
    } catch (e) {
      if (el) el.textContent = 'Scan failed: ' + e.message;
    }
  }

  function greet() {
    if (!window.speechSynthesis) return;
    const hour = new Date().getHours();
    const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
    const msg = new SpeechSynthesisUtterance(`${greeting}. JARUS is ready. Let's find your next opportunity.`);
    msg.rate = 0.95;
    msg.pitch = 1.05;
    window.speechSynthesis.speak(msg);
  }

  function scoreBadge(score) {
    if (!score) return '';
    const cls = score >= 75 ? 'badge-score-high' : score >= 50 ? 'badge-score-mid' : 'badge-score-low';
    return `<span class="badge ${cls}">${score}%</span>`;
  }

  function esc(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }

  return { init, switchTab, scanJobs, scoreBadge, esc };
})();

document.addEventListener('DOMContentLoaded', App.init);

// Admin tab
const Admin = {
  async load() {
    try {
      const res = await fetch('/api/admin/users');
      if (!res.ok) return;
      const data = await res.json();
      const list = document.getElementById('adminUserList');
      if (!list) return;

      // Section 1: Registered users (have logged in at least once)
      const users = data.users || [];
      const allowedEmails = data.allowedEmails || [];
      const registeredHtml = users.length === 0 ? '<p style="color:var(--text-muted);font-size:.85rem">No registered users yet.</p>'
        : users.map(u => `
          <div style="display:flex;align-items:center;gap:.75rem;margin-bottom:.6rem;padding:.5rem;background:rgba(0,212,255,.04);border-radius:6px">
            ${u.pictureUrl ? `<img src="${App.esc(u.pictureUrl)}" style="width:32px;height:32px;border-radius:50%;object-fit:cover" onerror="this.style.display='none'"/>` : '<div style="width:32px;height:32px;border-radius:50%;background:#1a2d42;display:flex;align-items:center;justify-content:center;font-size:.8rem;color:#5a7a9a">👤</div>'}
            <div style="flex:1;min-width:0">
              <div style="font-size:.88rem;color:#c8d8e8;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${App.esc(u.displayName || u.email)}</div>
              <div style="font-size:.77rem;color:#5a7a9a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${App.esc(u.email)}</div>
            </div>
            <button class="btn-danger" style="padding:.25rem .6rem;font-size:.75rem;flex-shrink:0" onclick="Admin.remove('${App.esc(u.email)}')">Revoke</button>
          </div>`).join('');

      // Section 2: Allowlisted emails (not yet logged in)
      const allowlistOnly = allowedEmails.filter(e => !users.some(u => u.email === e));
      const allowlistHtml = allowlistOnly.length === 0 ? ''
        : allowlistOnly.map(e => `
          <div style="display:flex;align-items:center;gap:.5rem;margin-bottom:.4rem">
            <div style="width:32px;height:32px;border-radius:50%;background:#1a2d42;display:flex;align-items:center;justify-content:center;font-size:.8rem;color:#5a7a9a;flex-shrink:0">✉</div>
            <span style="font-size:.86rem;color:#8aa0b8;flex:1">${App.esc(e)}</span>
            <button class="btn-danger" style="padding:.25rem .6rem;font-size:.75rem" onclick="Admin.remove('${App.esc(e)}')">Remove</button>
          </div>`).join('');

      list.innerHTML = `
        <h4 style="color:var(--text-muted);font-size:.8rem;text-transform:uppercase;letter-spacing:.05em;margin:0 0 .6rem">Registered Users (${users.length})</h4>
        ${registeredHtml}
        ${allowlistOnly.length > 0 ? `<h4 style="color:var(--text-muted);font-size:.8rem;text-transform:uppercase;letter-spacing:.05em;margin:1rem 0 .6rem">Pending (invited, not yet logged in)</h4>${allowlistHtml}` : ''}`;
    } catch (e) { console.error(e); }
  },
  async addUser() {
    const email = document.getElementById('newUserEmail').value.trim();
    if (!email) return;
    await fetch('/api/admin/users/add', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ email }) });
    document.getElementById('newUserEmail').value = '';
    Admin.load();
  },
  async remove(email) {
    if (!confirm('Revoke access for ' + email + '?')) return;
    await fetch('/api/admin/users/' + encodeURIComponent(email), { method: 'DELETE' });
    Admin.load();
  }
};

// Company tab
const Company = {
  async research() {
    const company = document.getElementById('companyInput').value.trim();
    const jobTitle = document.getElementById('companyJobTitle').value.trim();
    if (!company) return;
    const progress = document.getElementById('companyProgress');
    const result = document.getElementById('companyResult');
    progress.classList.remove('hidden');
    result.classList.add('hidden');
    if (window.MusicPlayer) MusicPlayer.start();
    try {
      const params = new URLSearchParams({ company });
      if (jobTitle) params.append('jobTitle', jobTitle);
      const res = await fetch('/api/company/research?' + params);
      if (!res.ok) {
        const text = await res.text();
        if (res.status === 402) alert('⚠ Gemini key not configured. Go to Settings → Gemini API Key to add one.');
        else if (res.status === 429) alert('⚠ Rate limited — please wait a minute and try again.');
        else alert(text);
        return;
      }
      const data = await res.json();
      document.getElementById('companyName').textContent = data.companyName;
      document.getElementById('companyOverview').textContent = data.overview;
      document.getElementById('linkedInLink').href = data.linkedInSearchUrl;
      document.getElementById('glassdoorLink').href = data.glassdoorSearchUrl;
      document.getElementById('googleLink').href = data.googleSearchUrl;
      document.getElementById('interviewRounds').textContent = data.interviewRounds;
      document.getElementById('interviewTips').textContent = data.tips;
      const qList = document.getElementById('interviewQuestions');
      qList.innerHTML = (data.interviewQuestions || []).map(q => `<li>${App.esc(q)}</li>`).join('');
      result.classList.remove('hidden');
    } catch (e) {
      alert('Research failed: ' + e.message);
    } finally {
      progress.classList.add('hidden');
      if (window.MusicPlayer) MusicPlayer.stop();
    }
  }
};

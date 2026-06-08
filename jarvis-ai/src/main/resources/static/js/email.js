/* ============================================================
   JARUS — email.js
   ============================================================ */
const Email = (() => {
  let emails = [];
  let currentFilter = 'ALL';

  async function load() {
    const list = document.getElementById('emailList');
    if (!list) return;
    list.innerHTML = '<p style="color:var(--text-muted)">Loading emails…</p>';
    try {
      const res = await fetch('/api/email/jobs');
      if (!res.ok) { list.innerHTML = '<p style="color:var(--danger)">Failed to load. Make sure Gmail access is allowed.</p>'; return; }
      emails = await res.json();
      render();
    } catch (e) {
      list.innerHTML = '<p style="color:var(--danger)">Error: ' + e.message + '</p>';
    }
  }

  function render() {
    const list = document.getElementById('emailList');
    if (!list) return;
    const filtered = currentFilter === 'ALL' ? emails : emails.filter(e => e.tag === currentFilter);
    if (filtered.length === 0) { list.innerHTML = '<p style="color:var(--text-muted)">No emails found.</p>'; return; }
    list.innerHTML = filtered.map(m => `
      <div class="email-item" onclick="Email.openThread('${m.threadId}', '${App.esc(m.subject)}')">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div class="email-subject">${App.esc(m.subject || '(no subject)')}</div>
          <span class="badge tag-${m.tag}" style="font-size:.75rem">${m.tag}</span>
        </div>
        <div class="email-from">${App.esc(m.from || '')}</div>
        <div class="email-snippet">${App.esc(m.snippet || '')}</div>
      </div>
    `).join('');
  }

  async function openThread(threadId, subject) {
    const panel = document.getElementById('threadPanel');
    const messagesEl = document.getElementById('threadMessages');
    document.getElementById('threadSubject').textContent = subject;
    panel.classList.remove('hidden');
    messagesEl.innerHTML = '<p style="color:var(--text-muted)">Loading thread…</p>';
    try {
      const res = await fetch('/api/email/thread/' + threadId);
      if (!res.ok) { messagesEl.innerHTML = '<p style="color:var(--danger)">Failed to load thread.</p>'; return; }
      const msgs = await res.json();
      messagesEl.innerHTML = msgs.map(m => `
        <div style="margin-bottom:1rem;padding:.75rem;background:var(--bg);border-radius:5px;border:1px solid var(--accent-border)">
          <div style="font-size:.8rem;color:var(--text-muted)">${App.esc(m.from || '')} &middot; ${m.receivedAt ? new Date(m.receivedAt).toLocaleString() : ''}</div>
          <div style="margin-top:.5rem;font-size:.88rem;white-space:pre-wrap">${App.esc((m.body || m.snippet || '').substring(0, 1500))}</div>
        </div>
      `).join('');
    } catch (e) {
      messagesEl.innerHTML = '<p style="color:var(--danger)">Error: ' + e.message + '</p>';
    }
  }

  function closeThread() {
    document.getElementById('threadPanel').classList.add('hidden');
  }

  // Filter buttons
  document.addEventListener('click', e => {
    if (e.target.closest('#tab-email') && e.target.classList.contains('filter-btn')) {
      document.querySelectorAll('#tab-email .filter-btn').forEach(b => b.classList.remove('active'));
      e.target.classList.add('active');
      currentFilter = e.target.dataset.filter;
      render();
    }
  });

  window.Email = { load, openThread, closeThread };
  return { load };
})();

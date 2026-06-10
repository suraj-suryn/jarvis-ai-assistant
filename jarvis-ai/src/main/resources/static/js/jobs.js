/* ============================================================
   JARUS — jobs.js
   ============================================================ */
const Jobs = (() => {
  let allJobs = [];
  let currentFilter = 'ALL';

  async function load() {
    try {
      const res = await fetch('/api/jobs');
      if (!res.ok) return;
      allJobs = await res.json();
      renderJobs();
      setupBookmarklet();
    } catch (e) { console.error(e); }
  }

  function renderJobs() {
    const grid = document.getElementById('jobGrid');
    if (!grid) return;
    const filtered = currentFilter === 'ALL' ? allJobs : allJobs.filter(j => j.status === currentFilter);
    if (filtered.length === 0) {
      grid.innerHTML = '<div class="card"><p style="color:var(--text-muted)">No jobs found. Use the bookmarklet to capture jobs, or click Scan Now.</p></div>';
      return;
    }
    grid.innerHTML = filtered.map(j => `
      <div class="job-card">
        <div class="job-card-body">
          <div class="job-title">${App.esc(j.title || 'Untitled')} ${j.newToday ? '<span class="badge badge-new">New</span>' : ''}</div>
          <div class="job-company">${App.esc(j.company || '')} &middot; <span class="badge badge-status">${j.status}</span></div>
          <div class="job-meta">${App.esc(j.source || '')} &middot; ${j.url ? `<a href="${App.esc(j.url)}" target="_blank">View</a>` : ''}</div>
          ${j.matchScore ? App.scoreBadge(j.matchScore) : ''}
          ${j.matchedSkills && j.matchedSkills.length ? `<div class="hint">✓ ${j.matchedSkills.slice(0,3).map(App.esc).join(', ')}</div>` : ''}
          <div class="job-actions">
            <button class="btn-secondary" onclick="Jobs.tailor('${j.id}')">✨ Tailor</button>
            <button class="btn-secondary" onclick="Jobs.research('${App.esc(j.company)}', '${App.esc(j.title)}')">🔎 Research</button>
            <button class="btn-secondary" onclick="Jobs.coverLetter('${j.id}')">📝 Cover Letter</button>
            <button class="btn-danger" onclick="Jobs.deleteJob('${j.id}')">🗑</button>
          </div>
        </div>
      </div>
    `).join('');
  }

  function setupBookmarklet() {
    const bookmarkletCode = `javascript:(function(){
      var title = document.title;
      var url = location.href;
      var company = '';
      var desc = document.body.innerText.substring(0, 3000);
      fetch('${window.location.origin}/api/jobs/capture', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        credentials: 'include',
        body: JSON.stringify({title, url, company, description: desc, source: 'Bookmarklet'})
      }).then(function(r){ alert(r.ok ? 'Job captured!' : 'Failed. Are you logged in?'); });
    })();`;
    const codeEl = document.getElementById('bookmarkletCode');
    if (codeEl) {
      const link = document.createElement('a');
      link.href = bookmarkletCode;
      link.textContent = '📎 Capture Job to JARUS';
      link.draggable = true;
      link.style.cssText = 'display:block;padding:.5rem;text-align:center;';
      codeEl.innerHTML = '';
      codeEl.appendChild(link);
    }
  }

  function aiErrorMessage(status, text) {
    if (status === 402) return '⚠ Gemini key not configured. Go to Settings → Gemini API Key to add one.';
    if (status === 429) return '⚠ Rate limited — please wait a minute and try again.';
    if (status === 401 || status === 403) return '⚠ Session expired. Please refresh and log in again.';
    return 'AI error (' + status + '): ' + (text || 'Please try again.');
  }

  function showBookmarklet() {
    const panel = document.getElementById('bookmarkletPanel');
    if (panel) panel.classList.toggle('hidden');
  }

  function tailor(jobId) {
    App.switchTab('resume');
    setTimeout(() => {
      const sel = document.getElementById('tailorJobSelect');
      if (sel) sel.value = jobId;
    }, 300);
  }

  function research(company, jobTitle) {
    App.switchTab('company');
    setTimeout(() => {
      document.getElementById('companyInput').value = company;
      document.getElementById('companyJobTitle').value = jobTitle;
    }, 200);
  }

  async function coverLetter(jobId) {
    // Load available resumes first
    let resumes = [];
    try {
      const r = await fetch('/api/resume/list');
      if (r.ok) resumes = await r.json();
    } catch (e) {}
    if (resumes.length === 0) {
      alert('No resumes found. Upload one in the Resume tab first.');
      return;
    }

    // Show inline modal for resume selection
    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.75);z-index:999;display:flex;align-items:center;justify-content:center';
    overlay.innerHTML = `
      <div style="background:#0d1b2a;border:1px solid rgba(0,212,255,.3);border-radius:10px;padding:1.5rem;min-width:300px;max-width:440px;width:90%">
        <h3 style="color:#00d4ff;margin-bottom:1rem;font-size:.9rem;text-transform:uppercase;letter-spacing:.06em">Generate Cover Letter</h3>
        <label style="display:block;font-size:.8rem;color:#5a7a9a;margin-bottom:.3rem">Choose resume</label>
        <select id="_clResume" style="width:100%;background:#0a1525;border:1px solid rgba(0,212,255,.25);color:#c8d8e8;padding:.5rem .75rem;border-radius:5px;font-size:.9rem;margin-bottom:1rem">
          ${resumes.map(r => `<option value="${r.id}">${App.esc(r.fileName)}</option>`).join('')}
        </select>
        <div id="_clProgress" style="display:none;color:#5a7a9a;font-size:.85rem;margin-bottom:.75rem">⏳ Generating with Gemini AI…</div>
        <div style="display:flex;gap:.75rem">
          <button id="_clGenBtn" style="flex:1;padding:.55rem;background:#00d4ff;color:#000;border:none;border-radius:5px;font-weight:700;cursor:pointer">Generate</button>
          <button id="_clCancelBtn" style="padding:.55rem 1rem;background:transparent;color:#5a7a9a;border:1px solid rgba(0,212,255,.2);border-radius:5px;cursor:pointer">Cancel</button>
        </div>
      </div>`;
    document.body.appendChild(overlay);

    document.getElementById('_clCancelBtn').onclick = () => overlay.remove();
    document.getElementById('_clGenBtn').onclick = async () => {
      const resumeId = document.getElementById('_clResume').value;
      const btn = document.getElementById('_clGenBtn');
      const progress = document.getElementById('_clProgress');
      btn.disabled = true; btn.textContent = '⏳';
      progress.style.display = 'block';
      if (window.MusicPlayer) MusicPlayer.start();
      try {
        const res = await fetch('/api/cover-letter/generate', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ resumeId, jobId })
        });
        overlay.remove();
        if (!res.ok) { alert(aiErrorMessage(res.status, await res.text())); return; }
        const cl = await res.json();
        // Offer immediate download
        const fmt = confirm('Cover letter generated!\n\nClick OK to download PDF, or Cancel to download DOCX.');
        window.location.href = '/api/cover-letter/download/' + cl.id + '?format=' + (fmt ? 'pdf' : 'docx');
        // Refresh cover letters list if Resume tab has loaded it
        if (window.Resume && Resume.loadCoverLetters) Resume.loadCoverLetters();
      } catch (e) {
        overlay.remove();
        alert('Failed: ' + e.message);
      } finally {
        if (window.MusicPlayer) MusicPlayer.stop();
      }
    };
  }

  async function deleteJob(id) {
    if (!confirm('Delete this job?')) return;
    await fetch('/api/jobs/' + id, { method: 'DELETE' });
    load();
  }

  // Filter buttons
  document.addEventListener('click', e => {
    if (e.target.closest('#tab-jobs') && e.target.classList.contains('filter-btn')) {
      document.querySelectorAll('#tab-jobs .filter-btn').forEach(b => b.classList.remove('active'));
      e.target.classList.add('active');
      currentFilter = e.target.dataset.filter;
      renderJobs();
    }
  });

  return { load, tailor, research, coverLetter, deleteJob, showBookmarklet };
})();

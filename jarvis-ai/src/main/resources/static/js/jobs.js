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
    const resumeId = prompt('Enter resume ID (check Resume tab):');
    if (!resumeId) return;
    const res = await fetch('/api/cover-letter/generate', {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify({ resumeId, jobId })
    });
    if (res.ok) { alert('Cover letter generated! Check Settings > Downloads.'); }
    else { alert(await res.text()); }
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

  window.Jobs = { load, tailor, research, coverLetter, deleteJob, showBookmarklet };
  return { load };
})();

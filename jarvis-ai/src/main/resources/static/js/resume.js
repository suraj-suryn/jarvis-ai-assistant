/* ============================================================
   JARUS — resume.js
   ============================================================ */
const Resume = (() => {
  let resumes = [];
  let currentTailoredId = null;

  function init() {
    setupDropzone();
    loadResumes();
    loadJobsForSelect();
    loadCoverLetters();
  }

  function setupDropzone() {
    const dz = document.getElementById('dropzone');
    const fileInput = document.getElementById('resumeFile');
    if (!dz) return;

    dz.addEventListener('click', () => fileInput.click());
    dz.addEventListener('dragover', e => { e.preventDefault(); dz.classList.add('drag-over'); });
    dz.addEventListener('dragleave', () => dz.classList.remove('drag-over'));
    dz.addEventListener('drop', e => {
      e.preventDefault(); dz.classList.remove('drag-over');
      const file = e.dataTransfer.files[0];
      if (file) uploadFile(file);
    });
    fileInput.addEventListener('change', () => {
      if (fileInput.files[0]) uploadFile(fileInput.files[0]);
    });
  }

  async function uploadFile(file) {
    const progress = document.getElementById('uploadProgress');
    progress.classList.remove('hidden');
    progress.textContent = 'Uploading ' + file.name + '…';
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await fetch('/api/resume/upload', { method: 'POST', body: formData });
      if (!res.ok) { alert('Upload failed: ' + await res.text()); return; }
      progress.textContent = 'Upload successful!';
      setTimeout(() => progress.classList.add('hidden'), 2000);
      loadResumes();
    } catch (e) {
      alert('Upload failed: ' + e.message);
      progress.classList.add('hidden');
    }
  }

  async function loadResumes() {
    try {
      const res = await fetch('/api/resume/list');
      if (!res.ok) return;
      resumes = await res.json();
      renderResumeList();
      populateResumeSelect();
    } catch (e) { console.error(e); }
  }

  function renderResumeList() {
    const list = document.getElementById('resumeList');
    if (!list) return;
    if (resumes.length === 0) { list.innerHTML = '<p style="color:var(--text-muted)">No resumes uploaded yet.</p>'; return; }
    list.innerHTML = resumes.map(r => `
      <div class="job-card">
        <div class="job-card-body">
          <div class="job-title">${App.esc(r.fileName)}</div>
          <div class="job-meta">${r.fileType.toUpperCase()} &middot; Uploaded ${new Date(r.uploadedAt).toLocaleDateString()}</div>
        </div>
        <button class="btn-danger" style="align-self:center" onclick="Resume.delete('${r.id}')">Delete</button>
      </div>
    `).join('');
  }

  function populateResumeSelect() {
    const sel = document.getElementById('tailorResumeSelect');
    if (!sel) return;
    sel.innerHTML = '<option value="">Select resume…</option>'
      + resumes.map(r => `<option value="${r.id}">${App.esc(r.fileName)}</option>`).join('');
  }

  async function loadJobsForSelect() {
    try {
      const res = await fetch('/api/jobs');
      if (!res.ok) return;
      const jobs = await res.json();
      const sel = document.getElementById('tailorJobSelect');
      if (!sel) return;
      sel.innerHTML = '<option value="">Select job…</option>'
        + jobs.map(j => `<option value="${j.id}">${App.esc(j.title)} — ${App.esc(j.company)}</option>`).join('');
    } catch (e) { console.error(e); }
  }

  async function tailor() {
    const resumeId = document.getElementById('tailorResumeSelect').value;
    const jobId = document.getElementById('tailorJobSelect').value;
    if (!resumeId || !jobId) { alert('Please select a resume and a job'); return; }
    const progress = document.getElementById('tailorProgress');
    progress.classList.remove('hidden');
    document.getElementById('diffView').classList.add('hidden');
    if (window.MusicPlayer) MusicPlayer.start();
    try {
      const res = await fetch('/api/resume/tailor', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ resumeId, jobId })
      });
      if (!res.ok) {
        const text = await res.text();
        if (res.status === 402) alert('⚠ Gemini key not configured. Go to Settings → Gemini API Key to add one.');
        else if (res.status === 429) alert('⚠ Rate limited — please wait a minute and try again.');
        else alert(text);
        return;
      }
      const tailored = await res.json();
      currentTailoredId = tailored.id;
      renderDiff(tailored);
    } catch (e) {
      alert('Tailor failed: ' + e.message);
    } finally {
      progress.classList.add('hidden');
      if (window.MusicPlayer) MusicPlayer.stop();
    }
  }

  function renderDiff(tailored) {
    const container = document.getElementById('diffContent');
    if (!container) return;
    const sections = tailored.modifiedSections || [];
    container.innerHTML = sections.map(sec => `
      <div class="diff-section">
        <div class="diff-section-title">${App.esc(sec.name)}</div>
        ${sec.wasModified ? `
          <div class="diff-original">${App.esc(sec.originalContent)}</div>
          <div class="diff-modified">▶ ${App.esc(sec.modifiedContent)}</div>
          ${sec.changeReason ? `<div class="hint" style="margin-top:.3rem">${App.esc(sec.changeReason)}</div>` : ''}
        ` : `
          <div class="diff-unchanged">${App.esc(sec.originalContent)}</div>
        `}
      </div>
    `).join('');
    document.getElementById('diffView').classList.remove('hidden');
  }

  function download(format) {
    if (!currentTailoredId) { alert('No tailored resume yet'); return; }
    window.location.href = `/api/resume/download/${currentTailoredId}?format=${format}`;
  }

  async function deleteResume(id) {
    if (!confirm('Delete this resume?')) return;
    await fetch('/api/resume/' + id, { method: 'DELETE' });
    loadResumes();
  }

  async function loadCoverLetters() {
    const el = document.getElementById('coverLetterList');
    if (!el) return;
    try {
      // Load jobs list so we can display job title alongside each letter
      let jobsMap = {};
      try {
        const jr = await fetch('/api/jobs');
        if (jr.ok) { const jobs = await jr.json(); jobs.forEach(j => { jobsMap[j.id] = j; }); }
      } catch (e) {}

      const res = await fetch('/api/cover-letter/list');
      if (!res.ok) return;
      const letters = await res.json();
      if (letters.length === 0) {
        el.innerHTML = '<p style="color:var(--text-muted);font-size:.9rem">No cover letters yet. Generate one from the Jobs tab or write one manually.</p>';
        return;
      }
      el.innerHTML = letters.map(cl => {
        const job = cl.jobId ? jobsMap[cl.jobId] : null;
        const jobLabel = job ? App.esc(job.title + ' — ' + job.company) : (cl.jobId ? 'Job #' + cl.jobId.substring(0, 8) : 'General');
        return `
          <div class="job-card" style="margin-bottom:.5rem">
            <div class="job-card-body">
              <div class="job-title" style="font-size:.88rem">${jobLabel}</div>
              <div class="job-meta">${new Date(cl.createdAt).toLocaleDateString()}</div>
            </div>
            <div style="display:flex;gap:.4rem;align-self:center;flex-wrap:wrap">
              <button class="btn-secondary" style="padding:.3rem .65rem;font-size:.78rem"
                onclick="Resume.viewCoverLetter('${cl.id}')">👁 View</button>
              <button class="btn-secondary" style="padding:.3rem .65rem;font-size:.78rem"
                onclick="window.location.href='/api/cover-letter/download/${cl.id}?format=pdf'">PDF</button>
              <button class="btn-secondary" style="padding:.3rem .65rem;font-size:.78rem"
                onclick="window.location.href='/api/cover-letter/download/${cl.id}?format=docx'">DOCX</button>
              <button class="btn-danger" style="padding:.3rem .65rem;font-size:.78rem"
                onclick="Resume.deleteCoverLetter('${cl.id}')">🗑</button>
            </div>
          </div>
        `;
      }).join('');
    } catch (e) { console.error(e); }
  }

  async function viewCoverLetter(id) {
    const res = await fetch('/api/cover-letter/' + id);
    if (!res.ok) { alert('Could not load cover letter'); return; }
    const cl = await res.json();

    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.8);z-index:1000;display:flex;align-items:center;justify-content:center;padding:1rem';
    overlay.innerHTML = `
      <div style="background:#0d1b2a;border:1px solid rgba(0,212,255,.3);border-radius:10px;padding:1.5rem;width:100%;max-width:640px;max-height:85vh;display:flex;flex-direction:column">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1rem">
          <h3 style="color:#00d4ff;font-size:.9rem;text-transform:uppercase;letter-spacing:.06em;margin:0">Cover Letter</h3>
          <button onclick="this.closest('.cl-overlay').remove()" style="background:none;border:none;color:#5a7a9a;font-size:1.4rem;cursor:pointer;line-height:1">✕</button>
        </div>
        <pre style="white-space:pre-wrap;word-break:break-word;font-family:inherit;font-size:.88rem;line-height:1.65;color:#c8d8e8;overflow-y:auto;flex:1;margin:0">${App.esc(cl.content || '')}</pre>
        <div style="display:flex;gap:.5rem;margin-top:1rem;justify-content:flex-end">
          <button class="btn-secondary" style="font-size:.82rem;padding:.4rem .9rem"
            onclick="window.location.href='/api/cover-letter/download/${id}?format=pdf'">⬇ PDF</button>
          <button class="btn-secondary" style="font-size:.82rem;padding:.4rem .9rem"
            onclick="window.location.href='/api/cover-letter/download/${id}?format=docx'">⬇ DOCX</button>
        </div>
      </div>`;
    overlay.classList.add('cl-overlay');
    overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });
    document.body.appendChild(overlay);
  }

  async function deleteCoverLetter(id) {
    if (!confirm('Delete this cover letter?')) return;
    const res = await fetch('/api/cover-letter/' + id, { method: 'DELETE' });
    if (res.ok) loadCoverLetters();
    else alert('Delete failed');
  }

  function writeCoverLetter() {
    let jobsCache = [];
    fetch('/api/jobs').then(r => r.ok ? r.json() : []).then(jobs => {
      jobsCache = jobs;
      const sel = overlay.querySelector('#_clWriteJob');
      if (sel) {
        sel.innerHTML = '<option value="">No specific job</option>'
          + jobs.map(j => `<option value="${j.id}">${App.esc(j.title + ' — ' + j.company)}</option>`).join('');
      }
    }).catch(() => {});

    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.8);z-index:1000;display:flex;align-items:center;justify-content:center;padding:1rem';
    overlay.innerHTML = `
      <div style="background:#0d1b2a;border:1px solid rgba(0,212,255,.3);border-radius:10px;padding:1.5rem;width:100%;max-width:600px;max-height:90vh;display:flex;flex-direction:column">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1rem">
          <h3 style="color:#00d4ff;font-size:.9rem;text-transform:uppercase;letter-spacing:.06em;margin:0">✍ Write Cover Letter</h3>
          <button onclick="this.closest('.cl-write-overlay').remove()" style="background:none;border:none;color:#5a7a9a;font-size:1.4rem;cursor:pointer;line-height:1">✕</button>
        </div>
        <label style="font-size:.78rem;color:#5a7a9a;margin-bottom:.3rem">Linked job (optional)</label>
        <select id="_clWriteJob" style="background:#0a1525;border:1px solid rgba(0,212,255,.25);color:#c8d8e8;padding:.45rem .75rem;border-radius:5px;font-size:.85rem;margin-bottom:.75rem">
          <option value="">Loading jobs…</option>
        </select>
        <label style="font-size:.78rem;color:#5a7a9a;margin-bottom:.3rem">Cover letter content</label>
        <textarea id="_clWriteText" placeholder="Dear Hiring Manager,&#10;&#10;I am writing to express my interest in…" style="flex:1;min-height:260px;background:#0a1525;border:1px solid rgba(0,212,255,.25);color:#c8d8e8;padding:.65rem .75rem;border-radius:5px;font-size:.87rem;line-height:1.6;resize:vertical;margin-bottom:.75rem;font-family:inherit"></textarea>
        <div id="_clWriteErr" style="display:none;color:#ff6b6b;font-size:.82rem;margin-bottom:.5rem"></div>
        <div style="display:flex;gap:.75rem;justify-content:flex-end">
          <button onclick="this.closest('.cl-write-overlay').remove()" style="padding:.5rem 1rem;background:transparent;color:#5a7a9a;border:1px solid rgba(0,212,255,.2);border-radius:5px;cursor:pointer;font-size:.85rem">Cancel</button>
          <button id="_clWriteSave" style="padding:.5rem 1.2rem;background:#00d4ff;color:#000;border:none;border-radius:5px;font-weight:700;cursor:pointer;font-size:.85rem">💾 Save</button>
        </div>
      </div>`;
    overlay.classList.add('cl-write-overlay');
    overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });
    document.body.appendChild(overlay);

    // Populate jobs after render
    fetch('/api/jobs').then(r => r.ok ? r.json() : []).then(jobs => {
      const sel = document.getElementById('_clWriteJob');
      if (sel) {
        sel.innerHTML = '<option value="">No specific job</option>'
          + jobs.map(j => `<option value="${j.id}">${App.esc(j.title + ' — ' + j.company)}</option>`).join('');
      }
    }).catch(() => {});

    document.getElementById('_clWriteSave').onclick = async () => {
      const content = document.getElementById('_clWriteText').value.trim();
      const jobId = document.getElementById('_clWriteJob').value || null;
      const errEl = document.getElementById('_clWriteErr');
      const btn = document.getElementById('_clWriteSave');
      if (!content) { errEl.textContent = 'Please enter cover letter content.'; errEl.style.display = 'block'; return; }
      errEl.style.display = 'none';
      btn.disabled = true; btn.textContent = '⏳ Saving…';
      try {
        const res = await fetch('/api/cover-letter/write', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ content, jobId })
        });
        if (!res.ok) { errEl.textContent = 'Save failed: ' + await res.text(); errEl.style.display = 'block'; btn.disabled = false; btn.textContent = '💾 Save'; return; }
        overlay.remove();
        loadCoverLetters();
      } catch (e) {
        errEl.textContent = 'Save failed: ' + e.message; errEl.style.display = 'block';
        btn.disabled = false; btn.textContent = '💾 Save';
      }
    };
  }

  return { init, tailor, download, loadCoverLetters, viewCoverLetter, deleteCoverLetter, writeCoverLetter, delete: deleteResume };
})();

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
    try {
      const res = await fetch('/api/resume/tailor', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ resumeId, jobId })
      });
      if (!res.ok) { alert(await res.text()); return; }
      const tailored = await res.json();
      currentTailoredId = tailored.id;
      renderDiff(tailored);
    } catch (e) {
      alert('Tailor failed: ' + e.message);
    } finally {
      progress.classList.add('hidden');
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
      const res = await fetch('/api/cover-letter/list');
      if (!res.ok) return;
      const letters = await res.json();
      if (letters.length === 0) {
        el.innerHTML = '<p style="color:var(--text-muted);font-size:.9rem">No cover letters yet. Generate one from the Jobs tab.</p>';
        return;
      }
      el.innerHTML = letters.map(cl => `
        <div class="job-card" style="margin-bottom:.5rem">
          <div class="job-card-body">
            <div class="job-title" style="font-size:.88rem">Cover Letter</div>
            <div class="job-meta">${new Date(cl.createdAt).toLocaleDateString()}</div>
          </div>
          <div style="display:flex;gap:.5rem;align-self:center">
            <button class="btn-secondary" style="padding:.3rem .7rem;font-size:.8rem"
              onclick="window.location.href='/api/cover-letter/download/${cl.id}?format=pdf'">PDF</button>
            <button class="btn-secondary" style="padding:.3rem .7rem;font-size:.8rem"
              onclick="window.location.href='/api/cover-letter/download/${cl.id}?format=docx'">DOCX</button>
          </div>
        </div>
      `).join('');
    } catch (e) { console.error(e); }
  }

  window.Resume = { init, tailor, download, loadCoverLetters, delete: deleteResume };
  return { init, tailor, download };
})();

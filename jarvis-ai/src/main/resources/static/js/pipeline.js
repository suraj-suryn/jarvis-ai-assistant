/* ============================================================
   JARUS — pipeline.js — Kanban board with drag-drop
   ============================================================ */
const Pipeline = (() => {
  let jobs = [];
  let notesJobId = null;
  const STATUSES = ['NEW', 'SAVED', 'APPLIED', 'INTERVIEW', 'OFFER'];

  async function load() {
    try {
      const res = await fetch('/api/jobs');
      if (!res.ok) return;
      jobs = await res.json();
      render();
    } catch (e) { console.error(e); }
  }

  function render() {
    STATUSES.forEach(status => {
      const col = document.getElementById('col-' + status);
      if (!col) return;
      const colJobs = jobs.filter(j => j.status === status);
      col.innerHTML = colJobs.map(j => `
        <div class="kanban-card" draggable="true" data-id="${j.id}" data-status="${j.status}" onclick="Pipeline.openNotes('${j.id}')">
          <div style="font-weight:600;font-size:.87rem">${App.esc(j.title || 'Untitled')}</div>
          <div style="font-size:.78rem;color:var(--text-muted)">${App.esc(j.company || '')}</div>
          ${j.matchScore ? App.scoreBadge(j.matchScore) : ''}
        </div>
      `).join('');
    });

    // Drag & drop
    document.querySelectorAll('.kanban-card').forEach(card => {
      card.addEventListener('dragstart', e => {
        e.dataTransfer.setData('jobId', card.dataset.id);
        card.classList.add('dragging');
      });
      card.addEventListener('dragend', () => card.classList.remove('dragging'));
    });
    document.querySelectorAll('.kanban-col').forEach(col => {
      col.addEventListener('dragover', e => { e.preventDefault(); col.classList.add('drag-over'); });
      col.addEventListener('dragleave', () => col.classList.remove('drag-over'));
      col.addEventListener('drop', async e => {
        e.preventDefault();
        col.classList.remove('drag-over');
        const jobId = e.dataTransfer.getData('jobId');
        const newStatus = col.dataset.status;
        await updateStatus(jobId, newStatus);
        await load();
      });
    });
  }

  async function updateStatus(jobId, status) {
    await fetch('/api/jobs/' + jobId + '/status', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status })
    });
    const job = jobs.find(j => j.id === jobId);
    if (job) job.status = status;
  }

  function openNotes(jobId) {
    const job = jobs.find(j => j.id === jobId);
    if (!job) return;
    notesJobId = jobId;
    document.getElementById('notesPanelTitle').textContent = (job.title || 'Job') + ' Notes';
    document.getElementById('notesTextarea').value = job.interviewNotes || '';
    document.getElementById('notesPanel').classList.remove('hidden');
  }

  function closeNotes() {
    document.getElementById('notesPanel').classList.add('hidden');
    notesJobId = null;
  }

  async function saveNotes() {
    if (!notesJobId) return;
    const notes = document.getElementById('notesTextarea').value;
    await fetch('/api/jobs/' + notesJobId + '/notes', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ notes })
    });
    const job = jobs.find(j => j.id === notesJobId);
    if (job) job.interviewNotes = notes;
    closeNotes();
  }

  window.Pipeline = { load, openNotes, closeNotes, saveNotes };
  return { load };
})();

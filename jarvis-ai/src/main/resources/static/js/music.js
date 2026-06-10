/* ============================================================
   JARUS — music.js — Ambient waiting music via Web Audio API
   Zero external files. Three synthesized tracks.
   ============================================================ */
const MusicPlayer = (() => {
  let ctx = null;
  let masterGain = null;
  let nodes = [];        // active audio nodes
  let activeTrack = null;
  let fadeTimer = null;
  let active = false;

  // Persist settings
  const PREFS_KEY = 'jarus_music_prefs';
  function loadPrefs() {
    try { return JSON.parse(localStorage.getItem(PREFS_KEY)) || {}; } catch { return {}; }
  }
  function savePrefs(p) {
    try { localStorage.setItem(PREFS_KEY, JSON.stringify({ ...loadPrefs(), ...p })); } catch {}
  }

  function getCtx() {
    if (!ctx || ctx.state === 'closed') {
      ctx = new (window.AudioContext || window.webkitAudioContext)();
      masterGain = ctx.createGain();
      masterGain.gain.value = 0;
      masterGain.connect(ctx.destination);
    }
    if (ctx.state === 'suspended') ctx.resume();
    return ctx;
  }

  // ── Track builders ─────────────────────────────────────────

  function buildLofi(c, dest) {
    // Soft sine pad at 220 Hz with slow amplitude tremolo (0.25 Hz LFO)
    const osc = c.createOscillator();
    osc.type = 'sine';
    osc.frequency.value = 220;

    const lfo = c.createOscillator();
    lfo.type = 'sine';
    lfo.frequency.value = 0.25;
    const lfoGain = c.createGain();
    lfoGain.gain.value = 0.18;
    lfo.connect(lfoGain);

    const oscGain = c.createGain();
    oscGain.gain.value = 0.32;
    lfoGain.connect(oscGain.gain); // tremolo modulation

    // Add a 5th harmonic softly
    const osc2 = c.createOscillator();
    osc2.type = 'sine';
    osc2.frequency.value = 330;
    const osc2Gain = c.createGain();
    osc2Gain.gain.value = 0.08;

    const filter = c.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.value = 900;
    filter.Q.value = 0.8;

    osc.connect(oscGain);
    osc2.connect(osc2Gain);
    oscGain.connect(filter);
    osc2Gain.connect(filter);
    filter.connect(dest);

    osc.start(); osc2.start(); lfo.start();
    return [osc, osc2, lfo, lfoGain, oscGain, osc2Gain, filter];
  }

  function buildDrone(c, dest) {
    // Binaural-style: two oscillators 4 Hz apart → perceived beat
    const f = 432;
    const makeOsc = (freq, pan) => {
      const o = c.createOscillator();
      o.type = 'sine';
      o.frequency.value = freq;
      const g = c.createGain();
      g.gain.value = 0.22;
      const p = c.createStereoPanner ? c.createStereoPanner() : null;
      o.connect(g);
      if (p) { g.connect(p); p.connect(dest); p.pan.value = pan; }
      else g.connect(dest);
      o.start();
      return p ? [o, g, p] : [o, g];
    };
    return [...makeOsc(f, -0.6), ...makeOsc(f + 4, 0.6)];
  }

  function buildRain(c, dest) {
    // White noise → lowpass filter + slow filter sweep
    const bufSize = c.sampleRate * 3;
    const buf = c.createBuffer(1, bufSize, c.sampleRate);
    const data = buf.getChannelData(0);
    for (let i = 0; i < bufSize; i++) data[i] = Math.random() * 2 - 1;

    const src = c.createBufferSource();
    src.buffer = buf;
    src.loop = true;

    const filter = c.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.value = 650;
    filter.Q.value = 0.5;

    // LFO to modulate filter frequency (simulates gusts)
    const lfo = c.createOscillator();
    lfo.type = 'sine';
    lfo.frequency.value = 0.08;
    const lfoGain = c.createGain();
    lfoGain.gain.value = 250;
    lfo.connect(lfoGain);
    lfoGain.connect(filter.frequency);

    const noiseGain = c.createGain();
    noiseGain.gain.value = 0.4;

    src.connect(filter);
    filter.connect(noiseGain);
    noiseGain.connect(dest);

    src.start();
    lfo.start();
    return [src, filter, lfo, lfoGain, noiseGain];
  }

  // ── Track registry ──────────────────────────────────────────
  const TRACKS = {
    lofi:  { label: 'Lo‑Fi Pad',    build: buildLofi },
    drone: { label: 'Focus Drone',  build: buildDrone },
    rain:  { label: 'Rain',         build: buildRain },
  };

  function stopNodes() {
    nodes.forEach(n => {
      try { if (n.stop) n.stop(); } catch {}
      try { n.disconnect(); } catch {}
    });
    nodes = [];
  }

  function _playTrack(name) {
    const c = getCtx();
    stopNodes();
    const track = TRACKS[name] || TRACKS.lofi;
    nodes = track.build(c, masterGain);
    activeTrack = name;
  }

  // ── Public API ──────────────────────────────────────────────

  function start() {
    const prefs = loadPrefs();
    if (prefs.muted) return;
    const track = prefs.track || 'lofi';
    if (!active) {
      _playTrack(track);
      active = true;
    }
    // Fade in
    clearTimeout(fadeTimer);
    const c = getCtx();
    masterGain.gain.cancelScheduledValues(c.currentTime);
    masterGain.gain.setValueAtTime(masterGain.gain.value, c.currentTime);
    masterGain.gain.linearRampToValueAtTime(0.55, c.currentTime + 1.2);
    _showWidget(track, prefs.muted || false);
  }

  function stop() {
    if (!ctx || !active) return;
    const c = ctx;
    // Fade out
    clearTimeout(fadeTimer);
    masterGain.gain.cancelScheduledValues(c.currentTime);
    masterGain.gain.setValueAtTime(masterGain.gain.value, c.currentTime);
    masterGain.gain.linearRampToValueAtTime(0, c.currentTime + 1.5);
    fadeTimer = setTimeout(() => {
      stopNodes();
      active = false;
      _hideWidget();
    }, 1600);
  }

  function setTrack(name) {
    if (!TRACKS[name]) return;
    savePrefs({ track: name });
    if (active) _playTrack(name);
    const sel = document.getElementById('musicTrackSel');
    if (sel) sel.value = name;
  }

  function toggle() {
    const prefs = loadPrefs();
    const nowMuted = !prefs.muted;
    savePrefs({ muted: nowMuted });
    const btn = document.getElementById('musicMuteBtn');
    if (btn) btn.textContent = nowMuted ? '🔇' : '🔊';
    if (nowMuted) {
      if (ctx) masterGain.gain.setTargetAtTime(0, ctx.currentTime, 0.1);
    } else {
      if (ctx && active) masterGain.gain.setTargetAtTime(0.55, ctx.currentTime, 0.1);
    }
  }

  // ── Widget UI ────────────────────────────────────────────────

  function _showWidget(currentTrack, muted) {
    let w = document.getElementById('musicWidget');
    if (!w) return;
    const sel = document.getElementById('musicTrackSel');
    const btn = document.getElementById('musicMuteBtn');
    if (sel) sel.value = currentTrack;
    if (btn) btn.textContent = muted ? '🔇' : '🔊';
    w.classList.remove('hidden');
    w.style.opacity = '0';
    w.style.transform = 'translateY(12px)';
    requestAnimationFrame(() => {
      w.style.transition = 'opacity .4s, transform .4s';
      w.style.opacity = '1';
      w.style.transform = 'translateY(0)';
    });
  }

  function _hideWidget() {
    const w = document.getElementById('musicWidget');
    if (!w) return;
    w.style.opacity = '0';
    w.style.transform = 'translateY(12px)';
    setTimeout(() => w.classList.add('hidden'), 420);
  }

  // Init widget event bindings once DOM is ready
  function initWidget() {
    const sel = document.getElementById('musicTrackSel');
    const btn = document.getElementById('musicMuteBtn');
    const prefs = loadPrefs();
    if (sel) {
      sel.value = prefs.track || 'lofi';
      sel.addEventListener('change', () => setTrack(sel.value));
    }
    if (btn) {
      btn.textContent = prefs.muted ? '🔇' : '🔊';
      btn.addEventListener('click', toggle);
    }
  }

  document.addEventListener('DOMContentLoaded', initWidget);

  return { start, stop, setTrack, toggle };
})();

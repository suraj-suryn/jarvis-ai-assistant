/* JARUS Service Worker */
const CACHE_NAME = 'jarus-v3';
const APP_SHELL = [
  '/',
  '/index.html',
  '/css/jarus.css',
  '/js/main.js?v=3',
  '/js/resume.js?v=3',
  '/js/jobs.js?v=3',
  '/js/pipeline.js?v=3',
  '/js/email.js?v=3',
  '/js/settings.js?v=3',
  '/js/music.js?v=3',
  '/manifest.json'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(APP_SHELL))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  // Never cache API calls or auth endpoints
  if (url.pathname.startsWith('/api/') ||
      url.pathname.startsWith('/login') ||
      url.pathname.startsWith('/oauth2/') ||
      url.pathname.startsWith('/logout')) {
    return;
  }
  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) return cached;
      return fetch(event.request).then(res => {
        if (res && res.status === 200 && event.request.method === 'GET') {
          const resClone = res.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, resClone));
        }
        return res;
      });
    })
  );
});

self.addEventListener('push', event => {
  let data = { title: 'JARUS', body: 'New notification from JARUS' };
  if (event.data) {
    try { data = JSON.parse(event.data.text()); } catch (e) { data.body = event.data.text(); }
  }
  event.waitUntil(
    self.registration.showNotification(data.title || 'JARUS', {
      body: data.body,
      icon: '/icons/icon-192.png',
      badge: '/icons/icon-192.png',
      data: { url: '/' }
    })
  );
});

self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(cs => {
      const existing = cs.find(c => c.url.includes(self.registration.scope));
      if (existing) return existing.focus();
      return clients.openWindow('/');
    })
  );
});

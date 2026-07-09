const CACHE='roomly-v9';
const ASSETS=['/','/index.html','/styles.css','/management.css','/employee.css','/reference-theme.css','/app.js','/admin-checker.js','/admin-checker.css','/employee-rooms.js','/favicon.svg','/manifest.webmanifest'];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS))));
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key!==CACHE).map(key=>caches.delete(key))))));
self.addEventListener('fetch',event=>{if(event.request.method!=='GET'||event.request.url.includes('/api/'))return;event.respondWith(fetch(event.request).then(response=>{const copy=response.clone();caches.open(CACHE).then(cache=>cache.put(event.request,copy));return response}).catch(()=>caches.match(event.request)))});
self.addEventListener('push',event=>{let data={title:'Roomly Work',body:'Ai o actualizare nouă.'};try{data=event.data.json()}catch{}event.waitUntil(self.registration.showNotification(data.title,{body:data.body,icon:'/favicon.svg',badge:'/favicon.svg'}))});

// Helium Mobile Modern Start Page Logic
(function() {
  'use strict';

  // 1. Search Bar Handling
  const searchForm = document.getElementById('search-form');
  const searchInput = document.getElementById('search-input');
  const engineSelect = document.getElementById('engine-select');

  if (searchForm && searchInput && engineSelect) {
    searchForm.addEventListener('submit', function(e) {
      e.preventDefault();
      const query = searchInput.value.trim();
      if (!query) return;

      // Direct URL navigation detection
      if (/^(https?:\/\/|[a-zA-Z0-9-]+\.[a-zA-Z]{2,})(\/.*)?$/.test(query)) {
        const targetUrl = query.startsWith('http://') || query.startsWith('https://') 
          ? query 
          : 'https://' + query;
        window.location.href = targetUrl;
        return;
      }

      // Search engine navigation
      const baseUrl = engineSelect.value;
      window.location.href = baseUrl + encodeURIComponent(query);
    });
  }

  // 2. Speed Dial Bookmarks
  const DEFAULT_BOOKMARKS = [
    { name: 'Helium', url: 'https://github.com/imputnet/helium', icon: 'H' },
    { name: 'DuckDuckGo', url: 'https://duckduckgo.com', icon: 'D' },
    { name: 'Wikipedia', url: 'https://en.wikipedia.org', icon: 'W' },
    { name: 'GitHub', url: 'https://github.com', icon: 'G' },
    { name: 'Reddit', url: 'https://reddit.com', icon: 'R' },
    { name: 'ArchWiki', url: 'https://wiki.archlinux.org', icon: 'A' },
    { name: 'HackerNews', url: 'https://news.ycombinator.com', icon: 'Y' },
    { name: 'ArsTechnica', url: 'https://arstechnica.com', icon: 'T' }
  ];

  function loadBookmarks() {
    let bookmarks = DEFAULT_BOOKMARKS;
    try {
      const stored = localStorage.getItem('helium_speed_dials');
      if (stored) {
        bookmarks = JSON.parse(stored);
      }
    } catch (e) {
      // Use defaults on storage access restriction
    }

    const grid = document.getElementById('bookmarks-grid');
    if (!grid) return;
    grid.innerHTML = '';

    bookmarks.forEach(function(bm) {
      const item = document.createElement('a');
      item.className = 'dial-item';
      item.href = bm.url;

      const icon = document.createElement('span');
      icon.className = 'dial-icon';
      icon.textContent = bm.icon || bm.name.charAt(0);

      const name = document.createElement('span');
      name.className = 'dial-name';
      name.textContent = bm.name;

      item.appendChild(icon);
      item.appendChild(name);
      grid.appendChild(item);
    });
  }

  // 3. Privacy Shield Counters
  function initShieldStats() {
    const counterEl = document.getElementById('blocked-count');
    if (!counterEl) return;

    let count = 142;
    try {
      const stored = localStorage.getItem('helium_blocked_count');
      if (stored) {
        count = parseInt(stored, 10) || count;
      }
    } catch (e) {
      // Fallback
    }

    counterEl.textContent = count.toLocaleString();
  }

  document.addEventListener('DOMContentLoaded', function() {
    loadBookmarks();
    initShieldStats();
  });
})();

/* Sidebar behaviour for the shared layout (fragments/layout.html).
   Desktop: the Collapse button switches between full width and the icon rail and remembers the choice.
   Phone: the menu button opens the sidebar as a drawer; the backdrop or Escape closes it. */
(function () {
  var layout = document.getElementById('layout');
  if (!layout) return;

  var KEY = 'cats.sidebar';
  var saved = null;
  try { saved = localStorage.getItem(KEY); } catch (e) { /* storage blocked: ignore */ }
  if (saved === 'collapsed' || (saved === null && window.innerWidth < 992)) {
    layout.classList.add('collapsed');
  }

  var toggle = document.getElementById('sidebarToggle');
  if (toggle) {
    toggle.addEventListener('click', function () {
      layout.classList.toggle('collapsed');
      var collapsed = layout.classList.contains('collapsed');
      toggle.setAttribute('aria-label', collapsed ? 'Expand sidebar' : 'Collapse sidebar');
      toggle.setAttribute('data-label', collapsed ? 'Expand sidebar' : 'Collapse sidebar');
      try { localStorage.setItem(KEY, collapsed ? 'collapsed' : 'expanded'); } catch (e) { /* ignore */ }
    });
  }

  var menu = document.getElementById('menuToggle');
  if (menu) {
    menu.addEventListener('click', function () {
      var open = layout.classList.toggle('open');
      menu.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  }

  var backdrop = document.getElementById('backdrop');
  if (backdrop) backdrop.addEventListener('click', function () { layout.classList.remove('open'); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') layout.classList.remove('open'); });
})();

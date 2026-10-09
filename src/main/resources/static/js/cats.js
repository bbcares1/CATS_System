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

// Confirm the original form and button, preserving decision values and normal validation.
(function () {
  var dialog = document.getElementById('cats-confirm');
  if (!dialog || !window.bootstrap) return;
  var modal = new bootstrap.Modal(dialog);
  var pending = null;
  document.addEventListener('submit', function (event) {
    var form = event.target;
    if (!form.hasAttribute('data-confirm-title') || form.dataset.confirmed === 'yes') return;
    event.preventDefault();
    pending = { form: form, button: event.submitter };
    document.getElementById('cats-confirm-title').textContent = form.dataset.confirmTitle;
    document.getElementById('cats-confirm-message').textContent = form.dataset.confirmMessage || 'Continue with this action?';
    modal.show();
  });
  document.getElementById('cats-confirm-submit').addEventListener('click', function () {
    if (!pending) return;
    var action = pending;
    action.form.dataset.confirmed = 'yes';
    modal.hide();
    action.form.requestSubmit(action.button || undefined);
    delete action.form.dataset.confirmed;
    pending = null;
  });
  dialog.addEventListener('hidden.bs.modal', function () { pending = null; });
})();

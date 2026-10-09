/* Shared navigation keeps hidden phone links out of the keyboard order. */
(function () {
  var layout = document.getElementById('layout');
  if (!layout) return;
  var sidebar = document.getElementById('sidebar');
  var content = document.querySelector('.cats-content');
  var menu = document.getElementById('menuToggle');
  var toggle = document.getElementById('sidebarToggle');
  var mobile = window.matchMedia('(max-width: 767.98px)');
  var saved = null;
  try { saved = localStorage.getItem('cats.sidebar'); } catch (e) { /* Storage can be disabled. */ }
  if (saved === 'collapsed' || (saved === null && innerWidth < 992)) layout.classList.add('collapsed');

  // Closing by Escape, backdrop or resize restores focus and the same expanded state.
  function setDrawer(open) {
    open = mobile.matches && open;
    layout.classList.toggle('open', open);
    menu.setAttribute('aria-expanded', String(open));
    menu.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
    sidebar.inert = mobile.matches && !open;
    content.inert = open;
    if (open) sidebar.querySelector('a').focus();
    else if (mobile.matches) menu.focus();
  }

  // The desktop rail remembers its width and exposes a useful button label.
  function updateToggle() {
    var label = layout.classList.contains('collapsed') ? 'Expand sidebar' : 'Collapse sidebar';
    toggle.setAttribute('aria-label', label);
    toggle.setAttribute('data-label', label);
  }
  updateToggle();
  toggle.addEventListener('click', function () {
    layout.classList.toggle('collapsed');
    updateToggle();
    try { localStorage.setItem('cats.sidebar', layout.classList.contains('collapsed') ? 'collapsed' : 'expanded'); } catch (e) { /* Keep navigation usable. */ }
  });
  menu.addEventListener('click', function () { setDrawer(!layout.classList.contains('open')); });
  document.getElementById('backdrop').addEventListener('click', function () { setDrawer(false); });
  mobile.addEventListener('change', function () { setDrawer(false); });
  sidebar.inert = mobile.matches;

  // Keep keyboard focus in the open drawer until it is closed.
  document.addEventListener('keydown', function (event) {
    if (!layout.classList.contains('open')) return;
    if (event.key === 'Escape') { setDrawer(false); return; }
    if (event.key !== 'Tab') return;
    var controls = Array.from(sidebar.querySelectorAll('a, button')).filter(function (element) { return element.getClientRects().length && !element.disabled; });
    var first = controls[0], last = controls[controls.length - 1];
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  });
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

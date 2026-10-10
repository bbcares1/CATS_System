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
  var accountRole = document.querySelector('[data-account-role]');
  var accountFields = document.querySelector('[data-account-fields]');
  if (accountRole && accountFields) {
    var updateAccountFields = function () {
      var isAdmin = accountRole.value === 'ADMIN';
      accountFields.hidden = isAdmin;
      accountFields.querySelectorAll('input, select').forEach(function (field) {
        field.disabled = isAdmin;
        field.required = !isAdmin && field.hasAttribute('data-required');
      });
    };
    accountFields.querySelectorAll('[required]').forEach(function (field) {
      field.setAttribute('data-required', 'true');
    });
    accountRole.addEventListener('change', updateAccountFields);
    updateAccountFields();
  }

  var courseCategory = document.querySelector('[data-course-category]');
  var courseFee = document.querySelector('[data-course-fee]');
  if (courseCategory && courseFee) {
    var updateCourseFee = function () {
      var option = courseCategory.options[courseCategory.selectedIndex];
      var isInternalTraining = option && option.textContent.trim().toLowerCase() === 'internal training';
      courseFee.readOnly = Boolean(isInternalTraining);
      if (isInternalTraining) courseFee.value = '0';
    };
    courseCategory.addEventListener('change', updateCourseFee);
    updateCourseFee();
  }

})();

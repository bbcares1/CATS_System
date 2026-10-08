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

  var scheduleForm = document.getElementById('scheduleForm');
  if (scheduleForm) {
    var weekendDatePicker = document.getElementById('weekendDatePicker');
    var weekendTrainingInput = document.getElementById('weekendTrainingDates');
    var selectedWeekendDates = document.getElementById('selectedWeekendDates');
    var selectedWeekends = weekendTrainingInput.value.trim()
      ? weekendTrainingInput.value.split(',').map(function (date) { return date.trim(); }).filter(Boolean)
      : [];
    var excludedDates = new Set(Array.prototype.map.call(
      scheduleForm.querySelectorAll('[data-excluded-date]'),
      function (item) { return item.getAttribute('data-date'); }
    ));

    var updateWeekendDates = function () {
      selectedWeekendDates.replaceChildren();
      selectedWeekends.forEach(function (date) {
        var item = document.createElement('div');
        item.className = 'cats-weekend-date';
        var label = document.createElement('span');
        label.textContent = date;
        var remove = document.createElement('button');
        remove.type = 'button';
        remove.className = 'btn btn-sm btn-outline-danger';
        remove.textContent = 'Remove';
        remove.setAttribute('aria-label', 'Remove weekend date ' + date);
        remove.addEventListener('click', function () {
          selectedWeekends = selectedWeekends.filter(function (selected) { return selected !== date; });
          updateWeekendDates();
        });
        item.append(label, remove);
        selectedWeekendDates.appendChild(item);
      });
      weekendTrainingInput.value = selectedWeekends.join(',');
    };

    scheduleForm.querySelector('[data-weekend-add]').addEventListener('click', function () {
      var date = weekendDatePicker.value;
      if (!date) {
        window.alert('Please select a date.');
        return;
      }
      var parts = date.split('-').map(Number);
      var dayOfWeek = new Date(parts[0], parts[1] - 1, parts[2]).getDay();
      if (dayOfWeek !== 0 && dayOfWeek !== 6) {
        window.alert('Please select a Saturday or Sunday.');
        return;
      }
      if (excludedDates.has(date)) {
        window.alert('This date is excluded and cannot be used for weekend training.');
        return;
      }
      if (selectedWeekends.indexOf(date) !== -1) {
        window.alert('This weekend date has already been added.');
        return;
      }
      selectedWeekends.push(date);
      selectedWeekends.sort();
      weekendDatePicker.value = '';
      updateWeekendDates();
    });

    scheduleForm.addEventListener('submit', function () {
      weekendTrainingInput.value = selectedWeekends.join(',');
    });
    updateWeekendDates();
  }
})();

// Custom dates are editable only when no preset session is selected.
(() => {
  const sessions = document.getElementById('scheduledBatch');
  const dates = document.getElementById('custom-dates');
  if (!sessions || !dates) return;
  function showDates() {
    const custom = sessions.value === '';
    dates.hidden = !custom;
    dates.querySelectorAll('input, select').forEach(field => { field.disabled = !custom; });
  }
  sessions.addEventListener('change', showDates);
  showDates();
})();

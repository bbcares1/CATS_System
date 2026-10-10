(function () {
  const category =
    document.getElementById("courseCategory") ||
    document.getElementById("catalogue-category");
  const halfDay = document.getElementById("half-day-field");
  const fee = document.getElementById("courseFee");
  const batch = document.getElementById("batchId");
  const dates = document.getElementById("date-options");
  const batchVersion = document.getElementById("batchVersion");

  // Fixed schedules use server dates; half-day choices belong to Internal Training only.
  function showFields() {
    const internal = category && category.value === "INTERNAL_TRAINING";
    if (halfDay) halfDay.hidden = !internal;
    if (fee) {
      fee.readOnly = internal;
      if (internal) fee.value = "0.00";
    }
    if (dates && batch) dates.hidden = batch.value !== "";
  }
  if (category) category.addEventListener("change", showFields);
  if (batch)
    batch.addEventListener("change", function () {
      batchVersion.value = batch.selectedOptions[0].dataset.version || "";
      showFields();
    });
  showFields();
})();

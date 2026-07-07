(() => {
  const months = ['Ian','Feb','Mar','Apr','Mai','Iun','Iul','Aug','Sep','Oct','Nov','Dec'];
  let mode = 'hours';

  function availableYears() {
    if (typeof state === 'undefined' || !state.data?.logs) return [];
    return [...new Set(state.data.logs.map(log => Number(log.date.slice(0, 4))))].sort((a, b) => b - a);
  }

  function renderAnnualChart() {
    if (typeof state === 'undefined' || !state.data?.logs) return;
    const yearSelect = document.querySelector('#annual-year');
    const chart = document.querySelector('#annual-chart');
    if (!yearSelect || !chart) return;
    const years = availableYears();
    const previous = Number(yearSelect.value);
    const selectedYear = years.includes(previous) ? previous : (years.includes(2025) ? 2025 : years[0]);
    if (!selectedYear) return;
    yearSelect.innerHTML = years.map(year => `<option value="${year}"${year === selectedYear ? ' selected' : ''}>${year}</option>`).join('');
    const hourlyRate = Number(state.data.me.hourlyRate || 0);
    const values = Array(12).fill(0);
    state.data.logs.filter(log => Number(log.date.slice(0, 4)) === selectedYear)
      .forEach(log => values[Number(log.date.slice(5, 7)) - 1] += Number(log.hours));
    const displayValues = mode === 'money' ? values.map(value => value * hourlyRate) : values;
    const maximum = Math.max(...displayValues, 1);
    const format = value => mode === 'money'
      ? new Intl.NumberFormat('ro-RO', {style:'currency', currency:'EUR', maximumFractionDigits:0}).format(value)
      : `${value.toLocaleString('ro-RO', {maximumFractionDigits:1})} h`;
    chart.innerHTML = displayValues.map((value, index) => `<div class="month-column" title="${months[index]}: ${format(value)}"><span class="month-value">${format(value)}</span><div class="month-bar-wrap"><div class="month-bar" style="height:${Math.max(1, value / maximum * 100)}%"></div></div><span class="month-label">${months[index]}</span></div>`).join('');
    document.querySelector('#annual-title').textContent = `${mode === 'money' ? 'Brut estimat' : 'Ore lucrate'} în ${selectedYear}`;
    document.querySelector('#annual-total').textContent = `Total: ${format(displayValues.reduce((sum, value) => sum + value, 0))}`;
    document.querySelector('#annual-note').classList.toggle('hidden-note', mode === 'hours');
  }

  document.addEventListener('DOMContentLoaded', () => {
    document.querySelector('#annual-year')?.addEventListener('change', renderAnnualChart);
    document.querySelectorAll('[data-chart-mode]').forEach(button => button.addEventListener('click', () => {
      mode = button.dataset.chartMode;
      document.querySelectorAll('[data-chart-mode]').forEach(item => item.classList.toggle('active', item === button));
      renderAnnualChart();
    }));
    const history = document.querySelector('#history-list');
    if (history) new MutationObserver(renderAnnualChart).observe(history, {childList:true});
    renderAnnualChart();
  });
})();

const state = {
  token: sessionStorage.getItem('roomly.token'),
  auth: {
    pendingEmail: ''
  },
  app: null,
  today: null,
  calendar: null,
  history: null,
  statistics: null,
  selectedDate: null,
  selectedView: 'today'
};

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const monthNames = ['Ianuarie', 'Februarie', 'Martie', 'Aprilie', 'Mai', 'Iunie', 'Iulie', 'August', 'Septembrie', 'Octombrie', 'Noiembrie', 'Decembrie'];

async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (!(options.body instanceof FormData)) headers['Content-Type'] = 'application/json';
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, { ...options, headers });
  if (!response.ok) {
    let body = {};
    try { body = await response.json(); } catch {}
    throw new Error(body.message || `Eroare ${response.status}`);
  }
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) return response.json();
  return response.blob();
}

function toast(message) {
  const el = $('#toast');
  el.textContent = message;
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 2200);
}

function money(value) {
  const amount = Number(value || 0);
  const currency = state.app?.me?.currency || 'EUR';
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency }).format(amount);
}

function hours(value) {
  return `${Number(value || 0).toLocaleString('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} h`;
}

function dateLabel(date) {
  return new Intl.DateTimeFormat('ro-RO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(`${date}T12:00:00`));
}

function formatShortDate(date) {
  return new Intl.DateTimeFormat('ro-RO', { day: '2-digit', month: 'short' }).format(new Date(`${date}T12:00:00`));
}

function formatTime(time) {
  return time ? time.slice(0, 5) : '—';
}

function isoToday() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

async function loadInitial() {
  state.app = await api('/api/app/bootstrap');
  state.today = await api('/api/work-entries/today');
  const now = new Date();
  state.selectedDate = state.selectedDate || isoToday();
  await Promise.all([
    loadCalendar(now.getFullYear(), now.getMonth() + 1),
    loadHistory(),
    loadStatistics()
  ]);
  renderApp();
}

async function loadCalendar(year, month) {
  state.calendar = await api(`/api/work-entries/calendar?year=${year}&month=${month}`);
  $('#calendar-year').value = String(year);
  $('#calendar-month').value = String(month);
}

async function loadHistory() {
  const year = $('#calendar-year')?.value || new Date().getFullYear();
  const month = $('#calendar-month')?.value || (new Date().getMonth() + 1);
  const workTypeId = $('#history-work-type')?.value || '';
  const query = new URLSearchParams({ year, month });
  if (workTypeId) query.set('workTypeId', workTypeId);
  state.history = await api(`/api/work-entries?${query.toString()}`);
}

async function loadStatistics() {
  const year = $('#stats-year')?.value || new Date().getFullYear();
  const month = $('#stats-month')?.value || '';
  const metric = $('#stats-metric')?.value || 'hours';
  const workTypeId = $('#stats-work-type')?.value || '';
  const summaryQuery = new URLSearchParams({ year });
  if (month) summaryQuery.set('month', month);
  if (workTypeId) summaryQuery.set('workTypeId', workTypeId);
  const monthlyQuery = new URLSearchParams({ year, metric });
  if (workTypeId) monthlyQuery.set('workTypeId', workTypeId);
  const [summary, monthly] = await Promise.all([
    api(`/api/statistics/summary?${summaryQuery.toString()}`),
    api(`/api/statistics/monthly?${monthlyQuery.toString()}`)
  ]);
  state.statistics = { summary, monthly };
}

function showApp() {
  $('#auth-screen').classList.add('hidden');
  $('#app').classList.remove('hidden');
}

function renderApp() {
  showApp();
  renderNavigation();
  renderToday();
  renderCalendarControls();
  renderCalendar();
  renderHistory();
  renderStatisticsControls();
  renderStatistics();
  renderProfile();
  renderWorkTypeOptions();
}

function renderNavigation() {
  const me = state.app.me;
  $('#sidebar-name').textContent = [me.firstName, me.lastName].filter(Boolean).join(' ') || me.email;
  $('#sidebar-email').textContent = me.email;
  navigate(state.selectedView);
}

function navigate(view) {
  state.selectedView = view;
  const titles = { today: 'Astăzi', calendar: 'Calendar', chat: 'Chat', statistics: 'Statistici', profile: 'Profil' };
  $('#topbar-title').textContent = titles[view];
  $$('.view').forEach(section => section.classList.toggle('active-view', section.id === `${view}-view`));
  $$('.nav-link').forEach(button => button.classList.toggle('active', button.dataset.view === view));
  $('.sidebar').classList.remove('open');
}

function renderToday() {
  const today = state.today;
  const me = state.app.me;
  $('#today-date-label').textContent = dateLabel(today.date).toUpperCase();
  $('#today-view h1').textContent = `Bun venit, ${me.firstName || me.email.split('@')[0]}`;
  $('#today-empty-copy').textContent = today.entries.length ? 'Activitățile tale de astăzi sunt deja salvate mai jos.' : 'Nu ai adăugat nicio activitate pentru astăzi.';
  $('#today-month-hours').textContent = hours(state.app.currentMonth.hoursWorked);
  $('#today-month-days').textContent = state.app.currentMonth.daysWorked;
  $('#today-month-gross').textContent = money(state.app.currentMonth.grossEstimated);
  $('#today-entries').innerHTML = today.entries.length
    ? today.entries.map(entryCard).join('')
    : '<div class="entry-card"><p class="muted">Nicio activitate pentru astăzi.</p></div>';
}

function entryCard(entry) {
  return `
    <article class="entry-card">
      <div class="entry-main">
        <div>
          <strong>${entry.workTypeName}</strong>
          <div class="entry-meta">
            <span>${formatShortDate(entry.date)}</span>
            <span>${formatTime(entry.startTime)} – ${formatTime(entry.endTime)}</span>
            <span>Pauză: ${entry.breakMinutes} min</span>
          </div>
        </div>
        <div>
          <strong>${hours(entry.hoursWorked)}</strong>
          <div class="muted">${money(entry.grossAmount)}</div>
        </div>
      </div>
      ${entry.notes ? `<p class="muted">${entry.notes}</p>` : ''}
      <div class="inline-actions">
        <button class="btn ghost small" data-edit-entry="${entry.id}">Editează</button>
        <button class="btn ghost small" data-duplicate-entry="${entry.id}">Duplică</button>
        <button class="btn ghost small" data-delete-entry="${entry.id}">Șterge</button>
      </div>
    </article>
  `;
}

function renderCalendarControls() {
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 6 }, (_, index) => currentYear - 2 + index);
  const yearOptions = years.map(year => `<option value="${year}">${year}</option>`).join('');
  $('#calendar-year').innerHTML = yearOptions;
  $('#stats-year').innerHTML = yearOptions;
  const monthOptions = monthNames.map((month, index) => `<option value="${index + 1}">${month}</option>`).join('');
  $('#calendar-month').innerHTML = monthOptions;
  $('#stats-month').innerHTML = `<option value="">Tot anul</option>${monthOptions}`;
}

function renderCalendar() {
  const selected = state.selectedDate;
  $('#calendar-grid').innerHTML = state.calendar.days.map(day => `
    <button class="calendar-cell ${day.date === isoToday() ? 'is-today' : ''} ${day.date === selected ? 'is-selected' : ''}" data-calendar-date="${day.date}">
      <header>
        <strong>${new Date(`${day.date}T12:00:00`).getDate()}</strong>
        ${day.entries.length ? `<span class="calendar-badge">${hours(day.totalHours)}</span>` : ''}
      </header>
      <div class="calendar-items">
        ${day.entries.slice(0, 3).map(entry => `<span class="calendar-pill" style="background:${entry.color}">${entry.workTypeCode}</span>`).join('')}
      </div>
    </button>
  `).join('');
  const selectedDay = state.calendar.days.find(day => day.date === selected) || state.calendar.days.find(day => day.date === isoToday()) || state.calendar.days[0];
  if (selectedDay) {
    state.selectedDate = selectedDay.date;
    $('#calendar-day-summary').textContent = `${dateLabel(selectedDay.date)} · ${selectedDay.entries.length} activități · ${hours(selectedDay.totalHours)} · ${money(selectedDay.totalGross)}`;
  }
}

function renderHistory() {
  $('#history-entries').innerHTML = state.history.entries.length
    ? state.history.entries.map(entryCard).join('')
    : '<div class="entry-card"><p class="muted">Nicio activitate în perioada selectată.</p></div>';
}

function renderStatisticsControls() {
  const workTypeOptions = ['<option value="">Toate activitățile</option>', ...state.app.workTypes.map(type => `<option value="${type.id}">${type.name}</option>`)].join('');
  $('#history-work-type').innerHTML = workTypeOptions;
  $('#stats-work-type').innerHTML = workTypeOptions;
  $('#compare-work-type').innerHTML = workTypeOptions;
  $('#compare-from-a').value = state.app.comparisonPreset.fromA;
  $('#compare-to-a').value = state.app.comparisonPreset.toA;
  $('#compare-from-b').value = state.app.comparisonPreset.fromB;
  $('#compare-to-b').value = state.app.comparisonPreset.toB;
}

function renderStatistics() {
  const summary = state.statistics.summary;
  $('#stats-hours').textContent = hours(summary.totalHoursWorked);
  $('#stats-days').textContent = summary.totalDaysWorked;
  $('#stats-gross').textContent = money(summary.totalGrossEstimated);
  $('#stats-average').textContent = hours(summary.averageHoursPerDay);
  const values = state.statistics.monthly.points.map(point => Number(point.value));
  const max = Math.max(...values, 1);
  $('#monthly-chart').innerHTML = state.statistics.monthly.points.map(point => `
    <div class="chart-bar-col">
      <span class="muted">${point.value}</span>
      <div class="chart-bar" style="height:${Math.max((Number(point.value) / max) * 180, 6)}px"></div>
      <small>${monthNames[point.month - 1].slice(0, 3)}</small>
    </div>
  `).join('');
}

function renderProfile() {
  const me = state.app.me;
  $('#profile-first').value = me.firstName || '';
  $('#profile-last').value = me.lastName || '';
  $('#profile-email').value = me.email || '';
  $('#profile-rate').value = me.hourlyRate || 0;
  $('#profile-currency').value = me.currency || 'EUR';
  $('#profile-break').value = String(me.defaultBreakMinutes ?? 30);
  $('#profile-language').value = me.language || 'ro';
  $('#work-types-list').innerHTML = state.app.workTypes.map(type => `
    <article class="entry-card">
      <div class="entry-main">
        <div>
          <strong>${type.name}</strong>
          <div class="entry-meta">
            <span>${type.code}</span>
            <span style="color:${type.color}">${type.color}</span>
            <span>${type.customHourlyRate ? money(type.customHourlyRate) : 'tarif general'}</span>
            <span>${type.defaultBreakMinutes} min</span>
          </div>
        </div>
        <div class="inline-actions">
          <button class="btn ghost small" data-edit-type="${type.id}">Editează</button>
          <button class="btn ghost small" data-deactivate-type="${type.id}">Dezactivează</button>
        </div>
      </div>
    </article>
  `).join('');
}

function renderWorkTypeOptions() {
  const options = state.app.workTypes.map(type => `<option value="${type.id}" data-rate="${type.customHourlyRate || ''}" data-break="${type.defaultBreakMinutes}" data-color="${type.color}">${type.name}</option>`).join('');
  $('#entry-work-type').innerHTML = options;
}

function openEntry(entry = null, date = isoToday()) {
  $('#entry-form').reset();
  $('#entry-error').textContent = '';
  $('#entry-id').value = entry?.id || '';
  $('#entry-title').textContent = entry ? 'Editează activitate' : 'Adaugă activitate';
  $('#entry-date').value = entry?.date || date;
  $('#entry-work-type').value = entry?.id ? state.app.workTypes.find(type => type.code === entry.workTypeCode)?.id : state.app.workTypes[0]?.id;
  $('#entry-start').value = entry?.startTime?.slice(0, 5) || '09:00';
  $('#entry-end').value = entry?.endTime?.slice(0, 5) || '17:30';
  $('#entry-break').value = String(entry?.breakMinutes ?? state.app.me.defaultBreakMinutes ?? 30);
  $('#entry-notes').value = entry?.notes || '';
  updateEntryPreview();
  $('#entry-dialog').showModal();
}

function updateEntryPreview() {
  const typeId = Number($('#entry-work-type').value);
  const type = state.app.workTypes.find(item => item.id === typeId);
  const start = $('#entry-start').value;
  const end = $('#entry-end').value;
  const breakMinutes = Number($('#entry-break').value || 0);
  let paidMinutes = 0;
  if (start && end) {
    const startMinutes = Number(start.slice(0, 2)) * 60 + Number(start.slice(3, 5));
    const endMinutes = Number(end.slice(0, 2)) * 60 + Number(end.slice(3, 5));
    let diff = endMinutes - startMinutes;
    if (diff <= 0) diff += 24 * 60;
    paidMinutes = Math.max(diff - breakMinutes, 0);
  }
  const workedHours = paidMinutes / 60;
  const rate = Number(type?.customHourlyRate || state.app.me.hourlyRate || 0);
  $('#entry-hours-preview').textContent = hours(workedHours);
  $('#entry-gross-preview').textContent = `${money(workedHours * rate)} brut estimat`;
}

async function downloadAuthenticated(url, filename) {
  const response = await fetch(url, {
    headers: state.token ? { Authorization: `Bearer ${state.token}` } : {}
  });
  if (!response.ok) {
    toast('Nu am putut genera exportul.');
    return;
  }
  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}

async function submitEntry(event) {
  event.preventDefault();
  $('#entry-error').textContent = '';
  const payload = {
    date: $('#entry-date').value,
    workTypeId: Number($('#entry-work-type').value),
    startTime: $('#entry-start').value,
    endTime: $('#entry-end').value,
    breakMinutes: Number($('#entry-break').value),
    notes: $('#entry-notes').value.trim() || null
  };
  try {
    const id = $('#entry-id').value;
    if (id) {
      await api(`/api/work-entries/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await api('/api/work-entries', { method: 'POST', body: JSON.stringify(payload) });
    }
    $('#entry-dialog').close();
    await loadInitial();
    toast('Activitatea a fost salvată.');
  } catch (error) {
    $('#entry-error').textContent = error.message;
  }
}

async function submitRegister(event) {
  event.preventDefault();
  $('#register-error').textContent = '';
  try {
    const response = await api('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        email: $('#register-email').value.trim(),
        password: $('#register-password').value,
        confirmPassword: $('#register-confirm-password').value
      })
    });
    state.auth.pendingEmail = response.email;
    if (response.demoCode) {
      $('#register-demo-code').textContent = response.demoCode;
      $('#register-code-box').classList.remove('hidden');
      $('#confirm-code').value = response.demoCode;
    }
    $('#confirm-dialog').showModal();
    toast('Cont creat. Confirmă codul primit pe email.');
  } catch (error) {
    $('#register-error').textContent = error.message;
  }
}

async function submitConfirm(event) {
  event.preventDefault();
  $('#confirm-error').textContent = '';
  try {
    await api('/api/auth/register/confirm', {
      method: 'POST',
      body: JSON.stringify({ code: $('#confirm-code').value.trim() })
    });
    $('#confirm-dialog').close();
    $('#register-dialog').close();
    $('#login').value = state.auth.pendingEmail;
    $('#password').value = '';
    toast('Email confirmat. Te poți autentifica.');
  } catch (error) {
    $('#confirm-error').textContent = error.message;
  }
}

async function submitResetRequest() {
  $('#reset-error').textContent = '';
  try {
    const response = await api('/api/auth/password-reset/request', {
      method: 'POST',
      body: JSON.stringify({ login: $('#reset-login').value.trim() })
    });
    if (response.demoCode) {
      $('#reset-demo-code').textContent = response.demoCode;
      $('#reset-code-box').classList.remove('hidden');
      $('#reset-code').value = response.demoCode;
    }
    toast('Codul de resetare a fost trimis.');
  } catch (error) {
    $('#reset-error').textContent = error.message;
  }
}

async function submitReset(event) {
  event.preventDefault();
  $('#reset-error').textContent = '';
  try {
    await api('/api/auth/password-reset/confirm', {
      method: 'POST',
      body: JSON.stringify({ code: $('#reset-code').value.trim(), newPassword: $('#reset-password').value })
    });
    $('#reset-dialog').close();
    toast('Parola a fost schimbată.');
  } catch (error) {
    $('#reset-error').textContent = error.message;
  }
}

async function submitLogin(event) {
  event.preventDefault();
  $('#login-error').textContent = '';
  try {
    const response = await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ login: $('#login').value.trim(), password: $('#password').value })
    });
    state.token = response.token;
    sessionStorage.setItem('roomly.token', response.token);
    await loadInitial();
    if (!response.onboardingComplete) openOnboarding();
  } catch (error) {
    sessionStorage.removeItem('roomly.token');
    state.token = null;
    $('#login-error').textContent = error.message;
  }
}

function openOnboarding() {
  $('#onboarding-first').value = state.app.me.firstName || '';
  $('#onboarding-last').value = state.app.me.lastName || '';
  $('#onboarding-rate').value = state.app.me.hourlyRate || 16;
  $('#onboarding-currency').value = state.app.me.currency || 'EUR';
  $('#onboarding-break').value = String(state.app.me.defaultBreakMinutes ?? 30);
  $('#onboarding-language').value = state.app.me.language || 'ro';
  $('#onboarding-dialog').showModal();
}

async function submitOnboarding(event) {
  event.preventDefault();
  $('#onboarding-error').textContent = '';
  const payload = {
    firstName: $('#onboarding-first').value.trim(),
    lastName: $('#onboarding-last').value.trim(),
    defaultHourlyRate: Number($('#onboarding-rate').value),
    currency: $('#onboarding-currency').value,
    defaultBreakMinutes: Number($('#onboarding-break').value),
    language: $('#onboarding-language').value
  };
  try {
    await api('/api/me/profile', { method: 'PUT', body: JSON.stringify(payload) });
    $('#onboarding-dialog').close();
    await loadInitial();
    toast('Profilul a fost salvat.');
  } catch (error) {
    $('#onboarding-error').textContent = error.message;
  }
}

async function submitProfile(event) {
  event.preventDefault();
  $('#profile-error').textContent = '';
  try {
    await api('/api/me/profile', {
      method: 'PUT',
      body: JSON.stringify({
        firstName: $('#profile-first').value.trim(),
        lastName: $('#profile-last').value.trim(),
        defaultHourlyRate: Number($('#profile-rate').value),
        currency: $('#profile-currency').value,
        defaultBreakMinutes: Number($('#profile-break').value),
        language: $('#profile-language').value
      })
    });
    await loadInitial();
    toast('Profilul a fost actualizat.');
  } catch (error) {
    $('#profile-error').textContent = error.message;
  }
}

async function submitWorkType(event) {
  event.preventDefault();
  const id = $('#work-type-id').value;
  const payload = {
    name: $('#work-type-name').value.trim(),
    code: $('#work-type-code').value.trim().toUpperCase(),
    color: $('#work-type-color').value,
    active: true,
    customHourlyRate: $('#work-type-rate').value ? Number($('#work-type-rate').value) : null,
    defaultBreakMinutes: Number($('#work-type-break').value)
  };
  if (id) {
    await api(`/api/work-types/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
  } else {
    await api('/api/work-types', { method: 'POST', body: JSON.stringify(payload) });
  }
  resetWorkTypeForm();
  await loadInitial();
  toast('Tipul de activitate a fost salvat.');
}

function resetWorkTypeForm() {
  $('#work-type-form').reset();
  $('#work-type-id').value = '';
  $('#work-type-color').value = '#3B82F6';
}

async function runComparison() {
  const query = new URLSearchParams({
    fromA: $('#compare-from-a').value,
    toA: $('#compare-to-a').value,
    fromB: $('#compare-from-b').value,
    toB: $('#compare-to-b').value,
    metric: $('#compare-metric').value
  });
  if ($('#compare-work-type').value) query.set('workTypeId', $('#compare-work-type').value);
  const result = await api(`/api/statistics/compare?${query.toString()}`);
  $('#compare-total-a').textContent = result.metric === 'gross' ? money(result.totalA) : result.metric === 'days' ? result.totalA : hours(result.totalA);
  $('#compare-total-b').textContent = result.metric === 'gross' ? money(result.totalB) : result.metric === 'days' ? result.totalB : hours(result.totalB);
  $('#compare-diff').textContent = result.metric === 'gross' ? money(result.differenceAbsolute) : result.metric === 'days' ? result.differenceAbsolute : hours(result.differenceAbsolute);
  $('#compare-percent').textContent = `${result.differencePercent}%`;
}

function bindEvents() {
  $('#login-form').addEventListener('submit', submitLogin);
  $('#register-form').addEventListener('submit', submitRegister);
  $('#confirm-form').addEventListener('submit', submitConfirm);
  $('#reset-form').addEventListener('submit', submitReset);
  $('#request-reset-code').addEventListener('click', submitResetRequest);
  $('#onboarding-form').addEventListener('submit', submitOnboarding);
  $('#profile-form').addEventListener('submit', submitProfile);
  $('#password-form').addEventListener('submit', async event => {
    event.preventDefault();
    $('#password-error').textContent = '';
    try {
      await api('/api/me/password', {
        method: 'PUT',
        body: JSON.stringify({
          currentPassword: $('#current-password').value,
          newPassword: $('#new-password').value
        })
      });
      $('#password-form').reset();
      toast('Parola a fost schimbată.');
    } catch (error) {
      $('#password-error').textContent = error.message;
    }
  });
  $('#work-type-form').addEventListener('submit', submitWorkType);
  $('#entry-form').addEventListener('submit', submitEntry);
  $('#open-entry').addEventListener('click', () => openEntry());
  $('#today-add-button').addEventListener('click', () => openEntry());
  $('#close-entry').addEventListener('click', () => $('#entry-dialog').close());
  $('#cancel-entry').addEventListener('click', () => $('#entry-dialog').close());
  $('#open-register').addEventListener('click', () => $('#register-dialog').showModal());
  $('#close-register').addEventListener('click', () => $('#register-dialog').close());
  $('#cancel-register').addEventListener('click', () => $('#register-dialog').close());
  $('#close-confirm').addEventListener('click', () => $('#confirm-dialog').close());
  $('#open-reset').addEventListener('click', () => { $('#reset-login').value = $('#login').value.trim(); $('#reset-dialog').showModal(); });
  $('#close-reset').addEventListener('click', () => $('#reset-dialog').close());
  $('#logout').addEventListener('click', () => { sessionStorage.clear(); location.reload(); });
  $('#menu-toggle').addEventListener('click', () => $('.sidebar').classList.toggle('open'));
  $$('.nav-link').forEach(button => button.addEventListener('click', () => navigate(button.dataset.view)));
  ['#entry-start', '#entry-end', '#entry-break', '#entry-work-type'].forEach(selector => $(selector).addEventListener('input', updateEntryPreview));
  $('#calendar-month').addEventListener('change', async () => { await loadCalendar(Number($('#calendar-year').value), Number($('#calendar-month').value)); await loadHistory(); renderCalendar(); renderHistory(); });
  $('#calendar-year').addEventListener('change', async () => { await loadCalendar(Number($('#calendar-year').value), Number($('#calendar-month').value)); await loadHistory(); renderCalendar(); renderHistory(); });
  $('#history-work-type').addEventListener('change', async () => { await loadHistory(); renderHistory(); });
  $('#stats-year').addEventListener('change', async () => { await loadStatistics(); renderStatistics(); });
  $('#stats-month').addEventListener('change', async () => { await loadStatistics(); renderStatistics(); });
  $('#stats-work-type').addEventListener('change', async () => { await loadStatistics(); renderStatistics(); });
  $('#stats-metric').addEventListener('change', async () => { await loadStatistics(); renderStatistics(); });
  $('#run-compare').addEventListener('click', runComparison);
  $('#reset-work-type').addEventListener('click', resetWorkTypeForm);
  $('#delete-account').addEventListener('click', async () => {
    if (!confirm('Sigur vrei să dezactivezi contul?')) return;
    await api('/api/me', { method: 'DELETE' });
    sessionStorage.clear();
    location.reload();
  });
  $('#calendar-grid').addEventListener('click', async event => {
    const button = event.target.closest('[data-calendar-date]');
    if (!button) return;
    state.selectedDate = button.dataset.calendarDate;
    renderCalendar();
    state.history = await api(`/api/work-entries?from=${state.selectedDate}&to=${state.selectedDate}${$('#history-work-type').value ? `&workTypeId=${$('#history-work-type').value}` : ''}`);
    renderHistory();
  });
  document.body.addEventListener('click', async event => {
    const editEntry = event.target.closest('[data-edit-entry]');
    if (editEntry) {
      const allEntries = [...state.today.entries, ...state.history.entries];
      const entry = allEntries.find(item => String(item.id) === editEntry.dataset.editEntry);
      if (entry) openEntry(entry, entry.date);
      return;
    }
    const duplicateEntry = event.target.closest('[data-duplicate-entry]');
    if (duplicateEntry) {
      await api(`/api/work-entries/${duplicateEntry.dataset.duplicateEntry}/duplicate`, { method: 'POST', body: JSON.stringify({ date: isoToday() }) });
      await loadInitial();
      toast('Activitatea a fost duplicată pentru astăzi.');
      return;
    }
    const deleteEntry = event.target.closest('[data-delete-entry]');
    if (deleteEntry) {
      if (!confirm('Sigur vrei să ștergi această activitate?')) return;
      await api(`/api/work-entries/${deleteEntry.dataset.deleteEntry}`, { method: 'DELETE' });
      await loadInitial();
      toast('Activitatea a fost ștearsă.');
      return;
    }
    const editType = event.target.closest('[data-edit-type]');
    if (editType) {
      const type = state.app.workTypes.find(item => String(item.id) === editType.dataset.editType);
      if (!type) return;
      $('#work-type-id').value = type.id;
      $('#work-type-name').value = type.name;
      $('#work-type-code').value = type.code;
      $('#work-type-color').value = type.color;
      $('#work-type-rate').value = type.customHourlyRate || '';
      $('#work-type-break').value = String(type.defaultBreakMinutes ?? 30);
      navigate('profile');
      return;
    }
    const deactivateType = event.target.closest('[data-deactivate-type]');
    if (deactivateType) {
      await api(`/api/work-types/${deactivateType.dataset.deactivateType}/deactivate`, { method: 'PUT' });
      await loadInitial();
      toast('Tipul de activitate a fost dezactivat.');
    }
  });

  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;
  $('#export-month-csv').addEventListener('click', () => downloadAuthenticated(`/api/export/csv?year=${currentYear}&month=${currentMonth}`, 'roomly-month.csv'));
  $('#export-year-csv').addEventListener('click', () => downloadAuthenticated(`/api/export/csv?year=${currentYear}`, 'roomly-year.csv'));
  $('#export-month-xlsx').addEventListener('click', () => downloadAuthenticated(`/api/export/excel?year=${currentYear}&month=${currentMonth}`, 'roomly-month.xlsx'));
  $('#export-year-xlsx').addEventListener('click', () => downloadAuthenticated(`/api/export/excel?year=${currentYear}`, 'roomly-year.xlsx'));
}

bindEvents();

if ('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js');
if (state.token) {
  loadInitial().catch(() => {
    sessionStorage.clear();
    state.token = null;
  });
}

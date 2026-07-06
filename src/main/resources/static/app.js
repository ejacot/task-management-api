const state = { credentials: sessionStorage.getItem("taskflow.credentials"), username: sessionStorage.getItem("taskflow.username"), tasks: [], filter: "" };
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.credentials) headers.Authorization = `Basic ${state.credentials}`;
  const response = await fetch(path, { ...options, headers });
  if (response.status === 401 && path !== "/api/tasks") logout();
  if (!response.ok) {
    let body = {};
    try { body = await response.json(); } catch (_) { /* empty response */ }
    const details = body.fields ? Object.values(body.fields).join(" · ") : "";
    throw new Error(details || body.message || `Cererea a eșuat (${response.status})`);
  }
  return response.status === 204 ? null : response.json();
}

function setSession(username, password) {
  state.username = username.toLowerCase();
  state.credentials = btoa(`${state.username}:${password}`);
  sessionStorage.setItem("taskflow.username", state.username);
  sessionStorage.setItem("taskflow.credentials", state.credentials);
}

function logout() {
  state.credentials = null; state.username = null; state.tasks = [];
  sessionStorage.clear();
  $("#app-view").classList.add("hidden"); $("#user-area").classList.add("hidden"); $("#auth-view").classList.remove("hidden");
}

async function login(username, password) {
  setSession(username, password);
  try { await loadTasks(); showApp(); }
  catch (error) { logout(); throw new Error("Utilizatorul sau parola nu sunt corecte."); }
}

function showApp() {
  $("#auth-view").classList.add("hidden"); $("#app-view").classList.remove("hidden"); $("#user-area").classList.remove("hidden");
  $("#user-label").textContent = `@${state.username}`;
  render();
}

async function loadTasks() {
  const suffix = state.filter ? `?status=${state.filter}&size=100&sort=createdAt,desc` : "?size=100&sort=createdAt,desc";
  const page = await api(`/api/tasks${suffix}`);
  state.tasks = page.content;
  render();
}

function render() {
  const labels = { TODO: "De făcut", IN_PROGRESS: "În lucru", DONE: "Finalizat", LOW: "Scăzută", MEDIUM: "Medie", HIGH: "Ridicată" };
  $("#task-list").innerHTML = state.tasks.map(task => `
    <article class="task-item status-${task.status}">
      <button class="status-dot" data-toggle="${task.id}" title="Schimbă statusul" aria-label="Schimbă statusul"></button>
      <div class="task-content">
        <h3>${escapeHtml(task.title)}</h3>
        <div class="task-meta"><span class="priority priority-${task.priority}">${labels[task.priority]}</span>${task.dueDate ? `<span>Termen ${formatDate(task.dueDate)}</span>` : ""}<span>${labels[task.status]}</span></div>
      </div>
      <div class="task-actions"><button class="icon-button" data-edit="${task.id}" title="Editează">✎</button><button class="icon-button" data-delete="${task.id}" title="Șterge">×</button></div>
    </article>`).join("");
  $("#empty-state").classList.toggle("hidden", state.tasks.length > 0);
  $("#task-count").textContent = `${state.tasks.length} ${state.tasks.length === 1 ? "task" : "task-uri"}`;
  $("#stat-total").textContent = state.tasks.length;
  $("#stat-todo").textContent = state.tasks.filter(t => t.status === "TODO").length;
  $("#stat-progress").textContent = state.tasks.filter(t => t.status === "IN_PROGRESS").length;
  $("#stat-done").textContent = state.tasks.filter(t => t.status === "DONE").length;
}

function escapeHtml(value) { const div = document.createElement("div"); div.textContent = value; return div.innerHTML; }
function formatDate(value) { return new Intl.DateTimeFormat("ro-RO", { day:"numeric", month:"short" }).format(new Date(`${value}T12:00:00`)); }
function toast(message) { const element = $("#toast"); element.textContent = message; element.classList.add("show"); setTimeout(() => element.classList.remove("show"), 2300); }

function openTask(task = null) {
  $("#task-form").reset(); $("#task-error").textContent = "";
  $("#task-id").value = task?.id || ""; $("#task-title").value = task?.title || ""; $("#task-description").value = task?.description || "";
  $("#task-priority").value = task?.priority || "MEDIUM"; $("#task-status").value = task?.status || "TODO"; $("#task-due-date").value = task?.dueDate || "";
  $("#task-status").closest("div").classList.toggle("hidden", !task); $("#dialog-title").textContent = task ? "Editează task-ul" : "Task nou";
  $("#task-dialog").showModal(); $("#task-title").focus();
}

$$('.tab').forEach(tab => tab.addEventListener("click", () => {
  $$('.tab').forEach(item => item.classList.toggle("active", item === tab));
  const register = tab.dataset.mode === "register";
  $("#auth-title").textContent = register ? "Creează-ți contul" : "Bine ai revenit";
  $("#auth-subtitle").textContent = register ? "Durează mai puțin de un minut." : "Intră în cont pentru a-ți vedea task-urile.";
  $("#auth-submit-label").textContent = register ? "Creează cont" : "Intră în cont";
  $("#password").autocomplete = register ? "new-password" : "current-password";
  $("#auth-error").textContent = "";
}));

$("#auth-form").addEventListener("submit", async event => {
  event.preventDefault(); const username = $("#username").value.trim(); const password = $("#password").value; const register = $(".tab.active").dataset.mode === "register";
  $("#auth-error").textContent = "";
  try { if (register) await api("/api/auth/register", { method:"POST", body:JSON.stringify({ username, password }) }); await login(username, password); toast(register ? "Cont creat. Bine ai venit!" : "Bine ai revenit!"); }
  catch (error) { $("#auth-error").textContent = error.message; }
});

$("#logout-button").addEventListener("click", logout);
$("#new-task-button").addEventListener("click", () => openTask());
$("#close-dialog").addEventListener("click", () => $("#task-dialog").close());
$("#cancel-task").addEventListener("click", () => $("#task-dialog").close());

$("#task-form").addEventListener("submit", async event => {
  event.preventDefault(); const id = $("#task-id").value;
  const data = { title:$("#task-title").value, description:$("#task-description").value || null, priority:$("#task-priority").value, dueDate:$("#task-due-date").value || null };
  if (id) data.status = $("#task-status").value;
  try { await api(id ? `/api/tasks/${id}` : "/api/tasks", { method:id ? "PUT" : "POST", body:JSON.stringify(data) }); $("#task-dialog").close(); await loadTasks(); toast(id ? "Task actualizat" : "Task adăugat"); }
  catch (error) { $("#task-error").textContent = error.message; }
});

$("#task-list").addEventListener("click", async event => {
  const editId = event.target.dataset.edit, deleteId = event.target.dataset.delete, toggleId = event.target.dataset.toggle;
  if (editId) openTask(state.tasks.find(t => t.id === Number(editId)));
  if (deleteId && confirm("Ștergi acest task?")) { await api(`/api/tasks/${deleteId}`, { method:"DELETE" }); await loadTasks(); toast("Task șters"); }
  if (toggleId) { const task = state.tasks.find(t => t.id === Number(toggleId)); const next = { TODO:"IN_PROGRESS", IN_PROGRESS:"DONE", DONE:"TODO" }[task.status]; await api(`/api/tasks/${task.id}`, { method:"PUT", body:JSON.stringify({ ...task, status:next }) }); await loadTasks(); }
});

$$('.filter').forEach(button => button.addEventListener("click", async () => { $$('.filter').forEach(item => item.classList.toggle("active", item === button)); state.filter = button.dataset.status; await loadTasks(); }));

if (state.credentials) loadTasks().then(showApp).catch(logout);

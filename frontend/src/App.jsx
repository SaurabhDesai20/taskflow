import { useEffect, useState } from "react";
import { api, setToken } from "./api";
import { pct, isOverdue, todayStr } from "./utils";

const PRIORITIES = [["LOW", "Low"], ["MEDIUM", "Medium"], ["HIGH", "High"]];
const label = (p) => PRIORITIES.find(([v]) => v === p)?.[1] ?? p;
const fmt = (d) => new Date(d + "T00:00:00").toLocaleDateString(undefined, { day: "numeric", month: "short" });

function Brand() {
  return <div className="brand"><span className="mark" aria-hidden="true" />TaskFlow</div>;
}

function Login({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState("");

  async function submit(e) {
    e.preventDefault();
    setErr("");
    try {
      if (mode === "register") await api("/api/auth/register", { method: "POST", body: { username, password } });
      const r = await api("/api/auth/login", { method: "POST", body: { username, password } });
      setToken(r.token);
      onLogin({ username: r.username, role: r.role });
    } catch (ex) {
      setErr(ex.status === 409 ? "That username is taken. Try another one."
        : ex.status === 401 ? "Wrong username or password."
        : ex.status === 400 ? "Use a username of 3+ characters and a password of 6+."
        : "Can't reach the server. Is the backend running?");
    }
  }

  return (
    <div className="auth">
      <Brand />
      <p className="lead">Keep your team's work in one place.</p>
      <form className="panel" onSubmit={submit}>
        <div className="tabs" role="tablist">
          <button type="button" role="tab" aria-selected={mode === "login"} onClick={() => setMode("login")}>Sign in</button>
          <button type="button" role="tab" aria-selected={mode === "register"} onClick={() => setMode("register")}>Create account</button>
        </div>
        <label className="field">Username
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
        </label>
        <label className="field">Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
            autoComplete={mode === "login" ? "current-password" : "new-password"} />
        </label>
        <button className="primary block" type="submit">{mode === "login" ? "Sign in" : "Create account"}</button>
        {err && <p className="msg" role="alert">{err}</p>}
      </form>
    </div>
  );
}

function Dashboard({ user, onLogout }) {
  const [tasks, setTasks] = useState([]);
  const [summary, setSummary] = useState(null);
  const [perUser, setPerUser] = useState([]);
  const [title, setTitle] = useState("");
  const [priority, setPriority] = useState("MEDIUM");
  const [dueDate, setDueDate] = useState("");
  const [err, setErr] = useState("");
  const admin = user.role === "ADMIN";

  async function load() {
    try {
      setTasks(await api("/api/tasks?size=50"));
      setSummary(await api("/reports/summary").catch(() => null)); // reports service may be down
      if (admin) setPerUser(await api("/reports/per-user").catch(() => []));
      setErr("");
    } catch (e) {
      if (e.status === 401) onLogout();
      else setErr("Could not load tasks.");
    }
  }
  useEffect(() => { load(); }, []);

  async function add(e) {
    e.preventDefault();
    if (!title.trim()) return;
    try {
      await api("/api/tasks", { method: "POST", body: { title, priority, dueDate: dueDate || undefined } });
      setTitle(""); setPriority("MEDIUM"); setDueDate("");
      load();
    } catch (ex) {
      setErr(ex.status === 400 ? "Check the task: it needs a title, and the due date can't be in the past." : "Could not add the task.");
    }
  }
  const complete = async (id) => { await api(`/api/tasks/${id}/complete`, { method: "PUT" }); load(); };
  const remove = async (id) => { await api(`/api/tasks/${id}`, { method: "DELETE" }); load(); };

  const done = summary?.counts?.DONE ?? 0;
  const pending = summary?.counts?.PENDING ?? 0;
  const total = done + pending;
  const late = tasks.filter((t) => isOverdue(t)).length;

  return (
    <>
      <header className="top">
        <Brand />
        <div className="who">
          <span className="avatar" aria-hidden="true">{user.username[0].toUpperCase()}</span>
          <span>{user.username}<small>{admin ? "Administrator" : "Member"}</small></span>
          <button className="linkbtn" onClick={onLogout}>Sign out</button>
        </div>
      </header>

      <form className="panel add" onSubmit={add}>
        <input className="title" placeholder="What needs doing?" aria-label="Task title" value={title} onChange={(e) => setTitle(e.target.value)} />
        <div className="opts">
          <div className="seg" role="group" aria-label="Priority">
            {PRIORITIES.map(([v, l]) => (
              <button type="button" key={v} aria-pressed={priority === v} onClick={() => setPriority(v)}>{l}</button>
            ))}
          </div>
          <input type="date" aria-label="Due date" min={todayStr()} value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          <span className="spacer" />
          <button className="primary" type="submit">Add task</button>
        </div>
        {err && <p className="msg" role="alert">{err}</p>}
      </form>

      <section className="panel">
        <div className="progress-head">
          <h2 style={{ margin: 0 }}>Progress</h2>
          {summary ? <span className="muted">{done} of {total} done{summary.cached ? " (cached)" : ""}</span>
            : <span className="muted">Report unavailable right now</span>}
        </div>
        <div className="bar" role="progressbar" aria-valuenow={pct(done, total)} aria-valuemin="0" aria-valuemax="100">
          <i style={{ width: `${pct(done, total)}%` }} />
        </div>
        {late > 0 && <p className="late-note" style={{ margin: "10px 0 0" }}>{late} overdue {late === 1 ? "task" : "tasks"}</p>}
      </section>

      <section className="panel" style={{ padding: "6px 12px" }}>
        {tasks.length === 0 ? <p className="empty">No tasks yet. Add your first one above.</p> : (
          <ul className="tasks">
            {tasks.map((t) => (
              <li key={t.id} className={`task ${t.priority} ${t.status === "DONE" ? "done" : ""}`}>
                <button className="check" disabled={t.status === "DONE"} onClick={() => complete(t.id)}
                  aria-label={t.status === "DONE" ? `${t.title} is done` : `Mark ${t.title} as done`}>
                  {t.status === "DONE" ? "✓" : ""}
                </button>
                <div className="body">
                  <span className="title">{t.title}</span>
                  <span className="meta">
                    <span className={`prio ${t.priority}`}>{label(t.priority)} priority</span>
                    {t.dueDate && <span className={isOverdue(t) ? "due late" : "due"}>{isOverdue(t) ? "Overdue since " : "Due "}{fmt(t.dueDate)}</span>}
                    {admin && <span>User #{t.userId}</span>}
                  </span>
                </div>
                <button className="remove" onClick={() => remove(t.id)} aria-label={`Remove ${t.title}`}>Remove</button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {admin && perUser.length > 0 && (
        <section className="panel">
          <h2>Tasks per user</h2>
          <table>
            <thead><tr><th>User</th><th>Done</th><th>Total</th></tr></thead>
            <tbody>{perUser.map((r) => <tr key={r.username}><td>{r.username}</td><td>{r.done}</td><td>{r.total}</td></tr>)}</tbody>
          </table>
        </section>
      )}
    </>
  );
}

export default function App() {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user") || "null"));
  const login = (u) => { localStorage.setItem("user", JSON.stringify(u)); setUser(u); };
  const logout = () => { setToken(null); localStorage.removeItem("user"); setUser(null); };
  return <main>{user ? <Dashboard user={user} onLogout={logout} /> : <Login onLogin={login} />}</main>;
}
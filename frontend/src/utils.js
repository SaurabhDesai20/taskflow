export const pct = (done, total) => (total === 0 ? 0 : Math.round((done / total) * 100));

// Today as YYYY-MM-DD in the user's local timezone ("en-CA" happens to format dates that way)
export const todayStr = () => new Date().toLocaleDateString("en-CA");

// ISO dates (YYYY-MM-DD) sort alphabetically in date order, so a plain < comparison works
export const isOverdue = (task, today = todayStr()) =>
  task.status !== "DONE" && !!task.dueDate && task.dueDate < today;
import { describe, expect, it } from "vitest";
import { isOverdue, pct } from "./utils";

describe("pct", () => {
  it("returns 0 when there are no tasks", () => expect(pct(0, 0)).toBe(0));
  it("rounds to a whole percent", () => expect(pct(1, 3)).toBe(33));
});

describe("isOverdue", () => {
  const today = "2026-11-12"; // passed in, so the test never depends on the real date
  it("is true for a pending task due yesterday", () =>
    expect(isOverdue({ status: "PENDING", dueDate: "2026-11-11" }, today)).toBe(true));
  it("is false when due today", () =>
    expect(isOverdue({ status: "PENDING", dueDate: "2026-11-12" }, today)).toBe(false));
  it("is false when the task is done", () =>
    expect(isOverdue({ status: "DONE", dueDate: "2026-11-01" }, today)).toBe(false));
  it("is false when there is no due date", () =>
    expect(isOverdue({ status: "PENDING", dueDate: null }, today)).toBe(false));
});
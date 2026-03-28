import { useCallback, useEffect, useMemo, useState } from "react";
import type { GoalItem, PlanItem, TaskItem, TodayTasksResponse } from "../api";
import {
  archiveInboxItem,
  completeTask,
  createInboxItem,
  deleteInboxItem,
  dismissTaskReminder,
  getTodayTasks,
  listGoals,
  listPlans,
  listTasks,
  snoozeTask,
  updateTask,
} from "../api";

type ViewMode = "today" | "inbox" | "all";

function statusLabel(status: string) {
  if (status === "todo") return "待办";
  if (status === "in_progress") return "进行中";
  if (status === "done") return "已完成";
  if (status === "blocked") return "阻塞";
  if (status === "cancelled") return "取消";
  return status;
}

function formatTime(value: string | null | undefined) {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

function toLocalInputValue(iso: string | null) {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  const yyyy = d.getFullYear();
  const mm = pad(d.getMonth() + 1);
  const dd = pad(d.getDate());
  const hh = pad(d.getHours());
  const mi = pad(d.getMinutes());
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
}

function fromLocalInputValue(value: string) {
  if (!value.trim()) return null;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return null;
  return d.toISOString();
}

export default function TaskPage() {
  const [mode, setMode] = useState<ViewMode>("today");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [page, setPage] = useState(1);
  const [pageSize] = useState(30);
  const [statusFilter, setStatusFilter] = useState<string>(""); // all when blank

  const [items, setItems] = useState<TaskItem[]>([]);
  const [total, setTotal] = useState(0);
  const [today, setToday] = useState<TodayTasksResponse | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const selected = useMemo(() => items.find((x) => x.id === selectedId) ?? null, [items, selectedId]);

  const [goalOptions, setGoalOptions] = useState<GoalItem[]>([]);
  const [planOptions, setPlanOptions] = useState<PlanItem[]>([]);

  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editStatus, setEditStatus] = useState("todo");
  const [editPriority, setEditPriority] = useState(0);
  const [editInbox, setEditInbox] = useState(false);
  const [editGoalId, setEditGoalId] = useState("");
  const [editPlanId, setEditPlanId] = useState("");
  const [editDueAt, setEditDueAt] = useState("");
  const [editRemindAt, setEditRemindAt] = useState("");
  const [editSnoozeUntil, setEditSnoozeUntil] = useState("");

  const [inboxTitle, setInboxTitle] = useState("");
  const [inboxDesc, setInboxDesc] = useState("");

  const loadGoalsAndPlans = useCallback(async () => {
    const goalsRes = await listGoals({ status: "active" }, 1, 100);
    setGoalOptions(goalsRes.items);
    const plansRes = await listPlans({ status: "active" }, 1, 200);
    setPlanOptions(plansRes.items);
  }, []);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (mode === "today") {
        const data = await getTodayTasks();
        setToday(data);
        const merged = [...data.todayDue, ...data.overdue, ...data.upcomingReminders];
        setItems(merged);
        setTotal(merged.length);
        setSelectedId(merged[0]?.id ?? null);
        return;
      }

      const inbox = mode === "inbox" ? true : null;
      const res = await listTasks(
        {
          inbox,
          status: statusFilter ? statusFilter : null,
        },
        page,
        pageSize,
      );
      setToday(null);
      setItems(res.items);
      setTotal(res.total);
      setSelectedId(res.items[0]?.id ?? null);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [mode, page, pageSize, statusFilter]);

  useEffect(() => {
    void loadGoalsAndPlans();
  }, [loadGoalsAndPlans]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  useEffect(() => {
    if (!selected) return;
    setEditTitle(selected.title ?? "");
    setEditDescription(selected.description ?? "");
    setEditStatus(selected.status ?? "todo");
    setEditPriority(selected.priority ?? 0);
    setEditInbox(!!selected.inbox);
    setEditGoalId(selected.goalId ?? "");
    setEditPlanId(selected.planId ?? "");
    setEditDueAt(toLocalInputValue(selected.dueAt ?? null));
    setEditRemindAt(toLocalInputValue(selected.remindAt ?? null));
    setEditSnoozeUntil(toLocalInputValue(selected.snoozeUntil ?? null));
  }, [selectedId, selected]);

  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  async function onSaveTask() {
    if (!selected) return;
    if (!editTitle.trim()) {
      setError("标题不能为空");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await updateTask(selected.id, {
        title: editTitle.trim(),
        description: editDescription.trim() ? editDescription.trim() : null,
        status: editStatus,
        priority: editPriority,
        inbox: editInbox,
        goalId: editGoalId.trim() ? editGoalId.trim() : null,
        planId: editPlanId.trim() ? editPlanId.trim() : null,
        dueAt: fromLocalInputValue(editDueAt),
        remindAt: fromLocalInputValue(editRemindAt),
        snoozeUntil: fromLocalInputValue(editSnoozeUntil),
      });
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onCompleteTask() {
    if (!selected) return;
    setLoading(true);
    setError(null);
    try {
      await completeTask(selected.id);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onSnoozeTask() {
    if (!selected) return;
    const iso = fromLocalInputValue(editSnoozeUntil);
    if (!iso) {
      setError("请先填写 snoozeUntil");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await snoozeTask(selected.id, iso);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onDismissReminder() {
    if (!selected) return;
    setLoading(true);
    setError(null);
    try {
      await dismissTaskReminder(selected.id);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onCreateInbox() {
    if (!inboxTitle.trim()) {
      setError("请输入收件箱任务标题");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await createInboxItem({ title: inboxTitle.trim(), description: inboxDesc.trim() ? inboxDesc.trim() : null });
      setInboxTitle("");
      setInboxDesc("");
      setMode("inbox");
      setPage(1);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onArchiveInbox() {
    if (!selected) return;
    setLoading(true);
    setError(null);
    try {
      await archiveInboxItem(selected.id, {
        goalId: editGoalId.trim() ? editGoalId.trim() : null,
        planId: editPlanId.trim() ? editPlanId.trim() : null,
      });
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onDeleteInbox() {
    if (!selected) return;
    setLoading(true);
    setError(null);
    try {
      await deleteInboxItem(selected.id);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="taskBoard">
      <div className="taskLeft">
        <div className="taskLeftHeader">
          <div>
            <h1>任务/计划</h1>
            <div className="muted">Today / Inbox / 列表管理</div>
          </div>
          <div className="taskTabs">
            <button className={mode === "today" ? "btn primary" : "btn"} onClick={() => setMode("today")} disabled={loading}>
              Today
            </button>
            <button className={mode === "inbox" ? "btn primary" : "btn"} onClick={() => setMode("inbox")} disabled={loading}>
              Inbox
            </button>
            <button className={mode === "all" ? "btn primary" : "btn"} onClick={() => setMode("all")} disabled={loading}>
              全部
            </button>
          </div>
        </div>

        <div className="taskLeftBody">
          {mode !== "today" ? (
            <div className="taskFilters">
              <label className="control">
                <span>状态</span>
                <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} disabled={loading}>
                  <option value="">全部</option>
                  <option value="todo">待办</option>
                  <option value="in_progress">进行中</option>
                  <option value="done">已完成</option>
                  <option value="blocked">阻塞</option>
                  <option value="cancelled">取消</option>
                </select>
              </label>
              <button className="btn" onClick={() => void refresh()} disabled={loading}>
                刷新
              </button>
            </div>
          ) : null}

          {mode === "inbox" ? (
            <div className="taskInboxCreate">
              <div className="taskSectionTitle">新建 Inbox</div>
              <input value={inboxTitle} onChange={(e) => setInboxTitle(e.target.value)} placeholder="标题" disabled={loading} />
              <input
                value={inboxDesc}
                onChange={(e) => setInboxDesc(e.target.value)}
                placeholder="描述（可选）"
                disabled={loading}
              />
              <button className="btn primary" onClick={() => void onCreateInbox()} disabled={loading}>
                加入收件箱
              </button>
            </div>
          ) : null}

          {mode === "today" && today ? (
            <div className="taskTodayMeta muted">
              今日到期 {today.todayDue.length} · 逾期 {today.overdue.length} · 24h 提醒 {today.upcomingReminders.length}
            </div>
          ) : null}

          <div className="taskList">
            {items.length === 0 && !loading ? <div className="muted">暂无任务</div> : null}
            {items.map((t) => (
              <button
                key={t.id}
                className={t.id === selectedId ? "taskListItem active" : "taskListItem"}
                onClick={() => setSelectedId(t.id)}
                disabled={loading}
              >
                <div className="taskListItemTitle">{t.title}</div>
                <div className="taskListItemMeta">
                  <span>{statusLabel(t.status)}</span>
                  {t.inbox ? (
                    <>
                      <span>·</span>
                      <span>Inbox</span>
                    </>
                  ) : null}
                  {t.dueAt ? (
                    <>
                      <span>·</span>
                      <span>due {formatTime(t.dueAt)}</span>
                    </>
                  ) : null}
                  {t.remindAt ? (
                    <>
                      <span>·</span>
                      <span>remind {formatTime(t.remindAt)}</span>
                    </>
                  ) : null}
                </div>
              </button>
            ))}
          </div>

          {mode !== "today" ? (
            <div className="taskPager">
              <div className="muted">
                共 {total} 条 · 第 {page}/{totalPages} 页
              </div>
              <div className="taskPagerActions">
                <button className="btn" disabled={loading || page <= 1} onClick={() => setPage((p) => Math.max(1, p - 1))}>
                  上一页
                </button>
                <button
                  className="btn"
                  disabled={loading || page >= totalPages}
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                >
                  下一页
                </button>
              </div>
            </div>
          ) : null}
        </div>
      </div>

      <div className="taskMain">
        <div className="taskMainHeader">
          <div className="taskMainTitle">详情</div>
          <div className="taskActions">
            <button className="btn primary" onClick={() => void onSaveTask()} disabled={loading || !selected}>
              保存
            </button>
            <button className="btn" onClick={() => void onCompleteTask()} disabled={loading || !selected}>
              完成
            </button>
            <button className="btn" onClick={() => void onDismissReminder()} disabled={loading || !selected}>
              关闭提醒
            </button>
          </div>
        </div>

        <div className="taskMainBody">
          {!selected ? <div className="muted">请选择一个任务</div> : null}
          {selected ? (
            <div className="taskForm">
              <div className="taskKv">
                <div className="muted">ID</div>
                <div>{selected.id}</div>
              </div>
              <div className="taskKv">
                <div className="muted">创建时间</div>
                <div className="muted">{formatTime(selected.createdAt)}</div>
              </div>

              <div className="taskDivider" />

              <label className="taskField">
                <div className="taskFieldLabel">标题</div>
                <input value={editTitle} onChange={(e) => setEditTitle(e.target.value)} disabled={loading} />
              </label>
              <label className="taskField">
                <div className="taskFieldLabel">描述</div>
                <textarea value={editDescription} onChange={(e) => setEditDescription(e.target.value)} rows={4} disabled={loading} />
              </label>

              <div className="taskGrid">
                <label className="taskField">
                  <div className="taskFieldLabel">状态</div>
                  <select value={editStatus} onChange={(e) => setEditStatus(e.target.value)} disabled={loading}>
                    <option value="todo">待办</option>
                    <option value="in_progress">进行中</option>
                    <option value="done">已完成</option>
                    <option value="blocked">阻塞</option>
                    <option value="cancelled">取消</option>
                  </select>
                </label>
                <label className="taskField">
                  <div className="taskFieldLabel">优先级</div>
                  <input
                    type="number"
                    value={editPriority}
                    onChange={(e) => setEditPriority(Number(e.target.value))}
                    disabled={loading}
                  />
                </label>
                <label className="taskField">
                  <div className="taskFieldLabel">Inbox</div>
                  <select value={editInbox ? "true" : "false"} onChange={(e) => setEditInbox(e.target.value === "true")} disabled={loading}>
                    <option value="false">否</option>
                    <option value="true">是</option>
                  </select>
                </label>
              </div>

              <div className="taskGrid">
                <label className="taskField">
                  <div className="taskFieldLabel">Goal</div>
                  <select value={editGoalId} onChange={(e) => setEditGoalId(e.target.value)} disabled={loading}>
                    <option value="">（不关联）</option>
                    {goalOptions.map((g) => (
                      <option key={g.id} value={g.id}>
                        {g.title} · {g.id}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="taskField">
                  <div className="taskFieldLabel">Plan</div>
                  <select value={editPlanId} onChange={(e) => setEditPlanId(e.target.value)} disabled={loading}>
                    <option value="">（不关联）</option>
                    {planOptions
                      .filter((p) => !editGoalId || p.goalId === editGoalId)
                      .map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.title} · {p.id}
                        </option>
                      ))}
                  </select>
                </label>
              </div>

              <div className="taskDivider" />

              <div className="taskSectionTitle">时间/提醒</div>
              <div className="taskGrid">
                <label className="taskField">
                  <div className="taskFieldLabel">Due At</div>
                  <input type="datetime-local" value={editDueAt} onChange={(e) => setEditDueAt(e.target.value)} disabled={loading} />
                </label>
                <label className="taskField">
                  <div className="taskFieldLabel">Remind At</div>
                  <input
                    type="datetime-local"
                    value={editRemindAt}
                    onChange={(e) => setEditRemindAt(e.target.value)}
                    disabled={loading}
                  />
                </label>
                <label className="taskField">
                  <div className="taskFieldLabel">Snooze Until</div>
                  <input
                    type="datetime-local"
                    value={editSnoozeUntil}
                    onChange={(e) => setEditSnoozeUntil(e.target.value)}
                    disabled={loading}
                  />
                </label>
              </div>

              <div className="taskInlineActions">
                <button className="btn" onClick={() => void onSnoozeTask()} disabled={loading}>
                  稍后提醒（Snooze）
                </button>
                {selected.inbox ? (
                  <>
                    <button className="btn" onClick={() => void onArchiveInbox()} disabled={loading}>
                      归档 Inbox（inbox=false）
                    </button>
                    <button className="btn" onClick={() => void onDeleteInbox()} disabled={loading}>
                      删除 Inbox（cancelled）
                    </button>
                  </>
                ) : null}
              </div>
            </div>
          ) : null}

          {error ? <div className="error">{error}</div> : null}
        </div>
      </div>
    </section>
  );
}

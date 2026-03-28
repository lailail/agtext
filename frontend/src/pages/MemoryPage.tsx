import { useCallback, useEffect, useMemo, useState } from "react";
import type { GoalItem, MemoryItem, PlanItem, TaskItem } from "../api";
import {
  approveMemoryItem,
  disableMemoryItem,
  linkMemoryItem,
  listGoals,
  listMemoryItems,
  listPlans,
  listTasks,
  updateMemoryItem,
} from "../api";

type StatusFilter = "candidate" | "approved" | "disabled" | "all";

function statusLabel(status: string) {
  if (status === "candidate") return "候选";
  if (status === "approved") return "已通过";
  if (status === "disabled") return "已禁用";
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

export default function MemoryPage() {
  const [status, setStatus] = useState<StatusFilter>("candidate");
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [items, setItems] = useState<MemoryItem[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const selected = useMemo(() => items.find((x) => x.id === selectedId) ?? null, [items, selectedId]);

  const [editTitle, setEditTitle] = useState("");
  const [editContent, setEditContent] = useState("");
  const [reviewerNote, setReviewerNote] = useState("");

  const [linkGoalId, setLinkGoalId] = useState("");
  const [linkPlanId, setLinkPlanId] = useState("");
  const [linkTaskId, setLinkTaskId] = useState("");
  const [manualGoalId, setManualGoalId] = useState("");
  const [manualPlanId, setManualPlanId] = useState("");
  const [manualTaskId, setManualTaskId] = useState("");

  const [goalOptions, setGoalOptions] = useState<GoalItem[]>([]);
  const [planOptions, setPlanOptions] = useState<PlanItem[]>([]);
  const [taskOptions, setTaskOptions] = useState<TaskItem[]>([]);
  const [optionsLoading, setOptionsLoading] = useState(false);

  const refresh = useCallback(async (nextSelectedId?: string | null) => {
    setLoading(true);
    setError(null);
    try {
      const res = await listMemoryItems({ status: status === "all" ? null : status }, page, pageSize);
      setItems(res.items);
      setTotal(res.total);
      const keep = nextSelectedId ?? selectedId;
      if (keep && res.items.some((x) => x.id === keep)) {
        setSelectedId(keep);
      } else {
        setSelectedId(res.items[0]?.id ?? null);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, selectedId, status]);

  useEffect(() => {
    void refresh(null);
  }, [refresh]);

  useEffect(() => {
    if (!selected) return;
    setEditTitle(selected.title ?? "");
    setEditContent(selected.content ?? "");
    setReviewerNote("");
    setLinkGoalId(selected.relatedGoalId ?? "");
    setLinkPlanId(selected.relatedPlanId ?? "");
    setLinkTaskId(selected.relatedTaskId ?? "");

    setManualGoalId(selected.relatedGoalId ?? "");
    setManualPlanId(selected.relatedPlanId ?? "");
    setManualTaskId(selected.relatedTaskId ?? "");
  }, [selectedId, selected]);

  const refreshOptions = useCallback(async (goalId: string | null, planId: string | null) => {
    setOptionsLoading(true);
    try {
      const goalsRes = await listGoals({ status: "active" }, 1, 100);
      setGoalOptions(goalsRes.items);

      const nextGoal = goalId?.trim() ? goalId.trim() : null;
      const plansRes = await listPlans({ goalId: nextGoal, status: "active" }, 1, 100);
      setPlanOptions(plansRes.items);

      const nextPlan = planId?.trim() ? planId.trim() : null;
      const tasksRes = await listTasks(
        {
          planId: nextPlan,
          goalId: nextPlan ? null : nextGoal,
          status: "in_progress",
        },
        1,
        100,
      );
      setTaskOptions(tasksRes.items);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setOptionsLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshOptions(linkGoalId, linkPlanId);
  }, [linkGoalId, linkPlanId, refreshOptions]);

  async function onSave() {
    if (!selected) return;
    if (!editContent.trim()) {
      setError("Content 不能为空");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await updateMemoryItem(selected.id, {
        title: editTitle.trim() ? editTitle.trim() : null,
        content: editContent.trim(),
      });
      await refresh(selected.id);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onApprove() {
    if (!selected) return;
    setLoading(true);
    setError(null);
    try {
      await approveMemoryItem(selected.id, { reviewerNote: reviewerNote ? reviewerNote : null });
      await refresh(selected.id);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onDisable() {
    if (!selected) return;
    setLoading(true);
    setError(null);
    try {
      await disableMemoryItem(selected.id, { reviewerNote: reviewerNote ? reviewerNote : null });
      await refresh(selected.id);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onLink() {
    if (!selected) return;
    setLoading(true);
    setError(null);
    const goalId = manualGoalId.trim() ? manualGoalId.trim() : linkGoalId.trim() ? linkGoalId.trim() : null;
    const planId = manualPlanId.trim() ? manualPlanId.trim() : linkPlanId.trim() ? linkPlanId.trim() : null;
    const taskId = manualTaskId.trim() ? manualTaskId.trim() : linkTaskId.trim() ? linkTaskId.trim() : null;
    try {
      await linkMemoryItem(selected.id, {
        goalId,
        planId,
        taskId,
      });
      await refresh(selected.id);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  return (
    <section className="memory">
      <div className="memoryLeft">
        <div className="memoryLeftHeader">
          <div>
            <h1>记忆审核</h1>
            <div className="muted">候选 / 通过 / 禁用 的人工审核与维护</div>
          </div>
          <div className="memoryControls">
            <label className="control">
              <span>状态</span>
              <select
                value={status}
                onChange={(e) => {
                  setPage(1);
                  setStatus(e.target.value as StatusFilter);
                }}
                disabled={loading}
              >
                <option value="candidate">候选</option>
                <option value="approved">已通过</option>
                <option value="disabled">已禁用</option>
                <option value="all">全部</option>
              </select>
            </label>
            <button className="btn" onClick={() => void refresh(selectedId)} disabled={loading}>
              刷新
            </button>
          </div>
        </div>

        <div className="memoryList">
          {loading && items.length === 0 ? <div className="muted">加载中…</div> : null}
          {items.length === 0 && !loading ? <div className="muted">暂无数据</div> : null}
          {items.map((it) => (
            <button
              key={it.id}
              className={it.id === selectedId ? "memoryListItem active" : "memoryListItem"}
              onClick={() => setSelectedId(it.id)}
              disabled={loading}
            >
              <div className="memoryListItemTop">
                <div className="memoryListItemTitle">{it.title?.trim() ? it.title : it.content.slice(0, 28)}</div>
                <div className="memoryBadge">{statusLabel(it.status)}</div>
              </div>
              <div className="memoryListItemMeta">
                <span>{it.id}</span>
                <span>·</span>
                <span>{formatTime(it.createdAt)}</span>
              </div>
            </button>
          ))}
        </div>

        <div className="memoryPager">
          <div className="muted">
            共 {total} 条 · 第 {page}/{totalPages} 页
          </div>
          <div className="memoryPagerActions">
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
      </div>

      <div className="memoryMain">
        <div className="memoryMainHeader">
          <div className="memoryMainTitle">详情</div>
          <div className="memoryActions">
            <button className="btn primary" onClick={() => void onSave()} disabled={loading || !selected}>
              保存编辑
            </button>
            <button className="btn" onClick={() => void onApprove()} disabled={loading || !selected}>
              通过
            </button>
            <button className="btn" onClick={() => void onDisable()} disabled={loading || !selected}>
              禁用
            </button>
          </div>
        </div>

        <div className="memoryMainBody">
          {!selected ? <div className="muted">请选择一条记忆</div> : null}

          {selected ? (
            <div className="memoryForm">
              <div className="memoryKv">
                <div className="muted">ID</div>
                <div>{selected.id}</div>
              </div>
              <div className="memoryKv">
                <div className="muted">状态</div>
                <div>{statusLabel(selected.status)}</div>
              </div>
              <div className="memoryKv">
                <div className="muted">来源</div>
                <div>
                  {selected.sourceType ?? "-"}
                  {selected.sourceConversationId ? ` · conv=${selected.sourceConversationId}` : ""}
                  {selected.sourceMessageId ? ` · msg=${selected.sourceMessageId}` : ""}
                </div>
              </div>
              <div className="memoryKv">
                <div className="muted">候选原因</div>
                <div className="memoryMono">{selected.candidateReason ?? "-"}</div>
              </div>
              <div className="memoryKv">
                <div className="muted">审核备注</div>
                <div className="memoryMono">{selected.reviewerNote ?? "-"}</div>
              </div>
              <div className="memoryKv">
                <div className="muted">时间</div>
                <div className="memoryMono">
                  createdAt={formatTime(selected.createdAt)} · updatedAt={formatTime(selected.updatedAt)} · reviewedAt=
                  {formatTime(selected.reviewedAt)} · approvedAt={formatTime(selected.approvedAt)} · disabledAt=
                  {formatTime(selected.disabledAt)}
                </div>
              </div>

              <div className="memoryDivider" />

              <label className="memoryField">
                <div className="memoryFieldLabel">Title</div>
                <input value={editTitle} onChange={(e) => setEditTitle(e.target.value)} disabled={loading} />
              </label>

              <label className="memoryField">
                <div className="memoryFieldLabel">Content</div>
                <textarea
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  rows={6}
                  disabled={loading}
                />
              </label>

              <label className="memoryField">
                <div className="memoryFieldLabel">Reviewer Note（用于通过/禁用）</div>
                <input
                  value={reviewerNote}
                  onChange={(e) => setReviewerNote(e.target.value)}
                  placeholder="可选"
                  disabled={loading}
                />
              </label>

              <div className="memoryDivider" />

              <div className="memorySectionTitle">关联（可选）</div>
              <div className="muted">优先用下拉选择；也支持手动输入 ID（goal_1 / plan_1 / task_1）。</div>

              <div className="memoryLinkGrid">
                <label className="memoryField">
                  <div className="memoryFieldLabel">
                    Goal（下拉）
                    {optionsLoading ? <span className="muted"> · 加载中…</span> : null}
                  </div>
                  <select
                    value={linkGoalId}
                    onChange={(e) => {
                      setLinkGoalId(e.target.value);
                      setLinkPlanId("");
                      setLinkTaskId("");
                    }}
                    disabled={loading || optionsLoading}
                  >
                    <option value="">（不关联）</option>
                    {goalOptions.map((g) => (
                      <option key={g.id} value={g.id}>
                        {g.title} · {g.id}
                      </option>
                    ))}
                  </select>
                  <input
                    value={manualGoalId}
                    onChange={(e) => setManualGoalId(e.target.value)}
                    placeholder="手动输入 goalId（可选）"
                    disabled={loading}
                  />
                </label>

                <label className="memoryField">
                  <div className="memoryFieldLabel">Plan（下拉，按 goal 过滤）</div>
                  <select
                    value={linkPlanId}
                    onChange={(e) => {
                      setLinkPlanId(e.target.value);
                      setLinkTaskId("");
                    }}
                    disabled={loading || optionsLoading}
                  >
                    <option value="">（不关联）</option>
                    {planOptions.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.title} · {p.id}
                      </option>
                    ))}
                  </select>
                  <input
                    value={manualPlanId}
                    onChange={(e) => setManualPlanId(e.target.value)}
                    placeholder="手动输入 planId（可选）"
                    disabled={loading}
                  />
                </label>

                <label className="memoryField">
                  <div className="memoryFieldLabel">Task（下拉，in_progress 优先）</div>
                  <select
                    value={linkTaskId}
                    onChange={(e) => setLinkTaskId(e.target.value)}
                    disabled={loading || optionsLoading}
                  >
                    <option value="">（不关联）</option>
                    {taskOptions.map((t) => (
                      <option key={t.id} value={t.id}>
                        [{t.status}] {t.title} · {t.id}
                      </option>
                    ))}
                  </select>
                  <input
                    value={manualTaskId}
                    onChange={(e) => setManualTaskId(e.target.value)}
                    placeholder="手动输入 taskId（可选）"
                    disabled={loading}
                  />
                </label>
              </div>

              <div className="memoryLinkActions">
                <button className="btn" onClick={() => void onLink()} disabled={loading}>
                  保存关联
                </button>
                <button
                  className="btn"
                  onClick={() => void refreshOptions(linkGoalId, linkPlanId)}
                  disabled={loading || optionsLoading}
                >
                  刷新下拉
                </button>
                <div className="muted">
                  当前：goal={selected.relatedGoalId ?? "-"} · plan={selected.relatedPlanId ?? "-"} · task=
                  {selected.relatedTaskId ?? "-"}
                </div>
              </div>
            </div>
          ) : null}

          {error ? <div className="error">{error}</div> : null}
        </div>
      </div>
    </section>
  );
}

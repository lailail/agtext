export type PageResponse<T> = {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
};

export type MemoryStatus = "candidate" | "approved" | "disabled" | string;

export type ConversationItem = {
  id: string;
  title: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type MessageItem = {
  id: string;
  conversationId: string;
  role: "system" | "user" | "assistant" | string;
  content: string;
  provider: string | null;
  modelName: string | null;
  tokens: number | null;
  createdAt: string;
  updatedAt: string;
};

const API_BASE =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "http://localhost:8080";

async function json<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return (await res.json()) as T;
}

export async function listConversations(page = 1, pageSize = 50): Promise<PageResponse<ConversationItem>> {
  const res = await fetch(`${API_BASE}/api/conversations?page=${page}&page_size=${pageSize}`);
  return json(res);
}

export async function createConversation(title?: string): Promise<ConversationItem> {
  const res = await fetch(`${API_BASE}/api/conversations`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title: title ?? null }),
  });
  return json(res);
}

export async function listMessages(conversationId: string): Promise<MessageItem[]> {
  const res = await fetch(`${API_BASE}/api/conversations/${conversationId}/messages`);
  return json(res);
}

export type ChatRequest = {
  conversationId?: string | null;
  knowledgeBaseId?: string | null;
  message: string;
  provider?: string | null;
  model?: string | null;
};

export type Citation = {
  documentId: string;
  documentTitle: string | null;
  sourceUri: string | null;
  chunkId: string;
  excerpt: string;
  score: number;
};

export type ChatResponse = {
  conversationId: string;
  provider: string;
  model: string;
  assistantMessage: string;
  citations: Citation[];
  createdAt: string;
};

export async function chat(req: ChatRequest): Promise<ChatResponse> {
  const res = await fetch(`${API_BASE}/api/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  return json(res);
}

export type KnowledgeBaseItem = {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
};

export type KnowledgeDocumentItem = {
  id: string;
  knowledgeBaseId: string;
  sourceType: string;
  sourceUri: string;
  title: string | null;
  status: string;
  parseStatus: string | null;
  indexStatus: string | null;
  errorMessage: string | null;
  latestImportJobId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ImportJobItem = {
  id: string;
  knowledgeBaseId: string;
  documentId: string | null;
  status: string;
  stage: string | null;
  progress: number | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ParseReportItem = {
  jobId: string;
  documentId: string;
  pageCount: number | null;
  extractedChars: number | null;
  chunkCount: number | null;
  parserName: string | null;
  failedAt: string | null;
  samplePreview: string | null;
  createdAt: string;
};

export type KnowledgeChunkItem = {
  id: string;
  knowledgeDocumentId: string;
  importJobId: string | null;
  chunkIndex: number;
  content: string;
  createdAt: string;
  updatedAt: string;
};

export async function listKnowledgeBases(page = 1, pageSize = 50): Promise<PageResponse<KnowledgeBaseItem>> {
  const res = await fetch(`${API_BASE}/api/knowledge/bases?page=${page}&page_size=${pageSize}`);
  return json(res);
}

export async function createKnowledgeBase(
  name: string,
  type?: string | null,
  description?: string | null,
): Promise<KnowledgeBaseItem> {
  const res = await fetch(`${API_BASE}/api/knowledge/bases`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, type: type ?? null, description: description ?? null }),
  });
  return json(res);
}

export async function listKnowledgeDocuments(
  knowledgeBaseId: string,
  page = 1,
  pageSize = 50,
): Promise<PageResponse<KnowledgeDocumentItem>> {
  const res = await fetch(
    `${API_BASE}/api/knowledge/bases/${knowledgeBaseId}/documents?page=${page}&page_size=${pageSize}`,
  );
  return json(res);
}

export async function importMarkdown(
  knowledgeBaseId: string,
  title: string | null,
  content: string,
): Promise<ImportJobItem> {
  const res = await fetch(`${API_BASE}/api/knowledge/bases/${knowledgeBaseId}/imports/markdown`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, content }),
  });
  return json(res);
}

export async function importWeb(
  knowledgeBaseId: string,
  url: string,
  title?: string | null,
): Promise<ImportJobItem> {
  const res = await fetch(`${API_BASE}/api/knowledge/bases/${knowledgeBaseId}/imports/web`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ url, title: title ?? null }),
  });
  return json(res);
}

export async function importPdf(knowledgeBaseId: string, file: File): Promise<ImportJobItem> {
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${API_BASE}/api/knowledge/bases/${knowledgeBaseId}/imports/pdf`, {
    method: "POST",
    body: form,
  });
  return json(res);
}

export async function listImportJobs(
  opts: { knowledgeBaseId?: string | null; documentId?: string | null; status?: string | null } = {},
  page = 1,
  pageSize = 20,
): Promise<PageResponse<ImportJobItem>> {
  const qs = new URLSearchParams();
  if (opts.knowledgeBaseId) qs.set("knowledge_base_id", opts.knowledgeBaseId);
  if (opts.documentId) qs.set("document_id", opts.documentId);
  if (opts.status) qs.set("status", opts.status);
  qs.set("page", String(page));
  qs.set("page_size", String(pageSize));
  const res = await fetch(`${API_BASE}/api/knowledge/import-jobs?${qs.toString()}`);
  return json(res);
}

export async function retryImportJob(jobId: string): Promise<ImportJobItem> {
  const res = await fetch(`${API_BASE}/api/knowledge/import-jobs/${jobId}/retry`, { method: "POST" });
  return json(res);
}

export async function cancelImportJob(jobId: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/knowledge/import-jobs/${jobId}/cancel`, { method: "POST" });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

export async function getParseReport(jobId: string): Promise<ParseReportItem> {
  const res = await fetch(`${API_BASE}/api/knowledge/import-jobs/${jobId}/parse-report`);
  return json(res);
}

export async function listDocumentChunks(
  documentId: string,
  opts: { jobId?: string | null } = {},
  page = 1,
  pageSize = 50,
): Promise<PageResponse<KnowledgeChunkItem>> {
  const qs = new URLSearchParams();
  if (opts.jobId) qs.set("job_id", opts.jobId);
  qs.set("page", String(page));
  qs.set("page_size", String(pageSize));
  const res = await fetch(`${API_BASE}/api/knowledge/documents/${documentId}/chunks?${qs.toString()}`);
  return json(res);
}

export async function reindexDocument(documentId: string): Promise<ImportJobItem> {
  const res = await fetch(`${API_BASE}/api/knowledge/documents/${documentId}/reindex`, { method: "POST" });
  return json(res);
}

export type NotificationItem = {
  id: string;
  type: string | null;
  title: string;
  content: string | null;
  refType: string | null;
  refId: string | null;
  status: "unread" | "read" | "archived" | string;
  createdAt: string;
};

export async function unreadNotificationCount(): Promise<number> {
  const res = await fetch(`${API_BASE}/api/notifications/unread-count`);
  const data = await json<{ count: number }>(res);
  return data.count;
}

export async function listNotifications(
  status: string | null,
  page = 1,
  pageSize = 50,
): Promise<PageResponse<NotificationItem>> {
  const qs = new URLSearchParams();
  if (status) qs.set("status", status);
  qs.set("page", String(page));
  qs.set("page_size", String(pageSize));
  const res = await fetch(`${API_BASE}/api/notifications?${qs.toString()}`);
  return json(res);
}

export async function markNotificationRead(id: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/notifications/${id}/read`, { method: "POST" });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

export async function archiveNotification(id: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/notifications/${id}/archive`, { method: "POST" });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

export type AgentRoleInfo = {
  name: string;
  description: string;
};

export async function listAgentRoles(): Promise<AgentRoleInfo[]> {
  const res = await fetch(`${API_BASE}/api/agents/roles`);
  return json(res);
}

export type AgentRunRequest = {
  role: string;
  input: string;
  provider?: string | null;
  model?: string | null;
};

export type AgentRunResponse = {
  provider: string;
  model: string;
  content: string;
  createdAt: string;
};

export async function runAgent(req: AgentRunRequest): Promise<AgentRunResponse> {
  const res = await fetch(`${API_BASE}/api/agents/run`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  return json(res);
}

export type ToolSettingItem = {
  name: string;
  description: string;
  type: string;
  requiresConfirmation: boolean;
  enabled: boolean;
};

export type ToolSettingsResponse = {
  domainAllowlist: string[];
  tools: ToolSettingItem[];
};

export async function getToolSettings(): Promise<ToolSettingsResponse> {
  const res = await fetch(`${API_BASE}/api/settings/tools`);
  return json(res);
}

export async function setToolDomainAllowlist(domains: string[]): Promise<void> {
  const res = await fetch(`${API_BASE}/api/settings/tools/domain-allowlist`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ domains }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

export async function setToolEnabled(toolName: string, enabled: boolean): Promise<void> {
  const res = await fetch(`${API_BASE}/api/settings/tools/${encodeURIComponent(toolName)}/enabled`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ enabled }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

export type ModelProviderItem = {
  name: string;
  baseUrl: string | null;
  apiKeyConfigured: boolean;
  model: string | null;
};

export type ModelSettingsResponse = {
  defaultProvider: string;
  fallbackModel: string;
  providers: ModelProviderItem[];
};

export async function getModelSettings(): Promise<ModelSettingsResponse> {
  const res = await fetch(`${API_BASE}/api/settings/models`);
  return json(res);
}

export async function setDefaultProvider(defaultProvider: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/settings/models/default-provider`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ defaultProvider }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

export async function setProviderModel(providerName: string, model: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/settings/models/providers/${encodeURIComponent(providerName)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ model }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

export type MemoryItem = {
  id: string;
  title: string | null;
  content: string;
  status: MemoryStatus;
  sourceType: string | null;
  sourceConversationId: number | null;
  sourceMessageId: number | null;
  relatedGoalId: string | null;
  relatedPlanId: string | null;
  relatedTaskId: string | null;
  candidateReason: string | null;
  reviewerNote: string | null;
  reviewedAt: string | null;
  approvedAt: string | null;
  disabledAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export async function listMemoryItems(
  opts: { status?: string | null } = {},
  page = 1,
  pageSize = 20,
): Promise<PageResponse<MemoryItem>> {
  const qs = new URLSearchParams();
  if (opts.status) qs.set("status", opts.status);
  qs.set("page", String(page));
  qs.set("page_size", String(pageSize));
  const res = await fetch(`${API_BASE}/api/memory/items?${qs.toString()}`);
  return json(res);
}

export async function getMemoryItem(id: string): Promise<MemoryItem> {
  const res = await fetch(`${API_BASE}/api/memory/items/${id}`);
  return json(res);
}

export async function updateMemoryItem(
  id: string,
  req: { title?: string | null; content: string },
): Promise<MemoryItem> {
  const res = await fetch(`${API_BASE}/api/memory/items/${id}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  return json(res);
}

export async function approveMemoryItem(
  id: string,
  req: { reviewerNote?: string | null } = {},
): Promise<MemoryItem> {
  const res = await fetch(`${API_BASE}/api/memory/items/${id}/approve`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  return json(res);
}

export async function disableMemoryItem(
  id: string,
  req: { reviewerNote?: string | null } = {},
): Promise<MemoryItem> {
  const res = await fetch(`${API_BASE}/api/memory/items/${id}/disable`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  return json(res);
}

export async function linkMemoryItem(
  id: string,
  req: { goalId?: string | null; planId?: string | null; taskId?: string | null },
): Promise<MemoryItem> {
  const res = await fetch(`${API_BASE}/api/memory/items/${id}/link`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  return json(res);
}

export type GoalItem = {
  id: string;
  title: string;
  description: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export async function listGoals(
  opts: { status?: string | null } = {},
  page = 1,
  pageSize = 50,
): Promise<PageResponse<GoalItem>> {
  const qs = new URLSearchParams();
  if (opts.status) qs.set("status", opts.status);
  qs.set("page", String(page));
  qs.set("page_size", String(pageSize));
  const res = await fetch(`${API_BASE}/api/goals?${qs.toString()}`);
  return json(res);
}

export type PlanItem = {
  id: string;
  goalId: string | null;
  title: string;
  description: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export async function listPlans(
  opts: { goalId?: string | null; status?: string | null } = {},
  page = 1,
  pageSize = 50,
): Promise<PageResponse<PlanItem>> {
  const qs = new URLSearchParams();
  if (opts.goalId) qs.set("goal_id", opts.goalId);
  if (opts.status) qs.set("status", opts.status);
  qs.set("page", String(page));
  qs.set("page_size", String(pageSize));
  const res = await fetch(`${API_BASE}/api/plans?${qs.toString()}`);
  return json(res);
}

export type TaskItem = {
  id: string;
  planId: string | null;
  goalId: string | null;
  inbox: boolean;
  title: string;
  description: string | null;
  status: string;
  priority: number;
  dueAt: string | null;
  remindAt: string | null;
  snoozeUntil: string | null;
  createdAt: string;
  updatedAt: string;
};

export async function listTasks(
  opts: {
    planId?: string | null;
    goalId?: string | null;
    inbox?: boolean | null;
    status?: string | null;
    dueBefore?: string | null;
  } = {},
  page = 1,
  pageSize = 50,
): Promise<PageResponse<TaskItem>> {
  const qs = new URLSearchParams();
  if (opts.planId) qs.set("plan_id", opts.planId);
  if (opts.goalId) qs.set("goal_id", opts.goalId);
  if (opts.inbox !== null && opts.inbox !== undefined) qs.set("inbox", String(opts.inbox));
  if (opts.status) qs.set("status", opts.status);
  if (opts.dueBefore) qs.set("due_before", opts.dueBefore);
  qs.set("page", String(page));
  qs.set("page_size", String(pageSize));
  const res = await fetch(`${API_BASE}/api/tasks?${qs.toString()}`);
  return json(res);
}

export async function getTask(id: string): Promise<TaskItem> {
  const res = await fetch(`${API_BASE}/api/tasks/${id}`);
  return json(res);
}

export async function updateTask(
  id: string,
  req: {
    planId?: string | null;
    goalId?: string | null;
    inbox?: boolean | null;
    title: string;
    description?: string | null;
    status?: string | null;
    priority?: number | null;
    dueAt?: string | null;
    remindAt?: string | null;
    snoozeUntil?: string | null;
  },
): Promise<TaskItem> {
  const res = await fetch(`${API_BASE}/api/tasks/${id}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  return json(res);
}

export async function completeTask(id: string): Promise<TaskItem> {
  const res = await fetch(`${API_BASE}/api/tasks/${id}/complete`, { method: "POST" });
  return json(res);
}

export async function snoozeTask(id: string, snoozeUntil: string): Promise<TaskItem> {
  const res = await fetch(`${API_BASE}/api/tasks/${id}/snooze`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ snoozeUntil }),
  });
  return json(res);
}

export async function dismissTaskReminder(id: string): Promise<TaskItem> {
  const res = await fetch(`${API_BASE}/api/tasks/${id}/dismiss-reminder`, { method: "POST" });
  return json(res);
}

export type TodayTasksResponse = {
  todayDue: TaskItem[];
  overdue: TaskItem[];
  upcomingReminders: TaskItem[];
};

export async function getTodayTasks(): Promise<TodayTasksResponse> {
  const res = await fetch(`${API_BASE}/api/tasks/today`);
  return json(res);
}

export async function createInboxItem(req: { title: string; description?: string | null }): Promise<TaskItem> {
  const res = await fetch(`${API_BASE}/api/inbox`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title: req.title, description: req.description ?? null }),
  });
  return json(res);
}

export async function archiveInboxItem(
  taskId: string,
  req: { planId?: string | null; goalId?: string | null } = {},
): Promise<TaskItem> {
  const res = await fetch(`${API_BASE}/api/inbox/${taskId}/archive`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ planId: req.planId ?? null, goalId: req.goalId ?? null }),
  });
  return json(res);
}

export async function deleteInboxItem(taskId: string): Promise<TaskItem> {
  const res = await fetch(`${API_BASE}/api/inbox/${taskId}/delete`, { method: "POST" });
  return json(res);
}

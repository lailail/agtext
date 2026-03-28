const BASE_URL = process.env.BASE_URL?.replace(/\/+$/, "") ?? "http://localhost:8080";

async function http(method, path, body) {
  const url = `${BASE_URL}${path}`;
  const res = await fetch(url, {
    method,
    headers: body ? { "content-type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    // ignore
  }
  return { res, text, json };
}

function assert(cond, msg) {
  if (!cond) throw new Error(msg);
}

async function waitFor(fn, { timeoutMs = 15000, intervalMs = 500 } = {}) {
  const start = Date.now();
  // eslint-disable-next-line no-constant-condition
  while (true) {
    const out = await fn();
    if (out?.done) return out.value;
    if (Date.now() - start > timeoutMs) throw new Error("timeout");
    await new Promise((r) => setTimeout(r, intervalMs));
  }
}

async function main() {
  console.log(`[smoke] baseUrl=${BASE_URL}`);

  // 1) Health
  {
    const { res, json, text } = await http("GET", "/actuator/health");
    assert(res.status === 200, `health status=${res.status} body=${text}`);
    assert(json?.status === "UP", `health json=${text}`);
    console.log("[smoke] health OK");
  }

  // 2) Chat (requires provider configured; recommended: start backend with SPRING_PROFILES_ACTIVE=test)
  {
    const { res, json, text } = await http("POST", "/api/chat", { message: "ping" });
    assert(res.status === 200, `chat status=${res.status} body=${text}`);
    assert(typeof json?.assistantMessage === "string", `chat json=${text}`);
    console.log("[smoke] chat OK");
  }

  // 3) Knowledge base + import markdown
  let kbId = null;
  {
    const { res, json, text } = await http("POST", "/api/knowledge/bases", {
      name: "smoke-kb",
      description: "smoke",
    });
    assert(res.status === 200, `kb create status=${res.status} body=${text}`);
    kbId = json?.id;
    assert(typeof kbId === "string" && kbId.length > 0, `kbId missing: ${text}`);
    console.log(`[smoke] kb created: ${kbId}`);
  }

  let jobId = null;
  {
    const { res, json, text } = await http(
      "POST",
      `/api/knowledge/bases/${kbId}/imports/markdown`,
      {
        title: "smoke-doc",
        content: "# Hello\n\nThis is a smoke import.\n",
      },
    );
    assert(res.status === 200, `import status=${res.status} body=${text}`);
    jobId = json?.id;
    assert(typeof jobId === "string" && jobId.length > 0, `jobId missing: ${text}`);
    console.log(`[smoke] import job created: ${jobId}`);
  }

  await waitFor(async () => {
    const { res, json, text } = await http(
      "GET",
      `/api/knowledge/import-jobs?knowledge_base_id=${encodeURIComponent(kbId)}&page=1&page_size=10`,
    );
    assert(res.status === 200, `jobs status=${res.status} body=${text}`);
    const items = json?.items ?? [];
    const job = items.find((it) => it.id === jobId);
    if (!job) return { done: false };
    if (job.status === "succeeded") return { done: true, value: job };
    if (job.status === "failed") throw new Error(`import failed: ${job.errorMessage ?? ""}`);
    return { done: false };
  });
  console.log("[smoke] knowledge import OK");

  // 4) Tasks: create + list + complete
  let taskId = null;
  {
    const { res, json, text } = await http("POST", "/api/tasks", {
      inbox: true,
      title: "smoke-task",
      description: "smoke",
      status: "todo",
      priority: 0,
    });
    assert(res.status === 200, `task create status=${res.status} body=${text}`);
    taskId = json?.id;
    assert(typeof taskId === "string" && taskId.length > 0, `taskId missing: ${text}`);
  }
  {
    const { res, json, text } = await http("GET", "/api/tasks?inbox=true&page=1&page_size=20");
    assert(res.status === 200, `task list status=${res.status} body=${text}`);
    assert((json?.items ?? []).some((t) => t.id === taskId), `task not found: ${text}`);
  }
  {
    const { res, json, text } = await http("POST", `/api/tasks/${taskId}/complete`);
    assert(res.status === 200, `task complete status=${res.status} body=${text}`);
    assert(json?.status === "done", `task complete json=${text}`);
  }
  console.log("[smoke] tasks OK");

  console.log("[smoke] ALL OK");
}

main().catch((e) => {
  console.error("[smoke] FAILED:", e?.message ?? e);
  process.exitCode = 1;
});


import { useCallback, useEffect, useMemo, useState } from "react";
import {
  cancelImportJob,
  createKnowledgeBase,
  getParseReport,
  importMarkdown,
  importPdf,
  importWeb,
  listDocumentChunks,
  listImportJobs,
  listKnowledgeBases,
  listKnowledgeDocuments,
  reindexDocument,
  retryImportJob,
  type ImportJobItem,
  type KnowledgeBaseItem,
  type KnowledgeDocumentItem,
  type ParseReportItem,
} from "../api";

export default function KnowledgePage() {
  const [bases, setBases] = useState<KnowledgeBaseItem[]>([]);
  const [selectedBaseId, setSelectedBaseId] = useState<string | null>(null);
  const [docs, setDocs] = useState<KnowledgeDocumentItem[]>([]);
  const [jobs, setJobs] = useState<ImportJobItem[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [newBaseName, setNewBaseName] = useState("");
  const [newBaseDesc, setNewBaseDesc] = useState("");

  const [mdTitle, setMdTitle] = useState("");
  const [mdContent, setMdContent] = useState("");
  const [webUrl, setWebUrl] = useState("");
  const [webTitle, setWebTitle] = useState("");
  const [pdfFile, setPdfFile] = useState<File | null>(null);

  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const [report, setReport] = useState<ParseReportItem | null>(null);
  const [chunkPreview, setChunkPreview] = useState<string[] | null>(null);

  const selectedBase = useMemo(
    () => bases.find((b) => b.id === selectedBaseId) ?? null,
    [bases, selectedBaseId],
  );

  const refreshBases = useCallback(async () => {
    const page = await listKnowledgeBases(1, 50);
    setBases(page.items);
    setSelectedBaseId((prev) => prev ?? page.items[0]?.id ?? null);
  }, []);

  const refreshBaseData = useCallback(async (baseId: string) => {
    const [docPage, jobPage] = await Promise.all([
      listKnowledgeDocuments(baseId, 1, 50),
      listImportJobs({ knowledgeBaseId: baseId }, 1, 50),
    ]);
    setDocs(docPage.items);
    setJobs(jobPage.items);
  }, []);

  useEffect(() => {
    refreshBases().catch((e: unknown) => setError(String(e)));
  }, [refreshBases]);

  useEffect(() => {
    if (!selectedBaseId) {
      setDocs([]);
      setJobs([]);
      return;
    }
    refreshBaseData(selectedBaseId).catch((e: unknown) => setError(String(e)));
  }, [refreshBaseData, selectedBaseId]);

  useEffect(() => {
    if (!selectedJobId) {
      setReport(null);
      setChunkPreview(null);
      return;
    }
    (async () => {
      const r = await getParseReport(selectedJobId);
      setReport(r);
      const chunksPage = await listDocumentChunks(r.documentId, { jobId: selectedJobId }, 1, 10);
      setChunkPreview(chunksPage.items.map((c) => c.content));
    })().catch((e: unknown) => setError(String(e)));
  }, [selectedJobId]);

  async function onCreateBase() {
    const name = newBaseName.trim();
    if (!name) return;
    setError(null);
    setBusy(true);
    try {
      const created = await createKnowledgeBase(name, null, newBaseDesc.trim() || null);
      setBases((prev) => [created, ...prev]);
      setSelectedBaseId(created.id);
      setNewBaseName("");
      setNewBaseDesc("");
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  async function onImportMarkdown() {
    if (!selectedBaseId) return;
    const content = mdContent.trim();
    if (!content) return;
    setError(null);
    setBusy(true);
    try {
      const job = await importMarkdown(selectedBaseId, mdTitle.trim() || null, content);
      setMdTitle("");
      setMdContent("");
      setJobs((prev) => [job, ...prev]);
      await refreshBaseData(selectedBaseId);
      setSelectedJobId(job.id);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  async function onImportWeb() {
    if (!selectedBaseId) return;
    const url = webUrl.trim();
    if (!url) return;
    setError(null);
    setBusy(true);
    try {
      const job = await importWeb(selectedBaseId, url, webTitle.trim() || null);
      setWebUrl("");
      setWebTitle("");
      setJobs((prev) => [job, ...prev]);
      await refreshBaseData(selectedBaseId);
      setSelectedJobId(job.id);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  async function onImportPdf() {
    if (!selectedBaseId || !pdfFile) return;
    setError(null);
    setBusy(true);
    try {
      const job = await importPdf(selectedBaseId, pdfFile);
      setPdfFile(null);
      setJobs((prev) => [job, ...prev]);
      await refreshBaseData(selectedBaseId);
      setSelectedJobId(job.id);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  async function onRetryJob(jobId: string) {
    setError(null);
    setBusy(true);
    try {
      await retryImportJob(jobId);
      if (selectedBaseId) await refreshBaseData(selectedBaseId);
      setSelectedJobId(jobId);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  async function onCancelJob(jobId: string) {
    setError(null);
    setBusy(true);
    try {
      await cancelImportJob(jobId);
      if (selectedBaseId) await refreshBaseData(selectedBaseId);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  async function onReindexDocument(documentId: string) {
    setError(null);
    setBusy(true);
    try {
      const job = await reindexDocument(documentId);
      if (selectedBaseId) await refreshBaseData(selectedBaseId);
      setSelectedJobId(job.id);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="knowledge">
      <div className="knowledgeLeft">
        <div className="knowledgeLeftHeader">
          <div className="knowledgeTitle">知识库</div>
        </div>
        <div className="knowledgeLeftBody">
          <div className="knowledgeCreate">
            <input
              value={newBaseName}
              onChange={(e) => setNewBaseName(e.target.value)}
              placeholder="新知识库名称"
              disabled={busy}
            />
            <input
              value={newBaseDesc}
              onChange={(e) => setNewBaseDesc(e.target.value)}
              placeholder="描述（可选）"
              disabled={busy}
            />
            <button className="btn primary" onClick={() => onCreateBase().catch(() => {})} disabled={busy}>
              创建
            </button>
          </div>
          <div className="knowledgeList">
            {bases.map((b) => (
              <button
                key={b.id}
                className={b.id === selectedBaseId ? "knowledgeListItem active" : "knowledgeListItem"}
                onClick={() => setSelectedBaseId(b.id)}
                title={b.id}
              >
                <div className="knowledgeListItemTitle">{b.name}</div>
                <div className="knowledgeListItemMeta">{b.updatedAt ?? b.createdAt}</div>
              </button>
            ))}
            {bases.length === 0 ? <div className="muted">还没有知识库，先创建一个。</div> : null}
          </div>
        </div>
      </div>

      <div className="knowledgeMain">
        <div className="knowledgeMainHeader">
          <div className="knowledgeMainTitle">{selectedBase?.name || "请选择一个知识库"}</div>
          <button
            className="btn"
            onClick={() => (selectedBaseId ? refreshBaseData(selectedBaseId).catch(() => {}) : null)}
            disabled={busy || !selectedBaseId}
          >
            刷新
          </button>
        </div>

        {!selectedBaseId ? (
          <div className="muted" style={{ padding: 12 }}>
            选择左侧知识库后可导入文档、查看作业与预览。
          </div>
        ) : (
          <div className="knowledgeGrid">
            <div className="knowledgePanel">
              <div className="knowledgePanelHeader">导入</div>
              <div className="knowledgePanelBody">
                <div className="knowledgeForm">
                  <div className="knowledgeFormTitle">Markdown 文本</div>
                  <input
                    value={mdTitle}
                    onChange={(e) => setMdTitle(e.target.value)}
                    placeholder="标题（可选）"
                    disabled={busy}
                  />
                  <textarea
                    value={mdContent}
                    onChange={(e) => setMdContent(e.target.value)}
                    placeholder="粘贴 Markdown 或纯文本"
                    disabled={busy}
                    rows={6}
                  />
                  <button className="btn primary" onClick={() => onImportMarkdown().catch(() => {})} disabled={busy}>
                    导入
                  </button>
                </div>

                <div className="knowledgeForm">
                  <div className="knowledgeFormTitle">网页</div>
                  <input value={webUrl} onChange={(e) => setWebUrl(e.target.value)} placeholder="URL" disabled={busy} />
                  <input
                    value={webTitle}
                    onChange={(e) => setWebTitle(e.target.value)}
                    placeholder="标题覆盖（可选）"
                    disabled={busy}
                  />
                  <button className="btn primary" onClick={() => onImportWeb().catch(() => {})} disabled={busy}>
                    抓取并导入
                  </button>
                </div>

                <div className="knowledgeForm">
                  <div className="knowledgeFormTitle">PDF</div>
                  <input
                    type="file"
                    accept="application/pdf"
                    onChange={(e) => setPdfFile(e.target.files?.[0] ?? null)}
                    disabled={busy}
                  />
                  <button
                    className="btn primary"
                    onClick={() => onImportPdf().catch(() => {})}
                    disabled={busy || !pdfFile}
                  >
                    上传并导入
                  </button>
                </div>
              </div>
            </div>

            <div className="knowledgePanel">
              <div className="knowledgePanelHeader">文档</div>
              <div className="knowledgePanelBody">
                {docs.length === 0 ? <div className="muted">暂无文档。</div> : null}
                {docs.map((d) => (
                  <div key={d.id} className="knowledgeRow">
                    <div className="knowledgeRowMain">
                      <div className="knowledgeRowTitle">{d.title}</div>
                      <div className="knowledgeRowMeta">
                        {d.sourceType} · {d.parseStatus ?? d.status} · {d.indexStatus ?? "-"}
                      </div>
                      {d.errorMessage ? <div className="knowledgeRowError">{d.errorMessage}</div> : null}
                    </div>
                    <div className="knowledgeRowActions">
                      <button className="btn" onClick={() => onReindexDocument(d.id).catch(() => {})} disabled={busy}>
                        重索引
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="knowledgePanel">
              <div className="knowledgePanelHeader">导入作业</div>
              <div className="knowledgePanelBody">
                {jobs.length === 0 ? <div className="muted">暂无作业。</div> : null}
                {jobs.map((j) => (
                  <div key={j.id} className="knowledgeRow">
                    <button
                      className={j.id === selectedJobId ? "knowledgeJob active" : "knowledgeJob"}
                      onClick={() => setSelectedJobId(j.id)}
                      title={j.id}
                    >
                      <div className="knowledgeRowTitle">
                        {j.status} · {j.stage ?? "-"}
                        {typeof j.progress === "number" ? ` · ${j.progress}%` : ""}
                      </div>
                      {j.errorMessage ? <div className="knowledgeRowError">{j.errorMessage}</div> : null}
                      <div className="knowledgeRowMeta">{j.updatedAt ?? j.createdAt}</div>
                    </button>
                    <div className="knowledgeRowActions">
                      <button className="btn" onClick={() => onRetryJob(j.id).catch(() => {})} disabled={busy}>
                        重试
                      </button>
                      <button className="btn" onClick={() => onCancelJob(j.id).catch(() => {})} disabled={busy}>
                        取消
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="knowledgePanel">
              <div className="knowledgePanelHeader">解析预览</div>
              <div className="knowledgePanelBody">
                {!selectedJobId ? <div className="muted">选择一个作业查看预览。</div> : null}
                {report ? (
                  <div className="knowledgeReport">
                    <div className="knowledgeReportMeta">
                      <div>parser: {report.parserName ?? "-"}</div>
                      <div>chars: {report.extractedChars ?? "-"}</div>
                      <div>chunks: {report.chunkCount ?? "-"}</div>
                      {report.failedAt ? <div>failedAt: {report.failedAt}</div> : null}
                    </div>
                    <pre className="knowledgePreview">{report.samplePreview ?? ""}</pre>
                    {chunkPreview ? (
                      <div className="knowledgeChunkPreview">
                        <div className="knowledgeFormTitle">分片（前 10 条）</div>
                        {chunkPreview.map((c, idx) => (
                          <pre key={idx} className="knowledgePreview">
                            {c}
                          </pre>
                        ))}
                      </div>
                    ) : null}
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        )}

        {error ? <div className="error">错误：{error}</div> : null}
      </div>
    </section>
  );
}

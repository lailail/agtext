import { useEffect, useMemo, useState } from "react";
import {
  chat,
  createConversation,
  listConversations,
  listKnowledgeBases,
  listMessages,
  type Citation,
  type ConversationItem,
  type KnowledgeBaseItem,
  type MessageItem,
} from "../api";

const providers = [
  { value: "", label: "默认" },
  { value: "openai", label: "OpenAI" },
  { value: "qwen", label: "通义千问" },
  { value: "zhipu", label: "智谱 GLM" },
  { value: "ernie", label: "百度文心" },
  { value: "hunyuan", label: "腾讯混元" },
] as const;

export default function ChatPage() {
  const [conversations, setConversations] = useState<ConversationItem[]>([]);
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [provider, setProvider] = useState<string>("");
  const [model, setModel] = useState<string>("");

  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseItem[]>([]);
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState<string>("");
  const [lastCitations, setLastCitations] = useState<Citation[] | null>(null);

  const selectedConversation = useMemo(
    () => conversations.find((c) => c.id === selectedConversationId) ?? null,
    [conversations, selectedConversationId],
  );

  async function refreshConversations() {
    const page = await listConversations(1, 50);
    setConversations(page.items);
    if (!selectedConversationId && page.items.length > 0) {
      setSelectedConversationId(page.items[0].id);
    }
  }

  async function refreshMessages(conversationId: string) {
    setMessages(await listMessages(conversationId));
  }

  useEffect(() => {
    refreshConversations().catch((e: unknown) => setError(String(e)));
    listKnowledgeBases(1, 100)
      .then((page) => setKnowledgeBases(page.items))
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!selectedConversationId) {
      setMessages([]);
      return;
    }
    refreshMessages(selectedConversationId).catch((e: unknown) => setError(String(e)));
  }, [selectedConversationId]);

  async function onNewConversation() {
    setError(null);
    const created = await createConversation();
    setConversations((prev) => [created, ...prev]);
    setSelectedConversationId(created.id);
  }

  async function onSend() {
    const text = input.trim();
    if (!text || busy) return;
    setError(null);
    setBusy(true);
    setLastCitations(null);

    try {
      const res = await chat({
        conversationId: selectedConversationId,
        knowledgeBaseId: selectedKnowledgeBaseId || null,
        message: text,
        provider: provider || null,
        model: model || null,
      });
      setLastCitations(res.citations ?? []);
      setInput("");
      if (!selectedConversationId) {
        setSelectedConversationId(res.conversationId);
      }
      await refreshConversations();
      await refreshMessages(res.conversationId);
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="chat">
      <div className="chatLeft">
        <div className="chatLeftHeader">
          <div className="chatTitle">会话</div>
          <button className="btn" onClick={() => onNewConversation().catch(() => {})}>
            新建
          </button>
        </div>
        <div className="chatList">
          {conversations.map((c) => (
            <button
              key={c.id}
              className={c.id === selectedConversationId ? "chatListItem active" : "chatListItem"}
              onClick={() => setSelectedConversationId(c.id)}
              title={c.id}
            >
              <div className="chatListItemTitle">{c.title || "未命名会话"}</div>
              <div className="chatListItemMeta">{c.updatedAt ?? c.createdAt}</div>
            </button>
          ))}
          {conversations.length === 0 ? <div className="muted">暂无会话，点“新建”。</div> : null}
        </div>
      </div>

      <div className="chatMain">
        <div className="chatMainHeader">
          <div className="chatMainTitle">{selectedConversation?.title || "聊天"}</div>
          <div className="chatControls">
            <label className="control">
              <span>知识库</span>
              <select
                value={selectedKnowledgeBaseId}
                onChange={(e) => setSelectedKnowledgeBaseId(e.target.value)}
              >
                <option value="">不使用</option>
                {knowledgeBases.map((kb) => (
                  <option key={kb.id} value={kb.id}>
                    {kb.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="control">
              <span>提供商</span>
              <select value={provider} onChange={(e) => setProvider(e.target.value)}>
                {providers.map((p) => (
                  <option key={p.value} value={p.value}>
                    {p.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="control">
              <span>模型</span>
              <input value={model} onChange={(e) => setModel(e.target.value)} placeholder="留空用默认" />
            </label>
          </div>
        </div>

        <div className="chatMessages">
          {messages.map((m) => (
            <div key={m.id} className={m.role === "user" ? "msg user" : "msg assistant"}>
              <div className="msgMeta">
                <span className="msgRole">{m.role}</span>
                {m.provider || m.modelName ? (
                  <span className="msgModel">
                    {m.provider ?? ""} {m.modelName ?? ""}
                  </span>
                ) : null}
              </div>
              <div className="msgContent">{m.content}</div>
            </div>
          ))}
          {messages.length === 0 ? <div className="muted">开始对话吧。</div> : null}
        </div>

        <div className="chatComposer">
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") onSend().catch(() => {});
            }}
            placeholder="输入消息，回车发送"
            disabled={busy}
          />
          <button className="btn primary" onClick={() => onSend().catch(() => {})} disabled={busy}>
            {busy ? "发送中…" : "发送"}
          </button>
        </div>

        {lastCitations && lastCitations.length > 0 ? (
          <div className="citations">
            <div className="citationsTitle">引用</div>
            <div className="citationsList">
              {lastCitations.map((c) => (
                <div key={`${c.documentId}:${c.chunkId}`} className="citationsItem">
                  <div className="citationsMeta">
                    <span className="citationsDoc">{c.documentTitle || c.documentId}</span>
                    <span className="citationsScore">{c.score.toFixed(3)}</span>
                  </div>
                  <div className="citationsUri">{c.sourceUri || ""}</div>
                  <div className="citationsExcerpt">{c.excerpt}</div>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        {error ? <div className="error">错误：{error}</div> : null}
      </div>
    </section>
  );
}


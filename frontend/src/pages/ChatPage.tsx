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

// 支持的 AI 模型提供商配置
const providers = [
  { value: "", label: "默认" },
  { value: "openai", label: "OpenAI" },
  { value: "qwen", label: "通义千问" },
  { value: "zhipu", label: "智谱 GLM" },
  { value: "ernie", label: "百度文心" },
  { value: "hunyuan", label: "腾讯混元" },
] as const;

export default function ChatPage() {
  // --- 状态管理 ---
  const [conversations, setConversations] = useState<ConversationItem[]>([]); // 左侧会话列表
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null); // 当前选中的会话 ID
  const [messages, setMessages] = useState<MessageItem[]>([]); // 当前会话的消息记录
  const [input, setInput] = useState(""); // 输入框内容
  const [busy, setBusy] = useState(false); // 请求状态锁，防止重复发送
  const [error, setError] = useState<string | null>(null); // 错误信息展示

  // --- 模型与知识库配置 ---
  const [provider, setProvider] = useState<string>(""); // 当前选中的模型供应商
  const [model, setModel] = useState<string>(""); // 自定义模型名称
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseItem[]>([]); // 可用的知识库列表
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState<string>(""); // 当前关联的知识库 ID
  const [lastCitations, setLastCitations] = useState<Citation[] | null>(null); // 最近一次回答的引用源（RAG 证据）

  // 性能优化：根据 ID 派生当前选中的会话对象，避免冗余查找
  const selectedConversation = useMemo(
    () => conversations.find((c) => c.id === selectedConversationId) ?? null,
    [conversations, selectedConversationId],
  );

  /**
   * 刷新并加载会话列表
   * 逻辑：获取第一页会话，若当前未选中任何会话且列表不为空，则默认选中第一个
   */
  async function refreshConversations() {
    const page = await listConversations(1, 50);
    setConversations(page.items);
    if (!selectedConversationId && page.items.length > 0) {
      setSelectedConversationId(page.items[0].id);
    }
  }

  /**
   * 加载指定会话的所有历史消息
   */
  async function refreshMessages(conversationId: string) {
    setMessages(await listMessages(conversationId));
  }

  // 初始化：组件挂载时加载会话列表和知识库选项
  useEffect(() => {
    refreshConversations().catch((e: unknown) => setError(String(e)));
    listKnowledgeBases(1, 100)
      .then((page) => setKnowledgeBases(page.items))
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 监听：当切换会话 ID 时，重新获取该会话的消息
  useEffect(() => {
    if (!selectedConversationId) {
      setMessages([]);
      return;
    }
    refreshMessages(selectedConversationId).catch((e: unknown) => setError(String(e)));
  }, [selectedConversationId]);

  /**
   * 处理“新建会话”逻辑
   */
  async function onNewConversation() {
    setError(null);
    const created = await createConversation();
    setConversations((prev) => [created, ...prev]); // 将新会话插入列表顶部
    setSelectedConversationId(created.id); // 立即切换到新会话
  }

  /**
   * 核心逻辑：发送消息
   */
  async function onSend() {
    const text = input.trim();
    if (!text || busy) return;

    setError(null);
    setBusy(true);
    setLastCitations(null); // 清除旧的引用展示

    try {
      const res = await chat({
        conversationId: selectedConversationId,
        knowledgeBaseId: selectedKnowledgeBaseId || null, // 若未选则传 null
        message: text,
        provider: provider || null,
        model: model || null,
      });

      // 更新引用源（用于展示 AI 回答的参考文档）
      setLastCitations(res.citations ?? []);
      setInput(""); // 清空输入框

      // 如果是首条消息（之前没 ID），则同步后端生成的会话 ID
      if (!selectedConversationId) {
        setSelectedConversationId(res.conversationId);
      }

      // 同步最新列表状态（如更新会话标题、时间戳等）
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
      {/* --- 左侧边栏：历史会话管理 --- */}
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

      {/* --- 右侧：聊天主区域 --- */}
      <div className="chatMain">
        {/* 工具栏：配置知识库、模型供应商等参数 */}
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
              <input
                value={model}
                onChange={(e) => setModel(e.target.value)}
                placeholder="留空用默认"
              />
            </label>
          </div>
        </div>

        {/* 消息滚动区域 */}
        <div className="chatMessages">
          {messages.map((m) => (
            <div key={m.id} className={m.role === "user" ? "msg user" : "msg assistant"}>
              <div className="msgMeta">
                <span className="msgRole">{m.role}</span>
                {/* 仅在消息包含模型信息时显示，方便辨认多模型回复 */}
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

        {/* 输入框区域 */}
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

        {/* 引用展示：当启用知识库（RAG）且后端返回相关片段时显示 */}
        {lastCitations && lastCitations.length > 0 ? (
          <div className="citations">
            <div className="citationsTitle">引用</div>
            <div className="citationsList">
              {lastCitations.map((c) => (
                <div key={`${c.documentId}:${c.chunkId}`} className="citationsItem">
                  <div className="citationsMeta">
                    <span className="citationsDoc">{c.documentTitle || c.documentId}</span>
                    <span className="citationsScore">{c.score.toFixed(3)}</span>{" "}
                    {/* 检索相关度分数 */}
                  </div>
                  <div className="citationsUri">{c.sourceUri || ""}</div>
                  <div className="citationsExcerpt">{c.excerpt}</div> {/* 片段摘要 */}
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

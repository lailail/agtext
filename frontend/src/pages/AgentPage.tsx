import { useEffect, useMemo, useState } from "react";
import type { AgentRoleInfo } from "../api";
import { listAgentRoles, runAgent } from "../api";

export default function AgentPage() {
  // --- 状态定义 ---
  const [roles, setRoles] = useState<AgentRoleInfo[]>([]); // 存储从后端获取的角色列表
  const [role, setRole] = useState("auto"); // 当前选择的 Agent 角色，默认为自动
  const [provider, setProvider] = useState(""); // 指定模型供应商（可选）
  const [model, setModel] = useState(""); // 指定具体模型名称（可选）
  const [input, setInput] = useState(""); // 用户输入的任务指令
  const [output, setOutput] = useState<string | null>(null); // Agent 返回的结果内容
  const [loading, setLoading] = useState(false); // 请求加载状态，用于 UI 禁用反馈
  const [error, setError] = useState<string | null>(null); // 错误信息捕获

  // --- 衍生状态 ---
  // 根据当前选择的 role，从 roles 数组中匹配并提取描述信息，避免在渲染逻辑中重复遍历
  const roleHelp = useMemo(
    () => roles.find((r) => r.name === role)?.description ?? "",
    [roles, role],
  );

  // --- 初始化数据加载 ---
  useEffect(() => {
    (async () => {
      try {
        const r = await listAgentRoles();
        setRoles(r); // 挂载时拉取角色配置
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      }
    })();
  }, []);

  // --- 核心交互逻辑 ---
  async function onRun() {
    // 校验：防止空输入提交
    if (!input.trim()) {
      setError("请输入内容");
      return;
    }

    setLoading(true);
    setError(null);
    setOutput(null);

    try {
      // 调用 API 执行 Agent 任务
      const r = await runAgent({
        role,
        input: input.trim(),
        // 如果 provider/model 为空字符串，则转换为 null，由后端使用默认值
        provider: provider.trim() ? provider.trim() : null,
        model: model.trim() ? model.trim() : null,
      });
      setOutput(r.content); // 渲染输出结果
    } catch (e) {
      // 捕获并记录请求异常
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false); // 无论成功与否，重置加载状态
    }
  }

  return (
    <section className="agentBoard">
      <div className="agentPanel">
        <h1>Agent Playground</h1>
        <div className="muted">受控子 Agent：只读输出建议，不直接执行写入。</div>

        {/* 配置表单区 */}
        <div className="agentGrid">
          <label className="control">
            <span>角色</span>
            <select value={role} onChange={(e) => setRole(e.target.value)} disabled={loading}>
              <option value="auto">auto（自动）</option>
              {roles.map((r) => (
                <option key={r.name} value={r.name}>
                  {r.name}
                </option>
              ))}
            </select>
          </label>
          <label className="control">
            <span>provider（可选）</span>
            <input
              value={provider}
              onChange={(e) => setProvider(e.target.value)}
              disabled={loading}
            />
          </label>
          <label className="control">
            <span>model（可选）</span>
            <input value={model} onChange={(e) => setModel(e.target.value)} disabled={loading} />
          </label>
        </div>

        {/* 动态提示区：显示当前角色的功能描述 */}
        {roleHelp ? <div className="agentHint muted">{roleHelp}</div> : null}

        {/* 输入与提交区 */}
        <div className="agentComposer">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            rows={6}
            placeholder="输入任务，例如：帮我把“整理并提交周报”拆解成 8 步…"
            disabled={loading}
          />
          <button className="btn primary" onClick={() => void onRun()} disabled={loading}>
            运行
          </button>
        </div>

        {/* 错误与结果展示区 */}
        {error ? <div className="error">{error}</div> : null}

        {output ? (
          <div className="agentOutput">
            <div className="agentOutputTitle">输出</div>
            <pre className="agentOutputBody">{output}</pre>
          </div>
        ) : null}
      </div>
    </section>
  );
}

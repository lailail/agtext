import { useEffect, useMemo, useState } from "react";
import type { AgentRoleInfo } from "../api";
import { listAgentRoles, runAgent } from "../api";

export default function AgentPage() {
  const [roles, setRoles] = useState<AgentRoleInfo[]>([]);
  const [role, setRole] = useState("auto");
  const [provider, setProvider] = useState("");
  const [model, setModel] = useState("");
  const [input, setInput] = useState("");
  const [output, setOutput] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const roleHelp = useMemo(() => roles.find((r) => r.name === role)?.description ?? "", [roles, role]);

  useEffect(() => {
    (async () => {
      try {
        const r = await listAgentRoles();
        setRoles(r);
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      }
    })();
  }, []);

  async function onRun() {
    if (!input.trim()) {
      setError("请输入内容");
      return;
    }
    setLoading(true);
    setError(null);
    setOutput(null);
    try {
      const r = await runAgent({
        role,
        input: input.trim(),
        provider: provider.trim() ? provider.trim() : null,
        model: model.trim() ? model.trim() : null,
      });
      setOutput(r.content);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="agentBoard">
      <div className="agentPanel">
        <h1>Agent Playground</h1>
        <div className="muted">受控子 Agent：只读输出建议，不直接执行写入。</div>

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
            <input value={provider} onChange={(e) => setProvider(e.target.value)} disabled={loading} />
          </label>
          <label className="control">
            <span>model（可选）</span>
            <input value={model} onChange={(e) => setModel(e.target.value)} disabled={loading} />
          </label>
        </div>

        {roleHelp ? <div className="agentHint muted">{roleHelp}</div> : null}

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

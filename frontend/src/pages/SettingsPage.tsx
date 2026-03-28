import { useEffect, useMemo, useState } from "react";
import type { ModelProviderItem, ModelSettingsResponse, ToolSettingsResponse } from "../api";
import {
  getModelSettings,
  getToolSettings,
  setDefaultProvider,
  setProviderModel,
  setToolDomainAllowlist,
  setToolEnabled,
} from "../api";

function parseDomainLines(text: string) {
  return text
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean);
}

export default function SettingsPage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [toolSettings, setToolSettingsState] = useState<ToolSettingsResponse | null>(null);
  const [modelSettings, setModelSettingsState] = useState<ModelSettingsResponse | null>(null);

  const [domainText, setDomainText] = useState("");
  const [defaultProviderValue, setDefaultProviderValue] = useState("");
  const [providerModels, setProviderModels] = useState<Record<string, string>>({});

  const providerList = useMemo<ModelProviderItem[]>(
    () => modelSettings?.providers ?? [],
    [modelSettings?.providers],
  );

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      const [tools, models] = await Promise.all([getToolSettings(), getModelSettings()]);
      setToolSettingsState(tools);
      setModelSettingsState(models);
      setDomainText((tools.domainAllowlist ?? []).join("\n"));
      setDefaultProviderValue(models.defaultProvider ?? "");
      const map: Record<string, string> = {};
      for (const p of models.providers) {
        map[p.name] = p.model ?? "";
      }
      setProviderModels(map);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void refresh();
  }, []);

  async function onSaveDomains() {
    setLoading(true);
    setError(null);
    try {
      await setToolDomainAllowlist(parseDomainLines(domainText));
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onToggleTool(name: string, enabled: boolean) {
    setLoading(true);
    setError(null);
    try {
      await setToolEnabled(name, enabled);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onSaveDefaultProvider() {
    if (!defaultProviderValue.trim()) {
      setError("defaultProvider 不能为空");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await setDefaultProvider(defaultProviderValue.trim());
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onSaveProviderModel(name: string) {
    const value = providerModels[name] ?? "";
    if (!value.trim()) {
      setError("model 不能为空");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await setProviderModel(name, value.trim());
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="settingsBoard">
      <div className="settingsPanel">
        <h1>系统设置</h1>
        <div className="muted">本页修改会写入本地数据库（app_settings），用于开发期动态调整。</div>

        <div className="settingsRow">
          <div className="settingsRowTitle">工具开关</div>
          <div className="muted">可禁用某个工具，避免误调用。</div>
          <div className="settingsTools">
            {(toolSettings?.tools ?? []).map((t) => (
              <div key={t.name} className="settingsToolItem">
                <div className="settingsToolMain">
                  <div className="settingsToolName">{t.name}</div>
                  <div className="muted">{t.description}</div>
                  <div className="muted">
                    type={t.type} · requiresConfirmation={String(t.requiresConfirmation)}
                  </div>
                </div>
                <div className="settingsToolActions">
                  <button
                    className={t.enabled ? "btn primary" : "btn"}
                    onClick={() => void onToggleTool(t.name, true)}
                    disabled={loading}
                  >
                    启用
                  </button>
                  <button
                    className={!t.enabled ? "btn primary" : "btn"}
                    onClick={() => void onToggleTool(t.name, false)}
                    disabled={loading}
                  >
                    禁用
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="settingsDivider" />

          <div className="settingsRowTitle">工具域名 allowlist（可选）</div>
          <div className="muted">留空=全放开（开发期推荐）。每行一个域名（如 zh.wikipedia.org）。</div>
          <textarea value={domainText} onChange={(e) => setDomainText(e.target.value)} rows={5} disabled={loading} />
          <button className="btn primary" onClick={() => void onSaveDomains()} disabled={loading}>
            保存 allowlist
          </button>
        </div>

        <div className="settingsDivider" />

        <div className="settingsRow">
          <div className="settingsRowTitle">模型配置（运行时覆盖）</div>
          <div className="muted">仅覆盖 defaultProvider 与 providers.*.model（baseUrl/apiKey 仍来自 env）。</div>

          <div className="settingsGrid2">
            <label className="settingsField">
              <div className="settingsFieldLabel">defaultProvider</div>
              <input
                value={defaultProviderValue}
                onChange={(e) => setDefaultProviderValue(e.target.value)}
                disabled={loading}
              />
            </label>
            <div className="settingsField">
              <div className="settingsFieldLabel">fallbackModel</div>
              <div className="muted">{modelSettings?.fallbackModel ?? "-"}</div>
            </div>
          </div>
          <button className="btn primary" onClick={() => void onSaveDefaultProvider()} disabled={loading}>
            保存 defaultProvider
          </button>

          <div className="settingsProviders">
            {providerList.map((p) => (
              <div key={p.name} className="settingsProviderItem">
                <div className="settingsProviderTitle">{p.name}</div>
                <div className="muted">
                  baseUrl={p.baseUrl ?? "-"} · apiKeyConfigured={String(p.apiKeyConfigured)}
                </div>
                <div className="settingsGrid2">
                  <label className="settingsField">
                    <div className="settingsFieldLabel">model</div>
                    <input
                      value={providerModels[p.name] ?? ""}
                      onChange={(e) => setProviderModels((m) => ({ ...m, [p.name]: e.target.value }))}
                      disabled={loading}
                    />
                  </label>
                  <div className="settingsField">
                    <div className="settingsFieldLabel">&nbsp;</div>
                    <button className="btn" onClick={() => void onSaveProviderModel(p.name)} disabled={loading}>
                      保存
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="settingsDivider" />
        <button className="btn" onClick={() => void refresh()} disabled={loading}>
          刷新
        </button>

        {error ? <div className="error">{error}</div> : null}
      </div>
    </section>
  );
}

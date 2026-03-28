import { useEffect, useState } from "react";
import {
  archiveNotification,
  listNotifications,
  markNotificationRead,
  type NotificationItem,
} from "../api";

export default function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function refresh() {
    const page = await listNotifications("unread", 1, 50);
    setItems(page.items);
  }

  useEffect(() => {
    refresh().catch((e: unknown) => setError(String(e)));
  }, []);

  async function onRead(id: string) {
    setBusy(true);
    setError(null);
    try {
      await markNotificationRead(id);
      await refresh();
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  async function onArchive(id: string) {
    setBusy(true);
    setError(null);
    try {
      await archiveNotification(id);
      await refresh();
    } catch (e: unknown) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <h1>通知</h1>
      <p className="muted">当前仅展示未读通知（Phase 2 最小实现）。</p>

      <div className="ntfList">
        {items.map((n) => (
          <div key={n.id} className="ntfItem">
            <div className="ntfTitle">{n.title}</div>
            {n.content ? <div className="ntfContent">{n.content}</div> : null}
            <div className="ntfActions">
              <button className="btn" onClick={() => onRead(n.id).catch(() => {})} disabled={busy}>
                标记已读
              </button>
              <button className="btn" onClick={() => onArchive(n.id).catch(() => {})} disabled={busy}>
                归档
              </button>
            </div>
          </div>
        ))}
        {items.length === 0 ? <div className="muted">暂无未读通知。</div> : null}
      </div>

      {error ? <div className="error">错误：{error}</div> : null}
    </section>
  );
}


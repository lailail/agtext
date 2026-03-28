import { NavLink, Route, Routes } from "react-router-dom";
import ChatPage from "./pages/ChatPage";
import NotificationsPage from "./pages/NotificationsPage";
import KnowledgePage from "./pages/KnowledgePage";
import TaskPage from "./pages/TaskPage";
import MemoryPage from "./pages/MemoryPage";
import AgentPage from "./pages/AgentPage";
import SettingsPage from "./pages/SettingsPage";

const navItems = [
  { to: "/chat", label: "聊天" },
  { to: "/notifications", label: "通知" },
  { to: "/knowledge", label: "知识库" },
  { to: "/task", label: "任务/计划" },
  { to: "/memory", label: "记忆" },
  { to: "/agent", label: "Agent" },
  { to: "/settings", label: "系统设置" },
] as const;

export default function App() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">agtext</div>
        <nav className="nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? "navItem active" : "navItem")}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="content">
        <Routes>
          <Route path="/" element={<ChatPage />} />
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/knowledge" element={<KnowledgePage />} />
          <Route path="/task" element={<TaskPage />} />
          <Route path="/memory" element={<MemoryPage />} />
          <Route path="/agent" element={<AgentPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Routes>
      </main>
    </div>
  );
}

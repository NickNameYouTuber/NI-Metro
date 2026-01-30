import { Routes, Route, Navigate } from 'react-router-dom';
import { HomePage } from './modules/app/HomePage';
import { EditorPage } from './modules/app/EditorPage';
import { DocsPage } from './modules/docs/DocsPage';
import { ApiKeySettings } from './modules/auth/ApiKeySettings';
import { useAuth } from './modules/auth/AuthContext';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/editor/:id" element={<EditorPage />} />
      <Route path="/docs" element={<DocsPage />} />
      <Route path="/settings" element={<ApiKeySettings />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}


import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { apiClient } from '../../api/client';

interface AuthContextType {
  apiKey: string | null;
  hasApiKey: boolean;
  setApiKey: (key: string | null) => void;
  clearApiKey: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [apiKey, setApiKeyState] = useState<string | null>(null);

  useEffect(() => {
    // Load API key from storage on mount
    const stored = apiClient.getApiKey();
    if (stored) {
      setApiKeyState(stored);
    }
  }, []);

  const setApiKey = (key: string | null) => {
    if (key) {
      apiClient.setApiKey(key);
      setApiKeyState(key);
    } else {
      apiClient.clearApiKey();
      setApiKeyState(null);
    }
  };

  const clearApiKey = () => {
    apiClient.clearApiKey();
    setApiKeyState(null);
  };

  return (
    <AuthContext.Provider
      value={{
        apiKey,
        hasApiKey: apiKey !== null,
        setApiKey,
        clearApiKey,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}


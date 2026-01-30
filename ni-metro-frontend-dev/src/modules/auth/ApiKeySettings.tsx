import React, { useState } from 'react';
import { Button, TextInput, Paper, Title, Stack, Text, Alert, AppShell, NavLink, Group } from '@mantine/core';
import { useNavigate, useLocation } from 'react-router-dom';
import { IconHome, IconBook, IconSettings } from '@tabler/icons-react';
import { useAuth } from './AuthContext';

export function ApiKeySettings() {
  const navigate = useNavigate();
  const location = useLocation();
  const { apiKey, setApiKey, clearApiKey } = useAuth();
  const [inputKey, setInputKey] = useState(apiKey || '');
  const [error, setError] = useState<string | null>(null);

  const handleSave = () => {
    if (!inputKey.trim()) {
      setError('API ключ не может быть пустым');
      return;
    }
    setError(null);
    setApiKey(inputKey.trim());
  };

  const handleClear = () => {
    setInputKey('');
    clearApiKey();
    setError(null);
  };

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 200, breakpoint: 'sm' }}
      padding="md"
      style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}
    >
      <AppShell.Header>
        <Group justify="space-between" align="center" h="100%" px="md">
          <Title order={3}>Настройки</Title>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <NavLink
          label="Проекты"
          leftSection={<IconHome size={16} />}
          onClick={() => navigate('/')}
          active={location.pathname === '/'}
        />
        <NavLink
          label="Документация"
          leftSection={<IconBook size={16} />}
          onClick={() => navigate('/docs')}
          active={location.pathname === '/docs'}
        />
        <NavLink
          label="Настройки"
          leftSection={<IconSettings size={16} />}
          onClick={() => navigate('/settings')}
          active={location.pathname === '/settings'}
        />
      </AppShell.Navbar>

      <AppShell.Main>
        <div style={{ maxWidth: 800, margin: '0 auto' }}>
          <Paper p="md" withBorder>
            <Stack gap="md">
              <div>
                <Title order={4}>API ключ</Title>
                <Text size="sm" c="dimmed">
                  Введите API ключ для доступа к защищенным endpoints. Публичные endpoints (чтение карт и оповещений) работают без ключа.
                </Text>
              </div>

              {error && (
                <Alert color="red" title="Ошибка">
                  {error}
                </Alert>
              )}

              {apiKey && (
                <Alert color="green" title="API ключ сохранен">
                  API ключ сохранен и будет использоваться для всех запросов.
                </Alert>
              )}

              <TextInput
                label="API ключ"
                placeholder="nmi-..."
                value={inputKey}
                onChange={(e) => {
                  setInputKey(e.currentTarget.value);
                  setError(null);
                }}
                type="password"
                description="Ключ отправляется в заголовке X-API-Key"
              />

              <div style={{ display: 'flex', gap: '8px' }}>
                <Button onClick={handleSave} disabled={!inputKey.trim()}>
                  Сохранить
                </Button>
                {apiKey && (
                  <Button variant="outline" onClick={handleClear}>
                    Очистить
                  </Button>
                )}
              </div>
            </Stack>
          </Paper>
        </div>
      </AppShell.Main>
    </AppShell>
  );
}


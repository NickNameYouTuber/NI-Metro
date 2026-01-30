import React, { useEffect, useMemo, useState } from 'react';
import { Button, Group, Paper, Stack, TextInput, Title, FileInput, Badge, AppShell, NavLink } from '@mantine/core';
import { useNavigate, useLocation } from 'react-router-dom';
import { IconHome, IconBook, IconSettings } from '@tabler/icons-react';
import { mapsApi, MapListItem } from '../../api/maps';
import { useAuth } from '../auth/AuthContext';

export function HomePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { hasApiKey } = useAuth();
  const [projects, setProjects] = useState<MapListItem[]>([]);
  const [filter, setFilter] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadMaps();
  }, []);

  const loadMaps = async () => {
    try {
      setLoading(true);
      const maps = await mapsApi.getAll();
      setProjects(maps);
    } catch (error) {
      console.error('Error loading maps:', error);
    } finally {
      setLoading(false);
    }
  };

  const filtered = useMemo(
    () => projects.filter((p) => p.name.toLowerCase().includes(filter.toLowerCase())),
    [projects, filter]
  );

  const createProject = async () => {
    if (!hasApiKey) {
      alert('Для создания проекта необходим API ключ. Настройте его в разделе Settings.');
      return;
    }

    try {
      const name = `Проект ${projects.length + 1}`;
      const newMap = await mapsApi.create({
        name,
        fileName: `project_${Date.now()}`,
        data: { info: { name }, metro_map: { lines: [] } },
      });
      navigate(`/editor/${newMap.id}`);
    } catch (error) {
      console.error('Error creating project:', error);
      alert('Ошибка при создании проекта');
    }
  };

  const importProject = async (file: File | null) => {
    if (!file || !hasApiKey) {
      if (!hasApiKey) {
        alert('Для импорта проекта необходим API ключ. Настройте его в разделе Settings.');
      }
      return;
    }

    try {
      const text = await file.text();
      const json = JSON.parse(text);
      const name = json?.info?.name || `Проект ${projects.length + 1}`;
      const newMap = await mapsApi.create({
        name,
        fileName: `project_${Date.now()}`,
        data: json,
      });
      navigate(`/editor/${newMap.id}`);
    } catch (e) {
      console.error('Error importing project:', e);
      alert('Ошибка при импорте проекта');
    }
  };

  const renameProject = async (id: string, current: string) => {
    if (!hasApiKey) {
      alert('Для переименования проекта необходим API ключ.');
      return;
    }

    const name = prompt('Новое имя проекта', current || 'Проект');
    if (!name) return;

    try {
      const map = await mapsApi.getById(id);
      await mapsApi.update(id, { ...map, name });
      await loadMaps();
    } catch (error) {
      console.error('Error renaming project:', error);
      alert('Ошибка при переименовании проекта');
    }
  };

  const deleteProject = async (id: string) => {
    if (!hasApiKey) {
      alert('Для удаления проекта необходим API ключ.');
      return;
    }

    if (!confirm('Удалить проект без возможности восстановления?')) return;

    try {
      await mapsApi.delete(id);
      await loadMaps();
    } catch (error) {
      console.error('Error deleting project:', error);
      alert('Ошибка при удалении проекта');
    }
  };

  const exportProject = async (id: string) => {
    try {
      const data = await mapsApi.getById(id);
      const json = JSON.stringify(data.data, null, 2);
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      const name = (data.name || 'map').toString().replace(/[^\w\-]+/g, '_');
      a.download = `${name}.json`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error exporting project:', error);
      alert('Ошибка при экспорте проекта');
    }
  };

  const duplicateProject = async (id: string) => {
    if (!hasApiKey) {
      alert('Для дублирования проекта необходим API ключ.');
      return;
    }

    try {
      const data = await mapsApi.getById(id);
      const newName = `${data.name || 'Проект'} (копия)`;
      const newMap = await mapsApi.create({
        ...data,
        name: newName,
        fileName: `project_${Date.now()}`,
        data: { ...data.data, info: { ...(data.data?.info || {}), name: newName } },
      });
      await loadMaps();
      navigate(`/editor/${newMap.id}`);
    } catch (error) {
      console.error('Error duplicating project:', error);
      alert('Ошибка при дублировании проекта');
    }
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
          <Title order={3}>NI‑Metro — проекты карт</Title>
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
        {!hasApiKey && (
          <Paper p="md" mb="md" withBorder style={{ backgroundColor: '#fff3cd' }}>
            <strong>Внимание:</strong> Для создания, редактирования и удаления проектов необходим API ключ.
            Настройте его в разделе <Button variant="subtle" size="xs" onClick={() => navigate('/settings')}>Settings</Button>.
            Чтение проектов доступно без ключа.
          </Paper>
        )}

        <Group justify="space-between" mt={12} mb={12}>
          <Group>
            <Button onClick={createProject} disabled={!hasApiKey}>
              Новый проект
            </Button>
            <FileInput
              accept="application/json"
              placeholder="Импорт проекта (.json)"
              onChange={importProject}
              disabled={!hasApiKey}
            />
          </Group>
          <TextInput placeholder="Поиск проекта" value={filter} onChange={(e) => setFilter(e.currentTarget.value)} />
        </Group>

        {loading ? (
          <div>Загрузка...</div>
        ) : (
          <Stack>
            {filtered.map((p) => (
              <Paper key={p.id} withBorder p={8} radius="md">
                <Group justify="space-between" align="center">
                  <div>
                    <div style={{ fontWeight: 600 }}>{p.name}</div>
                    <div style={{ fontSize: 12, opacity: 0.6 }}>
                      Обновлено: {new Date(p.updatedAt).toLocaleString()}
                    </div>
                  </div>
                  <Group>
                    <Button size="xs" onClick={() => navigate(`/editor/${p.id}`)}>
                      Открыть
                    </Button>
                    {hasApiKey && (
                      <>
                        <Button size="xs" variant="light" onClick={() => renameProject(p.id, p.name)}>
                          Переименовать
                        </Button>
                        <Button size="xs" variant="light" onClick={() => duplicateProject(p.id)}>
                          Дублировать
                        </Button>
                      </>
                    )}
                    <Button size="xs" variant="light" onClick={() => exportProject(p.id)}>
                      Экспорт
                    </Button>
                    {hasApiKey && (
                      <Button size="xs" color="red" onClick={() => deleteProject(p.id)}>
                        Удалить
                      </Button>
                    )}
                  </Group>
                </Group>
              </Paper>
            ))}
            {filtered.length === 0 && !loading && <div style={{ opacity: 0.6 }}>Нет проектов</div>}
          </Stack>
        )}
      </AppShell.Main>
    </AppShell>
  );
}


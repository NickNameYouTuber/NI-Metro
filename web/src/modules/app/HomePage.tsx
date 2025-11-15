import React, { useEffect, useMemo, useState } from 'react';
import { Button, Group, Paper, Stack, TextInput, Title, FileInput } from '@mantine/core';
import { useNavigate } from 'react-router-dom';
import { storage } from './storage';

type ProjectListItem = { id: string; name: string; updatedAt: number };

export function HomePage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<ProjectListItem[]>([]);
  const [filter, setFilter] = useState('');

  useEffect(() => {
    storage.list().then(setProjects).catch(() => setProjects([]));
  }, []);

  const filtered = useMemo(() => projects.filter(p => p.name.toLowerCase().includes(filter.toLowerCase())), [projects, filter]);

  const createProject = async () => {
    const id = crypto.randomUUID();
    const name = `Проект ${projects.length + 1}`;
    const now = Date.now();
    await storage.putProject({ id, name, updatedAt: now });
    await storage.put(id, { info: { name }, metro_map: { lines: [] } }, name);
    navigate(`/editor/${id}`);
  };

  const importProject = async (file: File | null) => {
    if (!file) return;
    try {
      const text = await file.text();
      const json = JSON.parse(text);
      const id = crypto.randomUUID();
      const name = json?.info?.name || `Проект ${projects.length + 1}`;
      const now = Date.now();
      await storage.putProject({ id, name, updatedAt: now });
      await storage.put(id, json, name);
      navigate(`/editor/${id}`);
    } catch (e) {
      // noop
    }
  };

  const refresh = async () => {
    const list = await storage.list();
    setProjects(list);
  };

  const renameProject = async (id: string, current: string) => {
    const name = prompt('Новое имя проекта', current || 'Проект');
    if (!name) return;
    // update metadata only
    await storage.putProject({ id, name, updatedAt: Date.now() });
    await refresh();
  };

  const deleteProject = async (id: string) => {
    if (!confirm('Удалить проект без возможности восстановления?')) return;
    await storage.deleteProject(id);
    await refresh();
  };

  const exportProject = async (id: string) => {
    const data = await storage.get(id);
    if (!data) return;
    const json = JSON.stringify(data, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const name = (data.info?.name || 'map').toString().replace(/[^\w\-]+/g, '_');
    a.download = `${name}.json`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  };

  const duplicateProject = async (id: string) => {
    const data = await storage.get(id);
    if (!data) return;
    const newId = crypto.randomUUID();
    const newName = `${data.info?.name || 'Проект'} (копия)`;
    await storage.putProject({ id: newId, name: newName, updatedAt: Date.now() });
    await storage.put(newId, { ...data, info: { ...(data.info || {}), name: newName } }, newName);
    navigate(`/editor/${newId}`);
  };

  return (
    <div style={{ padding: 16 }}>
      <Title order={3}>NI‑Metro — проекты карт</Title>
      <Group justify="space-between" mt={12} mb={12}>
        <Group>
          <Button onClick={createProject}>Новый проект</Button>
          <FileInput accept="application/json" placeholder="Импорт проекта (.json)" onChange={importProject} />
        </Group>
        <TextInput placeholder="Поиск проекта" value={filter} onChange={(e) => setFilter(e.currentTarget.value)} />
      </Group>
      <Stack>
        {filtered.map(p => (
          <Paper key={p.id} withBorder p={8} radius="md">
            <Group justify="space-between" align="center">
              <div>
                <div style={{ fontWeight: 600 }}>{p.name}</div>
                <div style={{ fontSize: 12, opacity: 0.6 }}>Обновлено: {new Date(p.updatedAt).toLocaleString()}</div>
              </div>
              <Group>
                <Button size="xs" onClick={() => navigate(`/editor/${p.id}`)}>Открыть</Button>
                <Button size="xs" variant="light" onClick={() => renameProject(p.id, p.name)}>Переименовать</Button>
                <Button size="xs" variant="light" onClick={() => duplicateProject(p.id)}>Дублировать</Button>
                <Button size="xs" variant="light" onClick={() => exportProject(p.id)}>Экспорт</Button>
                <Button size="xs" color="red" onClick={() => deleteProject(p.id)}>Удалить</Button>
              </Group>
            </Group>
          </Paper>
        ))}
        {filtered.length === 0 && <div style={{ opacity: 0.6 }}>Нет проектов</div>}
      </Stack>
    </div>
  );
}



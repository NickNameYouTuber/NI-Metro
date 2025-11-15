import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Group, Button, TextInput, Badge } from '@mantine/core';
import { PixiMapEditor } from '../editor/PixiMapEditor';
import { storage } from './storage';

export function EditorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [projectName, setProjectName] = useState('');
  const [data, setData] = useState<any | null>(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const saveTimer = useRef<any>(null);

  useEffect(() => {
    if (!id) { navigate('/'); return; }
    console.log('[EditorPage] load start id=', id);
    storage.get(id).then((parsed) => {
      console.log('[EditorPage] loaded raw from storage:', parsed);
      if (!parsed) { navigate('/'); return; }
      // Нормализуем структуру, если корень не содержит metro_map
      let normalized = parsed;
      if (!normalized.metro_map && Array.isArray(normalized.lines)) {
        normalized = { info: normalized.info || {}, metro_map: { lines: normalized.lines || [], transfers: normalized.transfers || [], intermediatePoints: normalized.intermediatePoints || [] } };
      } else if (!normalized.metro_map && (normalized.metromap_1 || normalized.metro_map_1 || normalized.metromap)) {
        const mm = normalized.metromap_1 || normalized.metro_map_1 || normalized.metromap;
        normalized = { info: normalized.info || {}, metro_map: { lines: mm.lines || [], transfers: mm.transfers || [], intermediatePoints: mm.intermediatePoints || [] } };
      }
      console.log('[EditorPage] normalized for editor:', normalized);
      setData(normalized);
      setProjectName(parsed?.info?.name || `Проект ${id}`);
      // Завершаем загрузку после того как данные установлены
      setTimeout(() => { setLoading(false); }, 500);
    }).catch((e) => { console.error('[EditorPage] load error', e); navigate('/'); });
  }, [id]);

  const debouncedSave = (next: any) => {
    // Не сохраняем пока идет загрузка
    if (loading) return;
    console.log('[EditorPage] debouncedSave ->', next?.info?.name, next?.metro_map?.lines?.length, 'lines');
    setSaving(true);
    if (saveTimer.current) clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(() => {
      try {
        console.log('[EditorPage] saving to storage...');
        storage.put(id!, next, next?.info?.name);
      } finally {
        setSaving(false);
        console.log('[EditorPage] save done');
      }
    }, 500);
  };

  const handleEditorChange = (next: any) => {
    console.log('[EditorPage] onChange from editor. lines=', next?.metro_map?.lines?.length);
    setData(next);
    debouncedSave(next);
  };

  const handleRename = (name: string) => {
    setProjectName(name);
    const next = { ...(data || {}), info: { ...(data?.info || {}), name } };
    handleEditorChange(next);
  };

  if (!id || !data) return null;

  return (
    <div style={{ padding: 12 }}>
      <Group justify="space-between" align="center" mb={8}>
        <Group>
          <TextInput value={projectName} onChange={(e) => handleRename(e.currentTarget.value)} />
          {loading ? <Badge color="blue">Загрузка…</Badge> : saving ? <Badge color="yellow">Сохранение…</Badge> : <Badge color="green">Сохранено</Badge>}
        </Group>
        <Button variant="subtle" onClick={() => navigate('/')}>К проектам</Button>
      </Group>

      <PixiMapEditor mapPath="" initialData={data} onChange={handleEditorChange} />
    </div>
  );
}



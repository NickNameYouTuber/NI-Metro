import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { AppShell, Group, Button, TextInput, Badge, NavLink } from '@mantine/core';
import { IconHome, IconBook, IconSettings } from '@tabler/icons-react';
import { PixiMapEditor } from '../../modules/editor/PixiMapEditor';
import { mapsApi } from '../../api/maps';
import { useAuth } from '../auth/AuthContext';

export function EditorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasApiKey } = useAuth();
  const [projectName, setProjectName] = useState('');
  const [data, setData] = useState<any | null>(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const saveTimer = useRef<any>(null);

  useEffect(() => {
    if (!id) {
      navigate('/');
      return;
    }
    console.log('[EditorPage] load start id=', id);
    loadMap();
  }, [id]);

  const loadMap = async () => {
    try {
      setLoading(true);
      const map = await mapsApi.getById(id!);
      console.log('[EditorPage] loaded from API:', map);

      // Normalize structure
      let normalized = map.data;
      if (!normalized.metro_map && Array.isArray(normalized.lines)) {
        normalized = {
          info: normalized.info || {},
          metro_map: {
            lines: normalized.lines || [],
            transfers: normalized.transfers || [],
            intermediatePoints: normalized.intermediatePoints || [],
          },
        };
      } else if (!normalized.metro_map && (normalized.metromap_1 || normalized.metro_map_1 || normalized.metromap)) {
        const mm = normalized.metromap_1 || normalized.metro_map_1 || normalized.metromap;
        normalized = {
          info: normalized.info || {},
          metro_map: {
            lines: mm.lines || [],
            transfers: mm.transfers || [],
            intermediatePoints: mm.intermediatePoints || [],
          },
        };
      }

      console.log('[EditorPage] normalized for editor:', normalized);
      setData(normalized);
      setProjectName(map.name || `Проект ${id}`);
    } catch (e) {
      console.error('[EditorPage] load error', e);
      navigate('/');
    } finally {
      setLoading(false);
    }
  };

  const debouncedSave = (next: any) => {
    if (loading || !hasApiKey) return;

    console.log('[EditorPage] debouncedSave ->', next?.info?.name, next?.metro_map?.lines?.length, 'lines');
    setSaving(true);
    if (saveTimer.current) clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(async () => {
      try {
        console.log('[EditorPage] saving to API...');
        const currentMap = await mapsApi.getById(id!);
        await mapsApi.update(id!, {
          ...currentMap,
          name: projectName,
          data: next,
        });
      } catch (error) {
        console.error('[EditorPage] save error', error);
        alert('Ошибка при сохранении');
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
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 200, breakpoint: 'sm' }}
      padding="md"
      style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}
    >
      <AppShell.Header>
        <Group justify="space-between" align="center" h="100%" px="md">
          <TextInput
            value={projectName}
            onChange={(e) => handleRename(e.currentTarget.value)}
            variant="unstyled"
            style={{ fontSize: '1.2rem', fontWeight: 600, flex: 1 }}
            placeholder="Название проекта"
          />
          <Group gap="sm">
            {loading ? (
              <Badge color="blue" size="sm">Загрузка…</Badge>
            ) : saving ? (
              <Badge color="yellow" size="sm">Сохранение…</Badge>
            ) : hasApiKey ? (
              <Badge color="green" size="sm">Сохранено</Badge>
            ) : (
              <Badge color="orange" size="sm">Только чтение</Badge>
            )}
            <Button variant="subtle" size="sm" onClick={() => navigate('/')}>
              К проектам
            </Button>
          </Group>
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

      <AppShell.Main style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden', height: '100%' }}>
        {!hasApiKey && (
          <div style={{ padding: '12px', backgroundColor: '#fff3cd', borderRadius: '8px', marginBottom: '12px' }}>
            <strong>Внимание:</strong> Сохранение изменений недоступно без API ключа. Настройте его в разделе Settings.
          </div>
        )}

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minHeight: 0 }}>
          <PixiMapEditor mapPath="" initialData={data} onChange={handleEditorChange} projectName={projectName} saving={saving} loading={loading} hasApiKey={hasApiKey} />
        </div>
      </AppShell.Main>
    </AppShell>
  );
}

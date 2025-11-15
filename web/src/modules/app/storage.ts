// Simple IndexedDB storage with localStorage fallback

type ProjectListItem = { id: string; name: string; updatedAt: number };

const DB_NAME = 'nimetro';
const DB_VERSION = 1;
const STORE_PROJECTS = 'projects'; // key: id, value: ProjectListItem
const STORE_DATA = 'projectData'; // key: id, value: any

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE_PROJECTS)) {
        db.createObjectStore(STORE_PROJECTS, { keyPath: 'id' });
      }
      if (!db.objectStoreNames.contains(STORE_DATA)) {
        db.createObjectStore(STORE_DATA);
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function idbGetAllProjects(): Promise<ProjectListItem[]> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_PROJECTS, 'readonly');
    const store = tx.objectStore(STORE_PROJECTS);
    const req = store.getAll();
    req.onsuccess = () => resolve(req.result as ProjectListItem[]);
    req.onerror = () => reject(req.error);
  });
}

async function idbPutProject(item: ProjectListItem): Promise<void> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_PROJECTS, 'readwrite');
    const store = tx.objectStore(STORE_PROJECTS);
    store.put(item);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function idbDeleteProject(id: string): Promise<void> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_PROJECTS, 'readwrite');
    tx.objectStore(STORE_PROJECTS).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function idbGetData(id: string): Promise<any | null> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_DATA, 'readonly');
    const req = tx.objectStore(STORE_DATA).get(id);
    req.onsuccess = () => resolve(req.result ?? null);
    req.onerror = () => reject(req.error);
  });
}

async function idbPutData(id: string, data: any): Promise<void> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_DATA, 'readwrite');
    tx.objectStore(STORE_DATA).put(data, id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

// Fallback to localStorage
function lsGetAllProjects(): ProjectListItem[] {
  try { return JSON.parse(localStorage.getItem('nimetro.projects') || '[]'); } catch { return []; }
}
function lsPutProject(item: ProjectListItem) {
  const list = lsGetAllProjects();
  const exists = list.some(p => p.id === item.id);
  const next = exists ? list.map(p => p.id === item.id ? item : p) : [...list, item];
  localStorage.setItem('nimetro.projects', JSON.stringify(next));
}
function lsDeleteProject(id: string) {
  const list = lsGetAllProjects().filter(p => p.id !== id);
  localStorage.setItem('nimetro.projects', JSON.stringify(list));
  localStorage.removeItem(`nimetro.project.${id}`);
}
function lsGetData(id: string) {
  try { return JSON.parse(localStorage.getItem(`nimetro.project.${id}`) || 'null'); } catch { return null; }
}
function lsPutData(id: string, data: any) {
  localStorage.setItem(`nimetro.project.${id}`, JSON.stringify(data));
}

const canUseIDB = (() => {
  try { return typeof indexedDB !== 'undefined'; } catch { return false; }
})();

export const storage = {
  async list(): Promise<ProjectListItem[]> {
    if (!canUseIDB) return lsGetAllProjects();
    try { return await idbGetAllProjects(); } catch { return lsGetAllProjects(); }
  },
  async get(id: string): Promise<any | null> {
    if (!canUseIDB) return lsGetData(id);
    try { return await idbGetData(id); } catch { return lsGetData(id); }
  },
  async put(id: string, data: any, name?: string) {
    if (canUseIDB) {
      try {
        await idbPutData(id, data);
        if (name) await idbPutProject({ id, name, updatedAt: Date.now() });
        return;
      } catch {}
    }
    lsPutData(id, data);
    if (name) lsPutProject({ id, name, updatedAt: Date.now() });
  },
  async putProject(item: ProjectListItem) {
    if (!canUseIDB) { lsPutProject(item); return; }
    try { await idbPutProject(item); } catch { lsPutProject(item); }
  },
  async deleteProject(id: string) {
    if (!canUseIDB) { lsDeleteProject(id); return; }
    try { await idbDeleteProject(id); } catch { lsDeleteProject(id); }
  },
};



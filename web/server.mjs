import express from 'express';
import cors from 'cors';
import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
// Проект: web/ → корень = на уровень выше
const projectRoot = path.resolve(__dirname, '..');
const baseDir = path.resolve(projectRoot, '');

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

function resolveSafe(p) {
  const joined = path.resolve(baseDir, p);
  if (!joined.startsWith(baseDir)) {
    throw new Error('Path escapes project root');
  }
  return joined;
}

app.get('/api/load', async (req, res) => {
  try {
    const relPath = req.query.path;
    if (typeof relPath !== 'string' || !relPath.length) return res.status(400).json({ error: 'path required' });
    const abs = resolveSafe(relPath);
    const data = await fs.readFile(abs, 'utf8');
    res.json({ ok: true, content: data });
  } catch (e) {
    res.status(500).json({ error: String(e) });
  }
});

app.post('/api/save', async (req, res) => {
  try {
    const { path: relPath, content } = req.body || {};
    if (typeof relPath !== 'string' || !relPath.length) return res.status(400).json({ error: 'path required' });
    if (typeof content !== 'string' || !content.length) return res.status(400).json({ error: 'content required' });
    const abs = resolveSafe(relPath);
    await fs.mkdir(path.dirname(abs), { recursive: true });
    await fs.writeFile(abs, content, 'utf8');
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: String(e) });
  }
});

const PORT = process.env.PORT || 5174;
app.listen(PORT, () => {
  console.log(`NI-Metro web API running at http://localhost:${PORT}`);
});



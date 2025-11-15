import React from 'react';
import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';
import { createRoot } from 'react-dom/client';
import { App } from './modules/app/App';

const container = document.getElementById('root');
if (!container) throw new Error('Root container not found');
createRoot(container).render(<App />);



import { defineConfig } from 'vitest/config';

export default defineConfig({
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
    // design-system/ vive fuera de frontend/ y se importa sin copiarlo (design D2).
    fs: { allow: ['..'] },
  },
  test: {
    environment: 'happy-dom',
  },
});

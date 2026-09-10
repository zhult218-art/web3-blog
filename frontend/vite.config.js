import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  cacheDir: 'node_modules/.vite',
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      },
      '/vr': {
        target: 'http://localhost:8900',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/vr\//, '/api/')
      },
      '/pyquant': {
        target: 'http://localhost:9006',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/pyquant/, '')
      },
      '/whisper': {
        target: 'http://127.0.0.1:9011',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/whisper/, '')
      }
    }
  },
  build: {
    // Don't use manualChunks — it breaks dev-mode async imports of three.js
  }
})

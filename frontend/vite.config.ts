import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    hmr: false,
    port: 3000,
    allowedHosts: true,
    proxy: {
      '/api/v1/ws': {
        target: "http://localhost:8080/",
        ws: true
      },
      '/api/v1': {
        target: "http://localhost:8080/",
        changeOrigin: true
      }
    }
  }
})

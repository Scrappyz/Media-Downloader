import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react-swc'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    server: {
      host: "0.0.0.0",
      port: (env.NODE_ENV === 'development') ? 5001 : 5000,
      allowedHosts: [
        '.micoapp.org', // Allows example.com and all its subdomains
        'localhost',
      ]
    }
  }
})

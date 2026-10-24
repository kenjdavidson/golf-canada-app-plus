import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

const codegenShim = fileURLToPath(new URL('./src/react-native-codegenNativeComponent.ts', import.meta.url))

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'robots.txt', 'apple-touch-icon.png'],
      manifest: {
        name: 'Golf Canada App+',
        short_name: 'GolfCanada+',
        description: 'Golf Canada App mini-app with enhanced game modes and data tracking',
        theme_color: '#1a6b3c',
        background_color: '#ffffff',
        display: 'standalone',
        icons: [
          {
            src: 'pwa-192x192.png',
            sizes: '192x192',
            type: 'image/png'
          },
          {
            src: 'pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png'
          }
        ]
      }
    })
  ],
  resolve: {
    alias: [
      {
        find: 'react-native/Libraries/Utilities/codegenNativeComponent',
        replacement: codegenShim,
      },
      {
        find: 'react-native',
        replacement: 'react-native-web',
      },
    ],
  },
  build: {
    outDir: 'dist'
  }
})

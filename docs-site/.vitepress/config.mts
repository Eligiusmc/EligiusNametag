import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "EligiusNametag",
  description: "Documentación oficial del plugin de nametags holográficos.",
  themeConfig: {
    nav: [
      { text: 'Inicio', link: '/' },
      { text: 'Documentación', link: '/installation' }
    ],

    sidebar: [
      {
        text: 'Primeros Pasos',
        items: [
          { text: 'Instalación', link: '/installation' },
          { text: 'Comandos y Permisos', link: '/commands' }
        ]
      },
      {
        text: 'Configuración Modular',
        items: [
          { text: 'Configuración Global (config.yml)', link: '/config/global' },
          { text: 'Diseños de Jugadores (players.yml)', link: '/config/players' },
          { text: 'Mascotas Holográficas (pets.yml)', link: '/config/pets' }
        ]
      },
      {
        text: 'Soporte',
        items: [
          { text: 'Troubleshooting & Errores', link: '/troubleshooting' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/Eligiusmc/EligiusNametag' }
    ],

    footer: {
      message: 'Desarrollado bajo licencia MIT.',
      copyright: 'Copyright © 2026 Eligius MC'
    }
  }
})

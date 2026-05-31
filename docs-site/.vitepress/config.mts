import { defineConfig } from 'vitepress'

export default defineConfig({
  base: '/EligiusNametag/',
  title: "EligiusNametag",
  srcDir: 'src',
  sitemap: {
    hostname: 'https://eligiusmc.github.io/EligiusNametag/'
  },
  transformHead({ siteConfig, pageData }) {
    const head = []
    const locales = Object.keys(siteConfig.site.locales)
    
    // Add hreflang for all locales
    for (const locale of locales) {
      if (locale === 'root') continue
      head.push(['link', { rel: 'alternate', hreflang: locale, href: `https://eligiusmc.github.io/EligiusNametag/${locale}/${pageData.relativePath.replace('index.md', '').replace('.md', '')}` }])
    }
    // Add x-default
    head.push(['link', { rel: 'alternate', hreflang: 'x-default', href: `https://eligiusmc.github.io/EligiusNametag/${pageData.relativePath.replace('index.md', '').replace('.md', '')}` }])
    
    return head
  },
  head: [
    ['link', { rel: 'icon', href: '/EligiusNametag/assets/angry.png' }],
    ['script', {}, `
      (function() {
        if (typeof window === 'undefined') return;
        if (localStorage.getItem('lang-redirect-done')) return;
        var lang = navigator.language || navigator.userLanguage;
        var prefix = lang.split('-')[0];
        var supported = ['es', 'fr', 'de', 'pt', 'ru'];
        var currentPath = window.location.pathname;
        if (supported.includes(prefix) && (currentPath === '/EligiusNametag/' || currentPath === '/EligiusNametag/index.html')) {
          localStorage.setItem('lang-redirect-done', 'true');
          window.location.href = '/EligiusNametag/' + prefix + '/';
        }
      })();
    `]
  ],
  locales: {
    root: {
      label: 'English',
      lang: 'en',
      description: "Official documentation for the holographic nametags plugin.",
      themeConfig: {
        nav: [
          { text: '🏠 Home', link: '/' },
          { text: '📖 Documentation', link: '/installation' }
        ],
        sidebar: [
          {
            text: '🚀 Getting Started',
            items: [
              { text: 'Installation', link: '/installation' },
              { text: 'Commands & Permissions', link: '/commands' }
            ]
          },
          {
            text: '⚙️ Modular Config',
            items: [
              { text: 'Global Config (config.yml)', link: '/config/global' },
              { text: 'Player Designs (players.yml)', link: '/config/players' },
              { text: 'Holographic Pets (pets.yml)', link: '/config/pets' }
            ]
          },
          {
            text: '🛡️ Support',
            items: [
              { text: 'Troubleshooting & Errors', link: '/troubleshooting' }
            ]
          }
        ]
      }
    },
    es: {
      label: 'Español',
      lang: 'es',
      description: "Documentación oficial del plugin de nametags holográficos.",
      themeConfig: {
        nav: [
          { text: '🏠 Inicio', link: '/es/' },
          { text: '📖 Documentación', link: '/es/installation' }
        ],
        sidebar: [
          {
            text: '🚀 Primeros Pasos',
            items: [
              { text: 'Instalación', link: '/es/installation' },
              { text: 'Comandos y Permisos', link: '/es/commands' }
            ]
          },
          {
            text: '⚙️ Configuración Modular',
            items: [
              { text: 'Global (config.yml)', link: '/es/config/global' },
              { text: 'Jugadores (players.yml)', link: '/es/config/players' },
              { text: 'Mascotas (pets.yml)', link: '/es/config/pets' }
            ]
          },
          {
            text: '🛡️ Soporte',
            items: [
              { text: 'Troubleshooting & Errores', link: '/es/troubleshooting' }
            ]
          }
        ]
      }
    },
    fr: {
      label: 'Français',
      lang: 'fr',
      themeConfig: {
        nav: [
          { text: '🏠 Accueil', link: '/fr/' },
          { text: '📖 Documentation', link: '/fr/installation' }
        ],
        sidebar: [
          { text: '🚀 Démarrage', items: [{ text: 'Installation', link: '/fr/installation' }, { text: 'Commandes', link: '/fr/commands' }] },
          { text: '⚙️ Configuration', items: [{ text: 'Global', link: '/fr/config/global' }, { text: 'Joueurs', link: '/fr/config/players' }, { text: 'Animaux', link: '/fr/config/pets' }] },
          { text: '🛡️ Support', items: [{ text: 'Troubleshooting', link: '/fr/troubleshooting' }] }
        ]
      }
    },
    de: {
      label: 'Deutsch',
      lang: 'de',
      themeConfig: {
        nav: [
          { text: '🏠 Startseite', link: '/de/' },
          { text: '📖 Dokumentation', link: '/de/installation' }
        ],
        sidebar: [
          { text: '🚀 Erste Schritte', items: [{ text: 'Installation', link: '/de/installation' }, { text: 'Befehle', link: '/de/commands' }] },
          { text: '⚙️ Konfiguration', items: [{ text: 'Global', link: '/de/config/global' }, { text: 'Spieler', link: '/de/config/players' }, { text: 'Haustiere', link: '/de/config/pets' }] },
          { text: '🛡️ Support', items: [{ text: 'Fehlerbehebung', link: '/de/troubleshooting' }] }
        ]
      }
    },
    pt: {
      label: 'Português',
      lang: 'pt',
      themeConfig: {
        nav: [
          { text: '🏠 Início', link: '/pt/' },
          { text: '📖 Documentação', link: '/pt/installation' }
        ],
        sidebar: [
          { text: '🚀 Primeiros Passos', items: [{ text: 'Instalação', link: '/pt/installation' }, { text: 'Comandos', link: '/pt/commands' }] },
          { text: '⚙️ Configuração', items: [{ text: 'Global', link: '/pt/config/global' }, { text: 'Jogadores', link: '/pt/config/players' }, { text: 'Pets', link: '/pt/config/pets' }] },
          { text: '🛡️ Suporte', items: [{ text: 'Troubleshooting', link: '/pt/troubleshooting' }] }
        ]
      }
    },
    ru: {
      label: 'Русский',
      lang: 'ru',
      themeConfig: {
        nav: [
          { text: '🏠 Главная', link: '/ru/' },
          { text: '📖 Документация', link: '/ru/installation' }
        ],
        sidebar: [
          { text: '🚀 Начало работы', items: [{ text: 'Установка', link: '/ru/installation' }, { text: 'Команды', link: '/ru/commands' }] },
          { text: '⚙️ Конфигурация', items: [{ text: 'Глобальная', link: '/ru/config/global' }, { text: 'Игроки', link: '/ru/config/players' }, { text: 'Питомцы', link: '/ru/config/pets' }] },
          { text: '🛡️ Поддержка', items: [{ text: 'Устранение неполадок', link: '/ru/troubleshooting' }] }
        ]
      }
    }
  },
  themeConfig: {
    socialLinks: [
      { icon: 'github', link: 'https://github.com/Eligiusmc/EligiusNametag' }
    ],
    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2026 Eligius MC'
    }
  }
})

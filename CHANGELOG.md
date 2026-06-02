# Changelog

## [1.5.0](https://github.com/Eligiusmc/EligiusNametag/compare/v1.4.0...v1.5.0) (2026-06-02)


### Features

* implement native redis pub/sub and fix ghost nametags ([#26](https://github.com/Eligiusmc/EligiusNametag/issues/26), [#28](https://github.com/Eligiusmc/EligiusNametag/issues/28)) ([04e6bef](https://github.com/Eligiusmc/EligiusNametag/commit/04e6befe8f04ba48525176acc4330feb199ea525))

## [1.4.0](https://github.com/Eligiusmc/EligiusNametag/compare/v1.3.1...v1.4.0) (2026-06-02)


### Features

* bStats integration and telemetry ([73f0dd1](https://github.com/Eligiusmc/EligiusNametag/commit/73f0dd156d3d5f7655ebce3027421ede680b0026))

## [1.3.1](https://github.com/Eligiusmc/EligiusNametag/compare/v1.3.0...v1.3.1) (2026-05-31)
## [1.3.1] - 2026-06-02

### Added
- **bStats Integration:** We now collect anonymous usage metrics (Server platform, Database type, etc.) to help guide future development. You can opt out via your global `bStats/config.yml`.

### Bug Fixes

* resolve TextDisplay memory leaks, fly-in interpolation bugs and update UTF-8 lang ([307b42a](https://github.com/Eligiusmc/EligiusNametag/commit/307b42a1fdf4549b5966ca0f5eaf09b1b98609e2))

## [1.3.0] - 2026-05-30


### Features

* **core:** migrate to native Bukkit TextDisplays and drop ProtocolLib ([526a1a9](https://github.com/Eligiusmc/EligiusNametag/commit/526a1a9aa482ef4e7b6f14654b3e570007100a8c))

## [1.2.0](https://github.com/Eligiusmc/EligiusNametag/compare/v1.1.0...v1.2.0) (2026-05-31)


### Features

* **wiki & ci:** i18n support, animated dark theme, and automated releases enhancement ([#11](https://github.com/Eligiusmc/EligiusNametag/issues/11)) ([2bae140](https://github.com/Eligiusmc/EligiusNametag/commit/2bae140cc64e5936d33a8778c59a677d2345af69))

## [1.1.0](https://github.com/Eligiusmc/EligiusNametag/compare/v1.0.0...v1.1.0) (2026-05-31)


### Features

* implement release please automation and update checker ([#8](https://github.com/Eligiusmc/EligiusNametag/issues/8)) ([#9](https://github.com/Eligiusmc/EligiusNametag/issues/9)) ([2c07851](https://github.com/Eligiusmc/EligiusNametag/commit/2c0785136b85f267c1d01cfb44402845b8b49af6))


### Bug Fixes

* enforce github pages deployment and set correct vitepress base ([f8d9e4b](https://github.com/Eligiusmc/EligiusNametag/commit/f8d9e4b746e2f62dae09a9479d233b0785f4c18d))
* resolve release-please unsupported extraFile type regex ([573c30c](https://github.com/Eligiusmc/EligiusNametag/commit/573c30cdf0cd97b48b80cef8e90bff077855e6bf))

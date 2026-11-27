# WuKongClient

![License](https://img.shields.io/badge/License-MIT-green)
![Developer](https://img.shields.io/badge/Developer-WKImods-orange)
![Version](https://img.shields.io/badge/Version-6.4.3--alpha.1-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-26.3-green)
![Modloader](https://img.shields.io/badge/Modloader-Fabric-beige)
![Java](https://img.shields.io/badge/Java-25-orange)
![Status](https://img.shields.io/badge/Status-Alpha-orange)

### Your World. Your Rules. Your Client.

WuKongClient is a free, open-source, modular Minecraft Fabric client developed by WKImods.

It combines visual customization, inventory management, survival utilities, configurable overlays, game diagnostics and individual world and server profiles.

**Current development version:** `6.4.3-alpha.1`

## Features

| Category | Features |
|---|---|
| Visuals | Fullbright, Dynamic Zoom |
| Navigation | Waypoints, death markers and configurable overlays |
| Survival | AutoTool, Tool Saver, Auto Refill, Auto Eat |
| Building | Configurable mining utilities |
| Smart Inventory | Inventory Sorter, slot protection, stack merging, Shulker and Bundle previews, Auto Fish |
| Intelligent Tooltips | Enchantment Info+, Trade Calculator |
| HUD Studio | Armor HUD, Potion HUD, FPS, ping and resource tracking |
| Combat | Configurable in-game aiming assistance, bow motion model and camera options |
| Research Lab | Inventory diagnostics, consistency checks and synthetic regression tests |
| Profiles | World and server profiles, reusable configurations |
| Client Evolution | Module search, favorites, themes, Quick Menu, update checker and notifications |
| Localization | German and English interface support |

## Research Lab

A client-side diagnostic environment for observing inventory behavior and evaluating synthetic test scenarios.

- Inventory snapshots and slot history
- Item component comparison
- Container observations
- Latency history
- Local consistency findings
- Historical Minecraft bug references
- Seed-based synthetic test scenarios
- Persistent report archive
- CSV and JSON exports
- Report comparison and duplicate detection

Research Lab observations do not establish the existence of server-side duplication vulnerabilities.

## Inventory Management

Inventory sorting supports singleplayer and multiplayer using the appropriate implementation for each environment.

Available sorting strategies include item ID, name, quantity and mod namespace. Individual main inventory slots can be protected. Hotbar, armor and offhand slots are excluded from sorting.

## Smart Auto Feeder

- Standard mode: regular hotbar-based eating.
- Background mode: integrated-server singleplayer functionality.

Background mode is not available on external multiplayer servers.

## Requirements

| Component | Version |
|---|---|
| Minecraft Java Edition | 26.3 |
| Mod Loader | Fabric |
| Fabric Loader | 0.20.1+ |
| Fabric API | 0.166.2+26.3 |
| Java | 25 |

## Installation

1. Install Fabric for Minecraft 26.3.
2. Download the compatible Fabric API.
3. Download the regular WuKongClient JAR from Releases.
4. Remove older WuKongClient JARs from the mods directory.
5. Install the new JAR and launch Minecraft.

Do not install the sources JAR.

[Releases](https://github.com/WKImods/WuKongClient/releases)

[Fabric API](https://modrinth.com/mod/fabric-api)

[Fabric Installer](https://fabricmc.net/use/installer/)

## Building from source

Java Development Kit 25 is required.

Windows:

```powershell
.\gradlew.bat clean build --console=plain
```

Linux/macOS:

```bash
chmod +x gradlew
./gradlew clean build --console=plain
```

Build outputs are located in `build/libs/`.

## Development Status

WuKongClient is currently in alpha development.

Version 6.4.3-alpha.1 has been successfully compiled, and the project owner has reported positive in-game results. Full regression coverage and compatibility with every mod or server configuration have not been established.

Use optional modules in environments where their functionality is permitted.

See [CHANGELOG.md](CHANGELOG.md) and [ROADMAP.md](ROADMAP.md).

## Community & Contributions

[Report bugs or request features](https://github.com/WKImods/WuKongClient/issues).

Please include the client version, environment, relevant logs and reproduction steps.

## License

MIT License - Copyright (c) 2026 WKImods.

See [LICENSE](LICENSE).

**WKImods - Independent Mod Development & Software Engineering**

WuKongClient is an independent project and is not affiliated with Mojang Studios or Microsoft.

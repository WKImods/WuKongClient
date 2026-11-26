# WuKongClient

![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)
![Developer](https://img.shields.io/badge/Developer-WKImods-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-26.3-green)
![Modloader](https://img.shields.io/badge/Modloader-Fabric-beige)
![Java](https://img.shields.io/badge/Java-25-orange)
![Status](https://img.shields.io/badge/Status-Beta-yellow)

### Your World. Your Rules. Your Client.

WuKongClient is a free, open-source, modular Minecraft Fabric client developed by WKImods. It focuses on quality-of-life utilities, visual customization, survival assistance, inventory management and personalized world and server configurations.

**Current development version:** `6.0.1-beta.1` — Smart Auto Feeder.

## Features

| Category | Features |
|---|---|
| Visuals | Fullbright 4.4, Dynamic Zoom with adjustable magnification |
| Survival | Auto Tool, Tool Saver, Smart Auto Refill, Auto Eat |
| Smart Inventory | Shulker and Bundle previews, Inventory Quick Sort, Auto Fish |
| Intelligent Tooltips | Enchantment Info+, Trade Calculator |
| Navigation | Custom waypoints, waypoint editor, death markers |
| HUD Studio | Armor, potions, FPS and ping, resource tracker, adjustable position, scale and opacity |
| Profiles | World/server profiles, reusable templates, JSON import/export, persistent settings |
| Client Evolution | Hierarchical module interface, category master switches, search, favorites, themes, Quick Menu, update checker and notifications |

### Smart Auto Feeder

Choose the mode in **Survival > Auto Eat > Config > Mode**:

- **Standard (Hotbar):** existing automatic eating behavior; suitable for permitted multiplayer environments.
- **Background (Singleplayer):** opt-in integrated-server consumption from inventory without changing the selected hotbar slot or starting the hand-use animation. The normal food-use duration, hunger, saturation and consumption effects are retained by design.

Background mode is inactive on external servers. There is no automatic mode fallback. Standard remains the default. Use optional modules only in environments where their functionality is permitted.

## Requirements

| Component | Version |
|---|---|
| Minecraft | 26.3 |
| Mod Loader | Fabric |
| Fabric Loader | 0.20.1+ |
| Fabric API | 0.166.2+26.3 |
| Java | 25 |

## Installation

1. Install Fabric for Minecraft 26.3.
2. Download the compatible Fabric API and the regular WuKongClient JAR from Releases.
3. Remove older WuKongClient JARs from your `mods` directory, then place the new JAR and Fabric API there.
4. Launch Minecraft using the Fabric profile.
5. Configure modules individually. Do not install a `-sources.jar` in the mods directory.

[Releases](https://github.com/WKImods/WuKongClient/releases) · [Fabric API](https://modrinth.com/mod/fabric-api) · [Fabric Installer](https://fabricmc.net/use/installer/)

## Building from source

Java Development Kit 25 and an internet connection for dependency resolution are required.

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

## Project status

Active beta development. The owner has reported successful in-game testing of the new Background Auto Feeder. Additional regression and clean-install verification remains open; this is not a stable-release claim. Optional modules are intended to start disabled on a fresh installation.

See [CHANGELOG.md](CHANGELOG.md) and [ROADMAP.md](ROADMAP.md) for development history and outstanding checks.

## Community & contributions

[Report a bug or request a feature](https://github.com/WKImods/WuKongClient/issues). Please include the client version, environment and steps to reproduce any issue.

## License

MIT License — Copyright (c) 2026 WKImods. See [LICENSE](LICENSE).

**WKImods** — Independent Mod Development & Software Engineering.

WuKongClient is an independent project and is not affiliated with Mojang Studios or Microsoft.

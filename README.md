# WuKongClient

![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)
![Developer](https://img.shields.io/badge/Developer-WKImods-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-26.3-green)
![Modloader](https://img.shields.io/badge/Modloader-Fabric-beige)
![Java](https://img.shields.io/badge/Java-25-orange)
![Status](https://img.shields.io/badge/Status-Beta-yellow)

### Your World. Your Rules. Your Client.

WuKongClient is an open-source, modular Minecraft client developed by WKImods and built on the Fabric modding framework.

The project focuses on customization, quality-of-life improvements, visual enhancements, survival utilities and personalized gameplay configurations.

## Core Features

### Visual Enhancements

- Fullbright 4.4
- Customizable visual settings
- Integrated rendering options

### Survival & Utilities

- Auto Tool
- Tool Saver
- Auto Refill
- Auto Eat
- Inventory HUD

### Navigation

- Custom waypoints
- Integrated waypoint editor
- Automatic death markers
- Individual world and server waypoints

### Profile Manager+ (5.6)

Currently undergoing testing.

- Automatic server and world profile switching
- Individual module configurations
- Custom profile templates
- Profile creation, duplication and renaming
- Profile deletion with confirmation
- JSON profile export
- Partial JSON profile import
- Conservative default settings for new multiplayer environments
- Automatic configuration persistence

### Customization

- Integrated configuration interface
- Customizable keybindings
- Individual module settings
- English and German localization
- Integrated help interface

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
2. Download the compatible Fabric API.
3. Download WuKongClient from the GitHub Releases section.
4. Place both JAR files inside your `.minecraft/mods` directory.
5. Launch Minecraft using your Fabric profile.

## Downloads

- [WuKongClient Releases](https://github.com/WKImods/WuKongClient/releases)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Fabric Installer](https://fabricmc.net/use/installer/)

## Development Roadmap

- Extended Entity Filters
- Damage Numbers
- Configurable Zoom
- Shulker Box Preview
- Potion Effect HUD
- Inventory Sorter
- Auto Fish
- Enchantment Information & Translation Overlay
- Extended Profile Management
- Server Compatibility Improvements
- Performance Optimization
- Additional Quality-of-Life Features

## Building from Source

Requirements:

- Java Development Kit 25
- Git
- Internet connection for dependency resolution

Windows:

```powershell
.\gradlew.bat clean build
```

Linux / macOS:

```bash
chmod +x gradlew
./gradlew clean build
```

Compiled JAR files are located in `build/libs/`.

## Development Status

WuKongClient is currently in active beta development.

Experimental features may contain bugs or compatibility issues. Features listed in the development roadmap are not necessarily available in current releases.

Use modules only in environments where they are permitted.

## Community & Contributions

Contributions, bug reports, feature requests and constructive feedback are welcome.

- [Report a Bug](https://github.com/WKImods/WuKongClient/issues)
- [View Source Code](https://github.com/WKImods/WuKongClient)
- [View Releases](https://github.com/WKImods/WuKongClient/releases)

## License

WuKongClient is licensed under the MIT License.

Copyright (c) 2026 WKImods.

## Developer

**WKImods**

Independent Mod Development & Software Engineering.

**Built with passion. Driven by curiosity.**

WuKongClient is an independent project and is not affiliated with Mojang Studios or Microsoft.

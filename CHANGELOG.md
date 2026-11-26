# Changelog

Notable changes to WuKongClient are recorded here. Internal development milestones are not necessarily separate public releases.

## 6.0.1-beta.1 — Smart Auto Feeder

### Added
- Opt-in integrated-server Background mode for Auto Eat in singleplayer.
- Localized Standard/Background mode selection and Standard-mode reset.
- Background inventory consumption without changing the selected hotbar slot or starting the hand-use animation.
- Inventory-state, lifecycle, manual-use and cooldown checks for background consumption.

### Preserved
- Standard Auto Eat mode, enabled state and hunger threshold.
- Normal food-use duration and consumption behavior by design.
- Existing Fullbright, HUD, Inventory Sorter and Auto Refill implementations.
- No automatic fallback from Background to Standard on remote servers.

### Verification
- Background Auto Feeder reported working in-game by the project owner.
- Full regression testing, fresh-install verification and individual food edge cases remain open.

## 6.0.0-beta.1 — Client Evolution

- Hierarchical module interface with category master switches and individual configuration screens.
- Module search, favorites, four theme presets and Quick Menu.
- GitHub release update checker and session-based Notification Center.
- Configuration recovery and backup protection.
- Dynamic Zoom (hold C, adjust with mouse wheel).
- Fixed HUD dragging, HUD scaling and German special characters.
- Refined profile selection and persistent settings.
- Owner confirmed successful compilation and 15/21 initial in-game checks; subsequently confirmed Quick Menu and favorite persistence, and located theme switching and the update checker.

## 5.9 — HUD Studio (including 5.9.1 and 5.9.2)

- Armor HUD, Potion HUD, FPS and ping, Resource Tracker and HUD Editor.
- Individual positions, scales and opacity controls.
- Smart Auto Refill improvements for manual inventory interactions.
- Searchable item registry and improved profile selection.
- Dedicated HUD configuration controls; final movement and scaling fixes confirmed in 6.0.

## 5.8 — Intelligent Tooltips (including 5.8.1)

- Enchantment Info+ with localized explanations and level information.
- Trade Calculator with trade prices, adjustments, uses and comparisons.
- Integrated-server inventory sorting fix: no empty hotbar slot requirement and persistent sorting.

## 5.7 — Smart Inventory (including 5.7.1)

- Shulker and Bundle previews.
- Singleplayer Inventory Sorter and Auto Fish.
- Inventory Quick Sort button, cursor protection and rapid-click protection.

## 5.6 — Profile Manager+

- World/server profiles, reusable templates, creation, duplication, renaming and deletion.
- JSON import/export and configuration persistence improvements.

## Earlier development baseline

- Fullbright 4.4, waypoints and core survival utilities.
- Gradle wrapper and GitHub Actions build infrastructure for Java 25.
- Previous internal build identifier: `2.0.0-fabric-dev8-fullbright44`.

No stable release is claimed by this changelog.

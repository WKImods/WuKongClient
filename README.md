# WuKong – Fabric Minecraft 26.3 (ESP Step 2)

Eigenständige, clientseitige Fabric-Mod. **Quellcode**: Java 25, Fabric Loader 0.20.1,
Fabric API 0.166.2+26.3, Fabric Loom 1.19-SNAPSHOT.

## Änderungen zum bisherigen ESP Step 1

- **Rebranding zu WuKong:** Mod-ID `wukong`, Anzeigename, GUI-Titel,
  Java-Package `dev.wukong.client`, Mod-Entrypoint und JAR `wukong-fabric-...jar`.
- **ESP Through Walls:** ON = gewohnte Boxen auch hinter Hindernissen;
  OFF = nur Entities/Container mit freier Block-Sichtlinie zu mindestens einem
  Hitbox-Punkt. Sichtbarkeit wird pro Spieltick aktualisiert (max. ca. 50 ms Verzögerung).
- **ESP-Killswitch F7:** ON/OFF, mit Flankenerkennung; keine Auslösung beim Tippen in Menüs.
  Bei OFF verschwinden **alle** ESP-Elemente einschließlich der alten HUD-Zielliste.
- **Aim Spot** unter Insert > Target Rules: Torso, Head, Neck, All Visible.
  Für Head/Neck/Torso werden geometrische Punkte in der Hitbox verwendet.
  All Visible wählt den ersten Punkt mit freier Block-Sichtlinie (inklusive Hitbox-Ränder,
  wenn z. B. nur eine Schulter hervorschaut). Das ist *keine* echte Erkennung
  von Gliedmaßen-/Knochenmodellen; Minecraft-Mobs nutzen gewöhnlich eine AABB-Hitbox.
- Die bestehende Bogen-Prediction und Separate-Camera-Interpolation bleiben erhalten.
- Kein F6-Freecam-Modul.

## Einstellungen übernehmen

WuKong benutzt `config/wukong.json`. Falls diese noch nicht existiert,
kopiert die Mod beim ersten Start die **alte** `config/lunyxbench.json` nach
`config/wukong.json` und liest sie ein. Die alte Datei bleibt erhalten.
Der alte Zustand von `ESP Through Walls` war ON; für ältere Configs wird ON übernommen.

## Installation & Build

1. **Nicht in den alten Quellordner mischen:** ZIP in einen neuen WuKong-Ordner entpacken.
2. PowerShell dort öffnen: `./gradlew.bat clean build --console=plain`
3. Die normale JAR aus `build/libs` in `%appdata%\.minecraft\mods` kopieren.
4. **Alle alten `lunyxbench-*.jar` aus dem Mods-Ordner entfernen**, bevor WuKong gestartet wird.
5. Fabric API muss weiterhin installiert sein. Nicht die `-sources.jar` verwenden.

Ein erfolgreicher Compilerlauf ist noch kein Ingame-Test. Der vollständige Minecraft-26.3-Build
kann in dieser Umgebung ohne JDK25/Fabric-Gradle-Abhängigkeiten nicht ausgeführt werden.

## Grenzen / Hinweise

- Die Boxen sind **Screen-Space-Projektionen** von 3D-Hitboxen, keine native 3D-Render-Pipeline.
- Block-Sichtprüfung arbeitet mit Vanilla-Block-Raycasts und kann bei transparenten,
  nicht vollständig kollidierenden Blöcken (z. B. Glas/Zaun) anders ausfallen als
  visuelle Sichtbarkeit. `ClipContext.Block.COLLIDER` ignoriert reine Flüssigkeiten.
- Die Anzeige nutzt ausschließlich vom Client geladene Chunks/Entitäten.
- Nutzung im Multiplayer unterliegt den Regeln des jeweiligen Servers.

# Port Blocklings 1.16 → 1.21.1 multiloader

## Fait (cette session)

- [x] Structure **multiloader** (`common` / `neoforge` / `fabric`) basée sur [MultiLoader-Template 1.21.1](https://github.com/jaredlll08/MultiLoader-Template/tree/1.21.1)
- [x] `BlocklingPacketGuard` + `BlocklingInventoryGuard` (serveur autoritaire, rate limit, distance, propriété)
- [x] Détection **Mohist / Youer / Arclight** (`BukkitDetector`, `HybridServerDetector`)
- [x] `ModCompatRegistry` + doc CurseForge relations
- [x] NeoForge payloads (`NeoForgeNetworkBridge`) — base pour remplacer l'ancien `SimpleChannel`
- [x] `pack.mcmeta` 1.21.1, `neoforge.mods.toml`

## Reste à faire (port gameplay ~247 fichiers)

Le code gameplay dans `common/` utilise encore les APIs **Forge 1.16** (`SimpleChannel`, `TameableEntity`, `RegistryObject`, etc.).

1. **Entité** — porter `BlocklingEntity` → `TamableAnimal` 1.21.1
2. **Réseau** — migrer les 22 messages vers `CustomPacketPayload` + garde serveur sur chaque handler
3. **Registrations** — `DeferredRegister` NeoForge 1.21.1 (items, entités, sons, creative tab)
4. **Capabilities** → **Data Attachments** NeoForge
5. **Spawn** — `BiomeLoadingEvent` → datapack / NeoForge biome modifiers
6. **Client** — GUI (`MatrixStack` → `GuiGraphics`), renderer entity
7. **Assets** — textures/sons manquants (Git LFS ?)
8. **Fabric** — brancher payloads Fabric quand le common compile

## Commandes

```bash
./gradlew :neoforge:runServer
./gradlew :neoforge:runClient
./gradlew :fabric:runServer
```

## Référence source

- Original : [WillR27/Blocklings 1.18](https://github.com/WillR27/Blocklings/tree/1.18/src/main/java/com/willr27/blocklings)
- Branche NeoForge la plus proche dans ce repo : `origin/1.21.1-Fabric` (bootstrap complet, APIs encore Forge 1.16)

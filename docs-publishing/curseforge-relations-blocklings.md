# Relations CurseForge — Blocklings Revival

Configurer sur CurseForge → **Relations** → **Optional dependency** avec les Project IDs ci-dessous.

| Mod | Project ID | URL |
|-----|------------|-----|
| LuckPerms | **431733** | [luckperms](https://www.curseforge.com/minecraft/mc-mods/luckperms) |
| spark | **495151** | [spark](https://www.curseforge.com/minecraft/mc-mods/spark) |

## Serveurs hybrides (description)

Blocklings Revival fonctionne sur serveurs dédiés NeoForge et sur **Mohist**, **Youer**, **Arclight** (NeoForge + plugins Bukkit). Aucune relation CurseForge pour les cores hybrides — mentionner dans la description du projet.

## Anti-duplication

Toutes les actions sensibles (inventaire blockling, skills, tasks) passent par `BlocklingPacketGuard` côté serveur. Les paquets client ne modifient jamais directement l'inventaire.

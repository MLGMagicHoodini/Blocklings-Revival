# -*- coding: utf-8 -*-
import json
from pathlib import Path

common = Path(r"c:\Mes mods crée\mm\Blocklings-Revival\common\src\main\resources\assets\blocklings\lang")
neo = Path(r"c:\Mes mods crée\mm\Blocklings-Revival\neoforge\src\main\resources\assets\blocklings\lang")

en = json.loads((common / "en_us.json").read_text(encoding="utf-8"))
fr = json.loads((common / "fr_fr.json").read_text(encoding="utf-8"))

additions = {
    "blocklings.config.search.too_many_results": "Trop de résultats...",
    "blocklings.config.items": "Objets",
    "blocklings.config.item.add": "Ajouter un objet",
    "blocklings.config.item.remove": "Retirer l'objet (%s)",
    "blocklings.config.item.amount": "Objets : %d/%d",
    "blocklings.config.item.start_at": "Commencer à",
    "blocklings.config.item.stop_at": "Arrêter à",
    "blocklings.config.item.inventory_start_amount.name": "Seuil de début d'inventaire",
    "blocklings.config.item.inventory_start_amount.desc": "Le nombre d'objets dans l'inventaire de votre blockling à partir duquel il commence à transférer. Laisser vide pour ignorer.",
    "blocklings.config.item.inventory_stop_amount.name": "Seuil d'arrêt d'inventaire",
    "blocklings.config.item.inventory_stop_amount.desc": "Le nombre d'objets dans l'inventaire de votre blockling à partir duquel il arrête de transférer. Laisser vide pour ignorer.",
    "blocklings.config.item.container_start_amount.name": "Seuil de début du conteneur",
    "blocklings.config.item.container_start_amount.desc": "Le nombre d'objets dans le conteneur cible à partir duquel votre blockling commence à transférer. Laisser vide pour ignorer.",
    "blocklings.config.item.container_stop_amount.name": "Seuil d'arrêt du conteneur",
    "blocklings.config.item.container_stop_amount.desc": "Le nombre d'objets dans le conteneur cible à partir duquel votre blockling arrête de transférer. Laisser vide pour ignorer.",
    "blocklings.config.container.blank": "Vide",
    "blocklings.config.containers": "Conteneurs",
    "blocklings.config.container.add": "Ajouter un conteneur",
    "blocklings.config.container.remove": "Retirer le conteneur (%s)",
    "blocklings.config.container.add.help": "Cliquez sur plus, puis clic droit sur le conteneur à ajouter. Clic gauche sur un bloc pour annuler. Ou maintenez %s pour ajouter manuellement.",
    "blocklings.config.container.amount": "Conteneurs : %d/%d",
    "blocklings.config.container.side_priority.name": "Priorité des côtés",
    "blocklings.config.container.side_priority.side": " (%s)",
    "blocklings.config.container.side_priority.desc": "L'ordre dans lequel votre blockling tente d'insérer les objets dans le conteneur (souvent des emplacements différents). Les plus petits nombres sont prioritaires.",
    "blocklings.direction.front": "Avant",
    "blocklings.direction.back": "Arrière",
    "blocklings.direction.left": "Gauche",
    "blocklings.direction.right": "Droite",
    "blocklings.direction.top": "Haut",
    "blocklings.direction.bottom": "Bas",
    "blocklings.skill.buy.yes": "Oui",
    "blocklings.skill.buy.no": "Non",
    "blocklings.skill.general.courier.name": "Courrier",
    "blocklings.skill.general.courier.desc": "Débloque les tâches \"Déposer des objets\" et \"Prendre des objets\".",
    "blocklings.skill.general.advanced_courier.name": "Courrier avancé",
    "blocklings.skill.general.advanced_courier.desc": "Débloque la configuration avancée des objets pour les tâches \"Déposer des objets\" et \"Prendre des objets\".",
    "blocklings.task.deposit_items.name": "Déposer des objets",
    "blocklings.task.deposit_items.desc": "Votre blockling déposera des objets de son inventaire dans les conteneurs proches.",
    "blocklings.task.take_items.name": "Prendre des objets",
    "blocklings.task.take_items.desc": "Votre blockling prendra des objets dans les conteneurs proches et les mettra dans son inventaire.",
    "blocklings.task.property.item_configuration_type.name": "Type de configuration des objets",
    "blocklings.task.property.item_configuration_type.desc": "Le type de configuration des objets à utiliser.",
    "blocklings.task.property.item_configuration_type.simple": "Simple",
    "blocklings.task.property.item_configuration_type.advanced": "Avancé",
    "blocklings.task.ui.task_amount": "Tâches : %d/%d",
}

new_fr = {}
for key, value in en.items():
    if key in fr:
        new_fr[key] = fr[key]
    elif key in additions:
        new_fr[key] = additions[key]
    else:
        new_fr[key] = value

assert set(new_fr) == set(en), sorted(set(en) - set(new_fr))
text = json.dumps(new_fr, ensure_ascii=False, indent=2) + "\n"
(common / "fr_fr.json").write_text(text, encoding="utf-8", newline="\n")
(neo / "fr_fr.json").write_text(text, encoding="utf-8", newline="\n")
print("fr_fr updated", len(new_fr))

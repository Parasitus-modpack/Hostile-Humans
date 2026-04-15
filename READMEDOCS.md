 # Human Gear JSON System

  ## Overview

  The mod now supports datapack-driven human gear loadouts.

  Instead of hardcoding human armor and weapons in Java arrays, the mod reads JSON files from:

  data/hostile_humans/human_loadouts/

  Supported default files are:

  data/hostile_humans/human_loadouts/tier1.json
  data/hostile_humans/human_loadouts/tier2.json
  data/hostile_humans/human_loadouts/roamer.json

  These files control what each human tier can spawn with.

  ## Why this exists

  This system makes it easier for players and pack makers to:

  - add support for weapons and armor from other mods
  - rebalance human loadouts without changing code
  - override default human gear with datapacks

  ## How loadouts are selected

  Each tier file can define:

  - mainhand
  - ranged_mainhand
  - offhand
  - inventory
  - bonus_mainhand
  - armor_sets
  - rules

  The mod uses weighted random rolls from these pools during human spawn.

  ## File Format

  ### Top-level structure

  {
    "schema_version": 1,
    "rules": {
      "enchant_chance": 0.3,
      "damage_percent_min": 0.0,
      "damage_percent_max": 0.85
    },
    "mainhand": [],
    "ranged_mainhand": [],
    "offhand": {
      "chance": 0.2,
      "entries": []
    },
    "inventory": [],
    "bonus_mainhand": {
      "chance": 0.0,
      "entries": []
    },
    "armor_sets": []
  }

  ## Field Reference

  ### schema_version

  "schema_version": 1

  Current version of the gear schema.

  ### rules

  "rules": {
    "enchant_chance": 0.3,
    "damage_percent_min": 0.0,
    "damage_percent_max": 0.85
  }

  Meaning:

  - enchant_chance: chance for spawned gear to receive enchantments
  - damage_percent_min: minimum durability damage percent applied to spawned gear
  - damage_percent_max: maximum durability damage percent applied to spawned gear

  ### mainhand

  Weighted pool for normal main-hand weapons.

  Example:

  "mainhand": [
    { "item": "minecraft:iron_sword", "weight": 2 },
    { "item": "minecraft:stone_sword", "weight": 2 },
    { "item": "minecraft:crossbow", "weight": 1 },
    { "item": "minecraft:bow", "weight": 1 }
  ]

  ### ranged_mainhand

  Used when a human is forced to spawn with a ranged weapon.

  Example:

  "ranged_mainhand": [
    { "item": "minecraft:crossbow", "weight": 1 },
    { "item": "minecraft:bow", "weight": 1 }
  ]

  ### offhand

  Controls optional offhand gear.

  Example:

  "offhand": {
    "chance": 0.5,
    "entries": [
      { "item": "minecraft:shield", "weight": 85 },
      { "item": "minecraft:totem_of_undying", "weight": 15 }
    ]
  }

  Meaning:

  - chance: chance to roll an offhand item at all
  - entries: weighted item pool if the roll succeeds

  ### inventory

  Extra inventory items, mainly used for backup weapons.

  Example:

  "inventory": [
    { "item": "minecraft:iron_sword", "weight": 1 },
    { "item": "minecraft:stone_sword", "weight": 1 }
  ]

  ### bonus_mainhand

  Optional rare override for the main hand after the normal roll.

  Example:

  "bonus_mainhand": {
    "chance": 0.05,
    "entries": [
      { "item": "minecraft:trident", "weight": 1 }
    ]
  }

  ### armor_sets

  Weighted pool of full armor sets.

  Example:

  "armor_sets": [
    {
      "weight": 1,
      "head": "minecraft:iron_helmet",
      "chest": "minecraft:iron_chestplate",
      "legs": "minecraft:iron_leggings",
      "feet": "minecraft:iron_boots"
    },
    {
      "weight": 1,
      "head": "minecraft:diamond_helmet",
      "chest": "minecraft:diamond_chestplate",
      "legs": "minecraft:diamond_leggings",
      "feet": "minecraft:diamond_boots"
    }
  ]

  ## Item Entry Format

  Basic item entry:

  {
    "item": "minecraft:diamond_sword",
    "weight": 3
  }

  Optional mod-gated entry:

  {
    "item": "some_mod:my_weapon",
    "weight": 2,
    "requires_mod": "some_mod"
  }

  Meaning:

  - item: item id
  - weight: weighted random chance
  - requires_mod: optional mod id, only used if that mod is loaded

  ## Example Tier 2 File

  {
    "schema_version": 1,
    "rules": {
      "enchant_chance": 1.0,
      "damage_percent_min": 0.0,
      "damage_percent_max": 0.85
    },
    "mainhand": [
      { "item": "minecraft:diamond_sword", "weight": 3 },
      { "item": "minecraft:diamond_axe", "weight": 2 },
      { "item": "minecraft:crossbow", "weight": 1 },
      { "item": "minecraft:bow", "weight": 1 }
    ],
    "ranged_mainhand": [
      { "item": "minecraft:crossbow", "weight": 1 },
      { "item": "minecraft:bow", "weight": 1 }
    ],
    "offhand": {
      "chance": 0.5,
      "entries": [
        { "item": "minecraft:shield", "weight": 85 },
        { "item": "minecraft:totem_of_undying", "weight": 15 }
      ]
    },
    "inventory": [
      { "item": "minecraft:iron_sword", "weight": 1 },
      { "item": "minecraft:stone_sword", "weight": 1 }
    ],
    "bonus_mainhand": {
      "chance": 0.05,
      "entries": [
        { "item": "minecraft:trident", "weight": 1 }
      ]
    },
    "armor_sets": [
      {
        "weight": 1,
        "head": "minecraft:iron_helmet",
        "chest": "minecraft:iron_chestplate",
        "legs": "minecraft:iron_leggings",
        "feet": "minecraft:iron_boots"
      },
      {
        "weight": 1,
        "head": "minecraft:diamond_helmet",
        "chest": "minecraft:diamond_chestplate",
        "legs": "minecraft:diamond_leggings",
        "feet": "minecraft:diamond_boots"
      }
    ]
  }

  ## Datapack Override Behavior

  If a datapack provides one of these files:

  - tier1.json
  - tier2.json
  - roamer.json

  then that datapack version is what the mod uses for that tier.

  This means players can replace the default tier loadout by shipping a datapack with the same file path.

  ## Current Limitations

  This first version supports:

  - exact item ids
  - weighted item pools
  - optional requires_mod
  - weighted armor sets
  - basic gear damage + enchant rules

  It does not currently support:

  - item tags
  - per-slot armor pools outside armor sets
  - merge/append behavior between datapacks
  - custom NBT/components in entries
  - advanced conditions beyond requires_mod

  ## Best Practice For Modpack Authors

  - use exact item ids for stable control
  - use requires_mod for optional compat entries
  - keep one loadout file per tier
  - if you want different styles, distribute different datapacks

  ## Summary

  This system exists so players and pack makers can fully control human spawn gear from JSON instead of code.

  The three tier files are the main extension points:

  - tier1.json
  - tier2.json
  - roamer.json

  If needed later, the schema can be expanded to support tags, custom components, and more advanced gear rules.

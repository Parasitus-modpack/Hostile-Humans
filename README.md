Add any armor/weapon to Humans

Do this by making a datapack, follow these easy steps:

1. Turn the mod jar file to a zip folder


2. Extract the zip so you can open it


3. Follow this folder path
data\hostile_humans\human_loadouts


4. Pick which type of human you want to edit, you will see stuff inside like

mainhand": [
    { "item": "minecraft:iron_sword", "weight": 2 },
    { "item": "minecraft:stone_sword", "weight": 2 }
]

If you want to add stuff just follow the format, for example I will now add a diamond sword

mainhand": [
    { "item": "minecraft:iron_sword", "weight": 2 },
    { "item": "minecraft:stone_sword", "weight": 2 },
 { "item": "minecraft:diamond_sword", "weight": 3 }
]


Make sure you use the id of the item your adding, igame you can do F3 + H to activate tooltips to see the id of an item by hovering over it.


5. Once done adding your edits, go back to data\hostile_humans

Copy the hostile_humans folder


6. Make sure you have a place to put your datapacks in your files, such as a data or datapack. For example the mod Kubejs will generate its own data folder you to use


7. Paste the hostile_humans folder in your datapack folder. Your files should now look the same, like
datapack\hostile_humans\human_loadouts

Now when the mod loads, it will instead load your file path instead of the original.
package com.craftix.hostile_humans.entity.entities;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.compat.ImmersiveArmors;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftix.hostile_humans.HumanUtil.isRangedWeapon;
import static com.craftix.hostile_humans.entity.entities.HumanTier.LEVEL1;
import static com.craftix.hostile_humans.entity.entities.HumanTier.LEVEL2;

public class HumanInventoryGenerator {

    public static int[] tier1Armor = new int[]{0, 2, 3, 1};
    public static int[] tier2Armor = new int[]{3, 4};
    public static Item[] tier1Weapons = new Item[]{Items.IRON_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.STONE_SWORD, Items.CROSSBOW, Items.BOW};
    public static Item[] tier2Weapons = new Item[]{Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_SWORD, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.CROSSBOW, Items.BOW};
    public static Item[] rangedWeapons = new Item[]{Items.CROSSBOW, Items.BOW};
    public static Item[] extraWeapons = new Item[]{Items.IRON_SWORD, Items.STONE_SWORD};

    public static void generateInventory(Human human, boolean forceRanged) {
        if (human.getData() == null) {
            HostileHumans.LOGGER.warn("Missing data during inventory generation" + " " + human);
            human.discard();
            return;
        }
        int[] armorPick = tier1Armor;
        Item[] weaponPick = tier1Weapons;
        float enchantChance = 0.3f;
        float armorChance = 0.25f;
        RandomSource random = human.getRandom();
        HumanTier humanTier = human.getTier();

        switch (humanTier) {
            case LEVEL2 -> {
                armorPick = tier2Armor;
                weaponPick = tier2Weapons;
                enchantChance = 1f;
                armorChance = 0.15f;
            }
            case ROAMER -> {
                enchantChance = 1f;
                armorChance = 0.2f;
                if (random.nextFloat() < 0.3f) {
                    armorPick = tier2Armor;
                }
                if (random.nextFloat() < 0.3f) {
                    weaponPick = tier2Weapons;
                }
            }
        }

        if (forceRanged) {
            weaponPick = rangedWeapons;
        }

        ItemStack stack = weaponPick[random.nextInt(weaponPick.length)].getDefaultInstance();
        damage(human, stack);
        human.setItemSlot(EquipmentSlot.MAINHAND, stack);

        if (random.nextFloat() < (humanTier == LEVEL2 ? 0.5f : ((humanTier == LEVEL1) ? 0.2f : 0.4f)) && !isRangedWeapon(human.getItemBySlot(EquipmentSlot.MAINHAND))) {

            if (random.nextFloat() < 0.15 && humanTier == LEVEL2) {
                human.setItemSlot(EquipmentSlot.OFFHAND, Items.TOTEM_OF_UNDYING.getDefaultInstance());
            } else {
                ItemStack shield = Items.SHIELD.getDefaultInstance();
                damage(human, shield);
                human.setItemSlot(EquipmentSlot.OFFHAND, shield);
            }
        }

        if (HumanUtil.isRangedWeapon(human.getItemBySlot(EquipmentSlot.MAINHAND))) {
            ItemStack extraWeapon = extraWeapons[random.nextInt(extraWeapons.length)].getDefaultInstance();
            damage(human, extraWeapon);
            extraWeapon.enchant(Enchantments.VANISHING_CURSE, 1);
            human.getData().setInventoryItem(0, extraWeapon);
        }

        int staticImmersivePick = -1;
        if (random.nextFloat() < 0.45 && ModList.get().isLoaded("immersive_armors")) {
            List<Integer> list = new ArrayList<>();
            for (int i : armorPick) {
                list.add(i);
            }
            list.addAll(Arrays.asList(5, 6));
            if (humanTier == LEVEL2) {
                list.add(7);
            }
            staticImmersivePick = list.get(random.nextInt(list.size()));

            //slight adjustments
            if ((staticImmersivePick == 3 || staticImmersivePick == 5) && random.nextFloat() < 0.25f) {
                staticImmersivePick = list.get(random.nextInt(list.size()));
            }

            if ((staticImmersivePick == 5) && random.nextFloat() < 0.05f) {
                staticImmersivePick = list.get(random.nextInt(list.size()));
            }
        }

        int pick = staticImmersivePick != -1 ? staticImmersivePick : armorPick[random.nextInt(armorPick.length)];

        int pickForChest = -1;

        var slots = EquipmentSlot.values();
        ArrayUtils.reverse(slots);// head to toes

        for (EquipmentSlot equipmentslot : slots) {
            if (equipmentslot.getType() == EquipmentSlot.Type.ARMOR) {

                if (humanTier != LEVEL2 && random.nextFloat() < armorChance && staticImmersivePick != -1) {
                    continue;
                }

                if (equipmentslot == EquipmentSlot.LEGS && pickForChest > pick) {
                    pick = pickForChest;
                }

                Item item = customGetEquipmentForSlot(equipmentslot, pick);
                if (staticImmersivePick != -1) {
                    item = ImmersiveArmors.getItemForSlot(equipmentslot, pick);
                }

                if (equipmentslot == EquipmentSlot.CHEST) {
                    pickForChest = pick;
                }

                if (item != null) {
                    human.setItemSlot(equipmentslot, damage(human, item.getDefaultInstance()));
                }
            }
        }

        //-Change how humans spawn with armor (Don't change immersive armor)
        //
        //1. Spawn with full set of armor
        //
        //2. Spawn with full set of armor but not helmet
        //
        //3. Spawn with full set of armor but random leggings

        if (staticImmersivePick == -1) {
            int randomValue = random.nextInt(3) + 1;
            switch (randomValue) {
                case 2 -> human.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                case 3 -> {
                    int pickOther = armorPick[random.nextInt(armorPick.length)];
                    Item item = customGetEquipmentForSlot(EquipmentSlot.LEGS, pickOther);
                    if (item != null)
                        human.setItemSlot(EquipmentSlot.LEGS, damage(human, item.getDefaultInstance()));
                }
            }
        }

        human.applySpawnedWeaponEnchantments(random, enchantChance);

        for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
            if (equipmentslot.getType() == EquipmentSlot.Type.ARMOR) {
                human.applySpawnedArmorEnchantments(random, enchantChance, equipmentslot);
            }
        }

        if (humanTier == LEVEL2 && random.nextFloat() < 0.05) {
            ItemStack tridentStack = Items.TRIDENT.getDefaultInstance();
            tridentStack.enchant(Enchantments.LOYALTY, 1);
            tridentStack.enchant(Enchantments.VANISHING_CURSE, 1);
            human.setItemSlot(EquipmentSlot.MAINHAND, tridentStack);
        }
    }

    public static ItemStack damage(Human human, ItemStack inStack) {
        int damage = (int) (human.getRandom().nextInt(inStack.getMaxDamage() - 1) * 1.35f);
        if (damage > inStack.getMaxDamage()) {
            damage = inStack.getMaxDamage() - 1;
        }
        inStack.setDamageValue(damage);
        return inStack;
    }

    public static Item customGetEquipmentForSlot(EquipmentSlot slot, int level) {
        switch (slot) {
            case HEAD:
                if (level == 0) {
                    return Items.LEATHER_HELMET;
                } else if (level == 1) {
                    return Items.GOLDEN_HELMET;
                } else if (level == 2) {
                    return Items.CHAINMAIL_HELMET;
                } else if (level == 3) {
                    return Items.IRON_HELMET;
                } else if (level == 4) {
                    return Items.DIAMOND_HELMET;
                }
            case CHEST:
                if (level == 0) {
                    return Items.LEATHER_CHESTPLATE;
                } else if (level == 1) {
                    return Items.GOLDEN_CHESTPLATE;
                } else if (level == 2) {
                    return Items.CHAINMAIL_CHESTPLATE;
                } else if (level == 3) {
                    return Items.IRON_CHESTPLATE;
                } else if (level == 4) {
                    return Items.DIAMOND_CHESTPLATE;
                }
            case LEGS:
                if (level == 0) {
                    return Items.LEATHER_LEGGINGS;
                } else if (level == 1) {
                    return Items.GOLDEN_LEGGINGS;
                } else if (level == 2) {
                    return Items.CHAINMAIL_LEGGINGS;
                } else if (level == 3) {
                    return Items.IRON_LEGGINGS;
                } else if (level == 4) {
                    return Items.DIAMOND_LEGGINGS;
                }
            case FEET:
                if (level == 0) {
                    return Items.LEATHER_BOOTS;
                } else if (level == 1) {
                    return Items.GOLDEN_BOOTS;
                } else if (level == 2) {
                    return Items.CHAINMAIL_BOOTS;
                } else if (level == 3) {
                    return Items.IRON_BOOTS;
                } else if (level == 4) {
                    return Items.DIAMOND_BOOTS;
                }
            default:
                return null;
        }
    }
}

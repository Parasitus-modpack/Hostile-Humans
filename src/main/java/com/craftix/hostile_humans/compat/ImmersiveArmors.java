package com.craftix.hostile_humans.compat;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.HumanInventoryGenerator;
import com.craftix.hostile_humans.entity.entities.HumanTier;
import immersive_armors.Items;
import immersive_armors.item.ExtendedArmorMaterial;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.minecraft.world.item.Items.IRON_HELMET;

public class ImmersiveArmors {

    public static void apply(Human human, float damagePercentMin, float damagePercentMax) {
        RandomSource random = human.getRandom();
        int equipmentLevel;
        switch (human.getTier()) {
            case LEVEL2 -> equipmentLevel = random.nextInt(5, 8);
            case ROAMER -> equipmentLevel = random.nextInt(3, 6);
            default -> equipmentLevel = random.nextInt(0, 3);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            Item item = getItemForSlot(slot, equipmentLevel);
            if (item != null) {
                human.setItemSlot(slot, HumanInventoryGenerator.damage(human, item.getDefaultInstance(), damagePercentMin, damagePercentMax));
            }
        }
    }

    public static Item getItemForSlot(EquipmentSlot equipmentSlot, int equipmentLevel) {
        Map<Integer, ExtendedArmorMaterial> items = new HashMap<>() {
            {
                this.put(0, Items.WOODEN_ARMOR);
                this.put(1, Items.WARRIOR_ARMOR);
                this.put(2, Items.WARRIOR_ARMOR);
                this.put(3, Items.DIVINE_ARMOR);
                this.put(4, Items.PRISMARINE_ARMOR);
                this.put(5, Items.SLIME_ARMOR);
                this.put(6, Items.WOODEN_ARMOR);
                this.put(7, Items.HEAVY_ARMOR);
            }
        };
        if (items.containsKey(equipmentLevel)) {
            String name = items.get(equipmentLevel).m_6082_();
            Supplier<?> supplier;
            switch (equipmentSlot) {
                case HEAD -> {
                    supplier = Items.items.get(name + "_helmet");
                    if (items.get(equipmentLevel) == Items.WARRIOR_ARMOR) {
                        return IRON_HELMET;
                    }
                }
                case CHEST -> supplier = Items.items.get(name + "_chestplate");
                case LEGS -> supplier = Items.items.get(name + "_leggings");
                case FEET -> supplier = Items.items.get(name + "_boots");
                default -> supplier = null;
            }

            if (supplier != null) {
                return (Item) supplier.get();
            }
        }
        return null;
    }
}

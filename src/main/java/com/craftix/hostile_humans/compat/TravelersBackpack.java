package com.craftix.hostile_humans.compat;

import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.tiviacz.travelersbackpack.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.List;

public class TravelersBackpack {
    private static final String BACK_SLOT = "back";

    public static void apply(LivingEntity living) {
        if (living.getRandom().nextInt(0, 3) != 0) {
            return;
        }

        CuriosApi.getCuriosInventory(living).ifPresent(curiosInventory ->
                curiosInventory.getStacksHandler(BACK_SLOT).ifPresent(backSlot -> equipBackpack(curiosInventory, backSlot, living))
        );
    }

    private static void equipBackpack(ICuriosItemHandler curiosInventory, ICurioStacksHandler backSlot, LivingEntity living) {
        if (backSlot.getStacks().getSlots() <= 0 || !backSlot.getStacks().getStackInSlot(0).isEmpty()) {
            return;
        }

        ItemStack backpack = pickBackpack(living);
        if (backpack.isEmpty()) {
            return;
        }

        backpack.getOrCreateTag().putInt("SleepingBagColor",
                DyeColor.values()[living.getRandom().nextInt(DyeColor.values().length)].getId());
        curiosInventory.setEquippedCurio(BACK_SLOT, 0, backpack);
    }

    private static ItemStack pickBackpack(LivingEntity living) {
        List<Item> entries = ModItems.COMPATIBLE_OVERWORLD_BACKPACK_ENTRIES;
        if (entries.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack standardBackpack = ModItems.STANDARD_TRAVELERS_BACKPACK.get().getDefaultInstance();
        for (int attempt = 0; attempt < 8; attempt++) {
            ItemStack backpack = entries.get(living.getRandom().nextInt(entries.size())).getDefaultInstance();
            String itemPath = getItemPath(backpack);

            if (itemPath.contains("end")) {
                continue;
            }

            if (living.getType() != ModEntityType.HUMAN2.get()
                    && (itemPath.contains("netherite")
                    || itemPath.contains("diamond")
                    || itemPath.contains("gold")
                    || itemPath.contains("emerald"))) {
                continue;
            }

            if (!standardBackpack.isEmpty() && living.getRandom().nextFloat() < 0.5f) {
                backpack = standardBackpack.copy();
            }

            return backpack;
        }

        return standardBackpack.isEmpty() ? ItemStack.EMPTY : standardBackpack.copy();
    }

    private static String getItemPath(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().key().location().getPath();
    }
}

package com.craftix.hostile_humans.compat;

import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.craftix.hostile_humans.HumanUtil.EDIBLE_ITEMS;
import static vectorwing.farmersdelight.common.registry.ModItems.*;

public class FarmersDelight {

    public static void addFoodItems() {
        var list = new java.util.ArrayList<>(List.of(EDIBLE_ITEMS));
        list.addAll(List.of(
                COOKED_CHICKEN_CUTS.get().getDefaultInstance(),
                COOKED_BACON.get().getDefaultInstance(),
                COOKED_COD_SLICE.get().getDefaultInstance(),
                COOKED_SALMON_SLICE.get().getDefaultInstance(),
                COOKED_MUTTON_CHOPS.get().getDefaultInstance(),
                SMOKED_HAM.get().getDefaultInstance(),
                BARBECUE_STICK.get().getDefaultInstance(),
                CHICKEN_SANDWICH.get().getDefaultInstance(),
                HAMBURGER.get().getDefaultInstance(),
                BACON_SANDWICH.get().getDefaultInstance(),
                MUTTON_WRAP.get().getDefaultInstance(),
                BEEF_STEW.get().getDefaultInstance(),
                CHICKEN_SOUP.get().getDefaultInstance(),
                FISH_STEW.get().getDefaultInstance(),
                BAKED_COD_STEW.get().getDefaultInstance(),
                BACON_AND_EGGS.get().getDefaultInstance(),
                PASTA_WITH_MEATBALLS.get().getDefaultInstance(),
                PASTA_WITH_MUTTON_CHOP.get().getDefaultInstance(),
                ROASTED_MUTTON_CHOPS.get().getDefaultInstance(),
                STEAK_AND_POTATOES.get().getDefaultInstance(),
                GRILLED_SALMON.get().getDefaultInstance(),
                ROAST_CHICKEN.get().getDefaultInstance(),
                HONEY_GLAZED_HAM.get().getDefaultInstance(),
                SHEPHERDS_PIE.get().getDefaultInstance()
        ));

        EDIBLE_ITEMS = list.toArray(new ItemStack[]{});
    }
}

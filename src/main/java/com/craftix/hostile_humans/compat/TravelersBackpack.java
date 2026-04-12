package com.craftix.hostile_humans.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static com.craftix.hostile_humans.entity.entities.ModEntityType.*;

public class TravelersBackpack {
    private static final String CAPABILITY_UTILS_CLASS = "com.tiviacz.travelersbackpack.capability.CapabilityUtils";
    private static final String REFERENCE_CLASS = "com.tiviacz.travelersbackpack.util.Reference";
    private static final String MOD_ITEMS_CLASS = "com.tiviacz.travelersbackpack.init.ModItems";

    public static void apply() {
        registerEntityType("COMPATIBLE_TYPE_ENTRIES", HUMAN1.get());
        registerEntityType("COMPATIBLE_TYPE_ENTRIES", HUMAN2.get());
        registerEntityType("COMPATIBLE_TYPE_ENTRIES", ROAMER.get());
        registerEntityType("ALLOWED_TYPE_ENTRIES", HUMAN1.get());
        registerEntityType("ALLOWED_TYPE_ENTRIES", HUMAN2.get());
        registerEntityType("ALLOWED_TYPE_ENTRIES", ROAMER.get());
    }

    public static void applyDeath(LivingDeathEvent event) {
        LivingEntity living = event.getEntity();
        if (!isCompatibleType(living.getType())) {
            return;
        }

        if (invokeIsWearingBackpack(living)) {
            Object entityCapability = resolveEntityCapability(living);
            if (entityCapability != null && living.getRandom().nextFloat() < 0.8f) {
                invokeNoArgs(entityCapability, "removeWearable");
            }
        }
    }

    public static void apply(LivingEntity living) {
        Object entityCapability = resolveEntityCapability(living);
        if (entityCapability == null || living.getRandom().nextInt(0, 3) != 0) {
            return;
        }

        ItemStack backpack = pickBackpack(living);
        if (backpack.isEmpty()) {
            return;
        }

        backpack.getOrCreateTag().putInt("SleepingBagColor", DyeColor.values()[living.getRandom().nextInt(DyeColor.values().length)].getId());
        invokeOneArg(entityCapability, "setWearable", ItemStack.class, backpack);
        invokeNoArgs(entityCapability, "synchronise");
    }

    private static boolean isCompatibleType(EntityType<?> entityType) {
        try {
            List<?> compatibleTypes = (List<?>) Class.forName(REFERENCE_CLASS).getField("COMPATIBLE_TYPE_ENTRIES").get(null);
            return compatibleTypes.contains(entityType);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void registerEntityType(String fieldName, EntityType<?> entityType) {
        try {
            Object fieldValue = Class.forName(REFERENCE_CLASS).getField(fieldName).get(null);
            if (fieldValue instanceof List<?> list && !list.contains(entityType)) {
                @SuppressWarnings("unchecked")
                List<EntityType<?>> mutableList = (List<EntityType<?>>) list;
                mutableList.add(entityType);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean invokeIsWearingBackpack(LivingEntity living) {
        try {
            Method method = Class.forName(CAPABILITY_UTILS_CLASS).getMethod("isWearingBackpack", LivingEntity.class);
            return Boolean.TRUE.equals(method.invoke(null, living));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Object resolveEntityCapability(LivingEntity living) {
        try {
            Method method = Class.forName(CAPABILITY_UTILS_CLASS).getMethod("getEntityCapability", LivingEntity.class);
            Object lazyOptional = method.invoke(null, living);
            if (lazyOptional == null) {
                return null;
            }

            Object resolved = invokeNoArgs(lazyOptional, "resolve");
            if (resolved instanceof Optional<?> optional) {
                return optional.orElse(null);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static ItemStack pickBackpack(LivingEntity living) {
        List<Item> entries = getBackpackEntries("COMPATIBLE_OVERWORLD_BACKPACK_ENTRIES");
        if (entries.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack standardBackpack = getRegistryObjectItem("STANDARD_TRAVELERS_BACKPACK");
        for (int attempt = 0; attempt < 8; attempt++) {
            ItemStack backpack = entries.get(living.getRandom().nextInt(entries.size())).getDefaultInstance();
            String itemPath = getItemPath(backpack);

            if (itemPath.contains("end")) {
                continue;
            }

            if (living.getType() != HUMAN2.get() && (itemPath.contains("netherite") || itemPath.contains("diamond") || itemPath.contains("gold") || itemPath.contains("emerald"))) {
                continue;
            }

            if (!standardBackpack.isEmpty() && living.getRandom().nextFloat() < 0.5f) {
                backpack = standardBackpack.copy();
            }

            return backpack;
        }

        return standardBackpack.isEmpty() ? ItemStack.EMPTY : standardBackpack.copy();
    }

    private static List<Item> getBackpackEntries(String fieldName) {
        try {
            Object fieldValue = Class.forName(MOD_ITEMS_CLASS).getField(fieldName).get(null);
            if (fieldValue instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Item> items = (List<Item>) list;
                return items;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return List.of();
    }

    private static ItemStack getRegistryObjectItem(String fieldName) {
        try {
            Field field = Class.forName(MOD_ITEMS_CLASS).getField(fieldName);
            Object registryObject = field.get(null);
            Object item = invokeNoArgs(registryObject, "get");
            if (item instanceof Item backpackItem) {
                return backpackItem.getDefaultInstance();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static String getItemPath(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().key().location().getPath();
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void invokeOneArg(Object target, String methodName, Class<?> parameterType, Object arg) {
        try {
            target.getClass().getMethod(methodName, parameterType).invoke(target, arg);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}

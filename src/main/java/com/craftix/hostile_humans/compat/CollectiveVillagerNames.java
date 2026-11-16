package com.craftix.hostile_humans.compat;

import com.craftix.hostile_humans.HostileHumans;
import com.natamus.collective_common_forge.functions.EntityFunctions;
import com.natamus.villagernames_common_forge.util.Names;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.util.List;

public class CollectiveVillagerNames {

    private static boolean available = true;

    public static void nameEntity(Entity e) {
        if (!available) return;
        try {
            EntityFunctions.nameEntity(e, Names.getRandomName());
        } catch (LinkageError error) {
            disable(error);
        }
    }

    public static void addCustomName(String name) {
        if (!available) return;
        try {
            Field field = Names.class.getField("customnames");
            Object fieldValue = field.get(null);
            if (fieldValue instanceof List<?> list && !list.contains(name)) {
                @SuppressWarnings("unchecked")
                List<String> mutableList = (List<String>) list;
                mutableList.add(name);
            }
        } catch (ReflectiveOperationException ignored) {
        } catch (LinkageError error) {
            disable(error);
        }
    }

    private static void disable(LinkageError error) {
        available = false;
        HostileHumans.LOGGER.warn("Villager Names integration disabled, this version is not compatible: {}", error.toString());
    }
}

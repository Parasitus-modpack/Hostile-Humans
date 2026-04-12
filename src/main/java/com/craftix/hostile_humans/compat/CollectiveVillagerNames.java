package com.craftix.hostile_humans.compat;

import com.natamus.collective_common_forge.functions.EntityFunctions;
import com.natamus.villagernames_common_forge.util.Names;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.util.List;

public class CollectiveVillagerNames {

    public static void nameEntity(Entity e) {
        EntityFunctions.nameEntity(e, Names.getRandomName());
    }

    public static void addCustomName(String name) {
        try {
            Field field = Names.class.getField("customnames");
            Object fieldValue = field.get(null);
            if (fieldValue instanceof List<?> list && !list.contains(name)) {
                @SuppressWarnings("unchecked")
                List<String> mutableList = (List<String>) list;
                mutableList.add(name);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }
}

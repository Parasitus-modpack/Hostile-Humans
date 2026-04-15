package com.craftix.hostile_humans.entity.loadout;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.entity.entities.HumanTier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HumanLoadoutManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<HumanTier, HumanLoadout> LOADOUTS = new EnumMap<>(HumanTier.class);

    private HumanLoadoutManager() {
    }

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    @Nullable
    public static HumanLoadout get(HumanTier tier) {
        return LOADOUTS.get(tier);
    }

    public static final class HumanLoadout {
        public final Rules rules;
        public final ItemPool mainhand;
        public final ItemPool rangedMainhand;
        public final ChanceItemPool offhand;
        public final ItemPool inventory;
        public final ChanceItemPool bonusMainhand;
        public final ArmorSetPool armorSets;

        public HumanLoadout(Rules rules, ItemPool mainhand, ItemPool rangedMainhand, ChanceItemPool offhand,
                            ItemPool inventory, ChanceItemPool bonusMainhand, ArmorSetPool armorSets) {
            this.rules = rules;
            this.mainhand = mainhand;
            this.rangedMainhand = rangedMainhand;
            this.offhand = offhand;
            this.inventory = inventory;
            this.bonusMainhand = bonusMainhand;
            this.armorSets = armorSets;
        }
    }

    public static final class Rules {
        public final float enchantChance;
        public final float damagePercentMin;
        public final float damagePercentMax;

        public Rules(float enchantChance, float damagePercentMin, float damagePercentMax) {
            this.enchantChance = enchantChance;
            this.damagePercentMin = damagePercentMin;
            this.damagePercentMax = damagePercentMax;
        }
    }

    public static class Pool<T extends WeightedEntry> {
        protected final List<T> entries;

        public Pool(List<T> entries) {
            this.entries = entries;
        }

        @Nullable
        public T roll(RandomSource random) {
            List<T> validEntries = this.entries.stream().filter(WeightedEntry::isAvailable).toList();
            if (validEntries.isEmpty()) {
                return null;
            }

            int totalWeight = 0;
            for (T entry : validEntries) {
                totalWeight += Math.max(1, entry.weight());
            }

            int roll = random.nextInt(totalWeight);
            for (T entry : validEntries) {
                roll -= Math.max(1, entry.weight());
                if (roll < 0) {
                    return entry;
                }
            }
            return validEntries.get(validEntries.size() - 1);
        }

        public boolean isEmpty() {
            return this.entries.isEmpty();
        }
    }

    public static class ItemPool extends Pool<ItemEntry> {
        public ItemPool(List<ItemEntry> entries) {
            super(entries);
        }
    }

    public static final class ChanceItemPool extends ItemPool {
        public final float chance;

        public ChanceItemPool(float chance, List<ItemEntry> entries) {
            super(entries);
            this.chance = chance;
        }
    }

    public static final class ArmorSetPool extends Pool<ArmorSetEntry> {
        public ArmorSetPool(List<ArmorSetEntry> entries) {
            super(entries);
        }
    }

    public interface WeightedEntry {
        int weight();

        boolean isAvailable();
    }

    public static final class ItemEntry implements WeightedEntry {
        public final ResourceLocation itemId;
        public final int weight;
        @Nullable
        public final String requiresMod;

        public ItemEntry(ResourceLocation itemId, int weight, @Nullable String requiresMod) {
            this.itemId = itemId;
            this.weight = weight;
            this.requiresMod = requiresMod;
        }

        @Override
        public int weight() {
            return this.weight;
        }

        @Override
        public boolean isAvailable() {
            return (this.requiresMod == null || ModList.get().isLoaded(this.requiresMod))
                    && ForgeRegistries.ITEMS.containsKey(this.itemId);
        }
    }

    public static final class ArmorSetEntry implements WeightedEntry {
        public final int weight;
        @Nullable
        public final String requiresMod;
        @Nullable
        public final ResourceLocation head;
        @Nullable
        public final ResourceLocation chest;
        @Nullable
        public final ResourceLocation legs;
        @Nullable
        public final ResourceLocation feet;

        public ArmorSetEntry(int weight, @Nullable String requiresMod, @Nullable ResourceLocation head,
                             @Nullable ResourceLocation chest, @Nullable ResourceLocation legs, @Nullable ResourceLocation feet) {
            this.weight = weight;
            this.requiresMod = requiresMod;
            this.head = head;
            this.chest = chest;
            this.legs = legs;
            this.feet = feet;
        }

        @Override
        public int weight() {
            return this.weight;
        }

        @Override
        public boolean isAvailable() {
            return (this.requiresMod == null || ModList.get().isLoaded(this.requiresMod))
                    && isPresent(this.head)
                    && isPresent(this.chest)
                    && isPresent(this.legs)
                    && isPresent(this.feet);
        }

        private static boolean isPresent(@Nullable ResourceLocation location) {
            return location == null || ForgeRegistries.ITEMS.containsKey(location);
        }
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, "human_loadouts");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            EnumMap<HumanTier, HumanLoadout> parsedLoadouts = new EnumMap<>(HumanTier.class);
            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
                HumanTier tier = parseTier(entry.getKey());
                if (tier == null || !entry.getValue().isJsonObject()) {
                    continue;
                }

                try {
                    parsedLoadouts.put(tier, parseLoadout(entry.getValue().getAsJsonObject()));
                } catch (RuntimeException exception) {
                    HostileHumans.LOGGER.error("Failed to parse human loadout {}", entry.getKey(), exception);
                }
            }

            LOADOUTS.clear();
            LOADOUTS.putAll(parsedLoadouts);
            HostileHumans.LOGGER.info("Loaded {} human loadout definitions", LOADOUTS.size());
        }

        @Nullable
        private static HumanTier parseTier(ResourceLocation id) {
            return switch (id.getPath()) {
                case "tier1" -> HumanTier.LEVEL1;
                case "tier2" -> HumanTier.LEVEL2;
                case "roamer" -> HumanTier.ROAMER;
                default -> null;
            };
        }

        private static HumanLoadout parseLoadout(JsonObject root) {
            JsonObject rulesObject = GsonHelper.getAsJsonObject(root, "rules", new JsonObject());
            Rules rules = new Rules(
                    GsonHelper.getAsFloat(rulesObject, "enchant_chance", 0.0F),
                    GsonHelper.getAsFloat(rulesObject, "damage_percent_min", 0.0F),
                    GsonHelper.getAsFloat(rulesObject, "damage_percent_max", 0.85F)
            );

            ItemPool mainhand = new ItemPool(parseItemEntries(GsonHelper.getAsJsonArray(root, "mainhand")));
            ItemPool rangedMainhand = new ItemPool(parseItemEntries(GsonHelper.getAsJsonArray(root, "ranged_mainhand", null)));
            ChanceItemPool offhand = parseChanceItemPool(GsonHelper.getAsJsonObject(root, "offhand", new JsonObject()));
            ItemPool inventory = new ItemPool(parseItemEntries(GsonHelper.getAsJsonArray(root, "inventory", null)));
            ChanceItemPool bonusMainhand = parseChanceItemPool(GsonHelper.getAsJsonObject(root, "bonus_mainhand", new JsonObject()));
            ArmorSetPool armorSets = new ArmorSetPool(parseArmorSets(GsonHelper.getAsJsonArray(root, "armor_sets")));

            return new HumanLoadout(rules, mainhand, rangedMainhand, offhand, inventory, bonusMainhand, armorSets);
        }

        private static ChanceItemPool parseChanceItemPool(JsonObject object) {
            float chance = GsonHelper.getAsFloat(object, "chance", 0.0F);
            return new ChanceItemPool(chance, parseItemEntries(GsonHelper.getAsJsonArray(object, "entries", null)));
        }

        private static List<ItemEntry> parseItemEntries(@Nullable com.google.gson.JsonArray array) {
            List<ItemEntry> entries = new ArrayList<>();
            if (array == null) {
                return entries;
            }

            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                entries.add(new ItemEntry(
                        new ResourceLocation(GsonHelper.getAsString(object, "item")),
                        GsonHelper.getAsInt(object, "weight", 1),
                        GsonHelper.getAsString(object, "requires_mod", null)
                ));
            }
            return entries;
        }

        private static List<ArmorSetEntry> parseArmorSets(@Nullable com.google.gson.JsonArray array) {
            List<ArmorSetEntry> entries = new ArrayList<>();
            if (array == null) {
                return entries;
            }

            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                entries.add(new ArmorSetEntry(
                        GsonHelper.getAsInt(object, "weight", 1),
                        GsonHelper.getAsString(object, "requires_mod", null),
                        parseOptionalId(object, "head"),
                        parseOptionalId(object, "chest"),
                        parseOptionalId(object, "legs"),
                        parseOptionalId(object, "feet")
                ));
            }
            return entries;
        }

        @Nullable
        private static ResourceLocation parseOptionalId(JsonObject object, String key) {
            return object.has(key) ? new ResourceLocation(GsonHelper.getAsString(object, key)) : null;
        }
    }
}

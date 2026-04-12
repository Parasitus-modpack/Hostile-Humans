package com.craftix.hostile_humans;

import com.craftix.hostile_humans.entity.entities.SpawnerEntity;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.ConfigValue<String> disabledStructures;
    public static ForgeConfigSpec.ConfigValue<Integer> maxTargeting;
    public static ForgeConfigSpec.ConfigValue<Double> greetChance;
    public static ForgeConfigSpec.ConfigValue<Double> fleeChance;
    public static ForgeConfigSpec.ConfigValue<Double> fleeHpPercent;
    public static ForgeConfigSpec.ConfigValue<Double> healCombatPercent;
    public static ForgeConfigSpec.ConfigValue<Integer> healUsesPerCombat;
    public static ForgeConfigSpec.ConfigValue<Integer> throwPotionsEvery;
    public static ForgeConfigSpec.ConfigValue<Boolean> runJump;
    public static ForgeConfigSpec.ConfigValue<Boolean> attackJump;
    public static ForgeConfigSpec.ConfigValue<Boolean> patreonNames;
    public static ForgeConfigSpec.ConfigValue<Boolean> noWaystones;
    public static ForgeConfigSpec.EnumValue<SpawnerEntity.SpawnType> eventType;

    static {
        BUILDER.push("Hostile Humans Settings");
        disabledStructures = BUILDER.comment("Disabled Structures (comma separated) ex. cottage, cozy_spruce_house, desert_house, desert_house_2, desert_house_3, desert_house_4, farmhouse, fortress_bottom, fortress_top, igloo, large_desert_house, large_spruce_home, oak_house, oak_house_2, oak_house_3, oak_house_4, oak_house_5, savanna_house_2, spruce_cottage, spruce_fort, spruce_house, thin_spruce, tiny_acacia, tiny_igloo, tiny_spruce_house, tower, warehouse").define("disabled_structures", "");
        maxTargeting = BUILDER.comment("The max amount of humans that can attack you at the same time").define("max_targeting", 3);
        greetChance = BUILDER.comment("The chance to send a chat message to the player upon targeting them").define("greet_chance", 0.05d);
        fleeChance = BUILDER.comment("The chance to run away from the player during the battle").define("flee_chance", 0.3d);
        fleeHpPercent = BUILDER.comment("The % of hp to start fleeing").define("flee_hp", 0.35d);
        healCombatPercent = BUILDER.comment("The % of hp to attempt healing during combat").define("heal_combat", 0.5d);
        healUsesPerCombat = BUILDER.comment("Maximum number of heal consumptions a human can use during one combat").define("heal_uses_per_combat", 2);
        throwPotionsEvery = BUILDER.comment("Throw potions every x ticks").define("throw_potions_every", 20 * 100);
        runJump = BUILDER.comment("Humans can run and jump (like a player)").define("run_jump", true);
        attackJump = BUILDER.comment("Humans can attack and jump (melee crits)").define("attack_jump", true);
        patreonNames = BUILDER.comment("Allow names of Patreon members to show up as viable names").define("patreon_names", true);
        noWaystones = BUILDER.comment("Should waystones not load in structures even with the mod present").define("no_waystones", false);
        eventType = BUILDER.comment("Which type of battle event should occur").defineEnum("battle_event", SpawnerEntity.SpawnType.Random);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

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
    public static ForgeConfigSpec.ConfigValue<Double> runAwayMiddleFightChance;
    public static ForgeConfigSpec.ConfigValue<Double> fleeHpPercent;
    public static ForgeConfigSpec.ConfigValue<Double> healCombatPercent;
    public static ForgeConfigSpec.ConfigValue<Double> chestOpenChance;
    public static ForgeConfigSpec.ConfigValue<Double> preAttackBuffChance;
    public static ForgeConfigSpec.ConfigValue<Double> midBattleBuffInsteadOfRunChance;
    public static ForgeConfigSpec.ConfigValue<Double> meleeFlurryChance;
    public static ForgeConfigSpec.ConfigValue<Integer> healUsesPerCombat;
    public static ForgeConfigSpec.ConfigValue<Integer> meleeAttackCooldownMin;
    public static ForgeConfigSpec.ConfigValue<Integer> meleeAttackCooldownMax;
    public static ForgeConfigSpec.ConfigValue<Integer> meleeFlurryHitsMin;
    public static ForgeConfigSpec.ConfigValue<Integer> meleeFlurryHitsMax;
    public static ForgeConfigSpec.ConfigValue<Integer> throwPotionsEvery;
    public static ForgeConfigSpec.ConfigValue<Boolean> enableRunning;
    public static ForgeConfigSpec.ConfigValue<Boolean> runJump;
    public static ForgeConfigSpec.ConfigValue<Boolean> attackJump;
    public static ForgeConfigSpec.ConfigValue<Double> runLungeVelocity;
    public static ForgeConfigSpec.ConfigValue<Double> runAirAcceleration;
    public static ForgeConfigSpec.ConfigValue<Double> runMaxHorizontalSpeed;
    public static ForgeConfigSpec.ConfigValue<Double> underwaterWaterPotionChance;
    public static ForgeConfigSpec.ConfigValue<Integer> postBreathNoSwimTicks;
    public static ForgeConfigSpec.ConfigValue<Boolean> patreonNames;
    public static ForgeConfigSpec.ConfigValue<Boolean> noWaystones;
    public static ForgeConfigSpec.EnumValue<SpawnerEntity.SpawnType> eventType;

    static {
        BUILDER.push("Hostile Humans Settings");
        disabledStructures = BUILDER.comment("Disabled Structures (comma separated) ex. cottage, cozy_spruce_house, desert_house, desert_house_2, desert_house_3, desert_house_4, farmhouse, fortress_bottom, fortress_top, igloo, large_desert_house, large_spruce_home, oak_house, oak_house_2, oak_house_3, oak_house_4, oak_house_5, savanna_house_2, spruce_cottage, spruce_fort, spruce_house, thin_spruce, tiny_acacia, tiny_igloo, tiny_spruce_house, tower, warehouse").define("disabled_structures", "");
        maxTargeting = BUILDER.comment("The max amount of humans that can attack you at the same time").define("max_targeting", 3);
        greetChance = BUILDER.comment("The chance to send a chat message to the player upon targeting them").define("greet_chance", 0.05d);
        fleeChance = BUILDER.comment("The chance to run away from the player during the battle").define("flee_chance", 0.3d);
        runAwayMiddleFightChance = BUILDER.comment("Chance that a human chooses to run away once low health mid-fight").define("run_away_middle_fight_chance", 0.5d);
        fleeHpPercent = BUILDER.comment("The % of hp to start fleeing").define("flee_hp", 0.15d);
        healCombatPercent = BUILDER.comment("The % of hp to attempt healing during combat").define("heal_combat", 0.5d);
        chestOpenChance = BUILDER.comment("Chance that an idle human will decide to inspect a nearby chest when eligible").define("chest_open_chance", 0.1d);
        preAttackBuffChance = BUILDER.comment("Chance that a human will use a pre-attack buff item once after spotting a player").define("pre_attack_buff_chance", 0.05d);
        midBattleBuffInsteadOfRunChance = BUILDER.comment("Chance that a tier 2 human eats an enchanted golden apple instead of fleeing mid-fight").define("mid_battle_buff_instead_of_run_chance", 0.01d);
        meleeFlurryChance = BUILDER.comment("Rare chance that a human enters a short low-damage melee flurry").define("melee_flurry_chance", 0.05d);
        healUsesPerCombat = BUILDER.comment("Maximum number of heal consumptions a human can use during one combat").define("heal_uses_per_combat", 2);
        meleeAttackCooldownMin = BUILDER.comment("Minimum melee attack cooldown in ticks").define("melee_attack_cooldown_min", 3);
        meleeAttackCooldownMax = BUILDER.comment("Maximum melee attack cooldown in ticks").define("melee_attack_cooldown_max", 12);
        meleeFlurryHitsMin = BUILDER.comment("Minimum number of hits in a melee flurry").define("melee_flurry_hits_min", 3);
        meleeFlurryHitsMax = BUILDER.comment("Maximum number of hits in a melee flurry").define("melee_flurry_hits_max", 6);
        throwPotionsEvery = BUILDER.comment("Throw potions every x ticks").define("throw_potions_every", 20 * 100);
        enableRunning = BUILDER.comment("Enable run behavior (run control, sprint chase, and run-jump lunges)").define("enable_running", false);
        runJump = BUILDER.comment("Humans can run and jump (like a player)").define("run_jump", true);
        attackJump = BUILDER.comment("Humans can attack and jump (melee crits)").define("attack_jump", true);
        runLungeVelocity = BUILDER.comment("Forward velocity applied when humans perform a long-distance chase lunge").define("run_lunge_velocity", 0.75d);
        runAirAcceleration = BUILDER.comment("Per-tick horizontal acceleration toward target while airborne during a lunge").define("run_air_acceleration", 0.12d);
        runMaxHorizontalSpeed = BUILDER.comment("Maximum horizontal speed while airborne during chase lunges").define("run_max_horizontal_speed", 1.35d);
        underwaterWaterPotionChance = BUILDER.comment("Chance [0..1] that a tier 2 human chooses a water breathing potion when underwater and lacking the effect").define("underwater_water_potion_chance", 0.15d);
        postBreathNoSwimTicks = BUILDER.comment("Ticks a human must avoid diving back down after surfacing for air").define("post_breath_no_swim_ticks", 40);
        patreonNames = BUILDER.comment("Allow names of Patreon members to show up as viable names").define("patreon_names", true);
        noWaystones = BUILDER.comment("Should waystones not load in structures even with the mod present").define("no_waystones", false);
        eventType = BUILDER.comment("Which type of battle event should occur").defineEnum("battle_event", SpawnerEntity.SpawnType.Random);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}


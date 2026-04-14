package com.craftix.hostile_humans.entity.entities;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.HumanEntity;
import com.craftix.hostile_humans.entity.PotionRangedAttackMob;
import com.craftix.hostile_humans.entity.ai.goal.*;
import com.google.common.collect.Maps;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static com.craftix.hostile_humans.Config.throwPotionsEvery;
import static com.craftix.hostile_humans.HumanUtil.*;
import static com.craftix.hostile_humans.entity.entities.HumanInventoryGenerator.generateInventory;
import static com.craftix.hostile_humans.entity.entities.ModEntityType.ROAMER;

public class Human extends HumanEntity implements RangedAttackMob, CrossbowAttackMob, PotionRangedAttackMob {

    public static final ItemStack[] EXTRA_EDIBLE_ITEMS = new ItemStack[]{Items.GOLDEN_APPLE.getDefaultInstance(), PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.REGENERATION), PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.HEALING), PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.STRENGTH)};
    public static final ItemStack[] PRE_ATTACK_BUFF_ITEMS = new ItemStack[]{
            Items.GOLDEN_APPLE.getDefaultInstance(),
            PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.STRENGTH),
            PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.REGENERATION),
            PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.SWIFTNESS)
    };

    private static final UUID MODIFIER_UUID = UUID.fromString("7a0811af-4025-4691-ba75-2d638d4ab3f4");

    private static final AttributeModifier USE_ITEM_SPEED_PENALTY = new AttributeModifier(MODIFIER_UUID, "Use item speed penalty", -0.25D, AttributeModifier.Operation.ADDITION);
    private static final Map<String, ResourceLocation> TEXTURE_BY_VARIANT = Util.make(Maps.newHashMap(), hashMap -> {
        for (int i = 1; i <= 37; i++) {
            String name = "skin" + i;
            hashMap.put(name, new ResourceLocation(HostileHumans.MOD_ID, "textures/entity/human/" + name + ".png"));
        }
    });
    private final CrossbowGoal<Human> crossbowAttackGoal = new CrossbowGoal<>(this, 1D, 15.0F);
    private final TridentAttackGoal tridentGoal = new TridentAttackGoal(this, 1.0D, 40, 15.0F);
    private final BowAttack<Human> bowAttackGoal = new BowAttack<>(this, 1D, 80, 15.0F);
    private final MeleeAttackGoal meleeAttackGoal = new MeleeAttackGoal(this, 1.05D, true);
    public int shieldCoolDown;
    public int shieldUpTicks;
    public int ticksEyesOutOfWater;
    public int switchingWeaponCoolDown;
    public int meleeFlurryHitsRemaining;
    public int meleeFlurryDamageTicks;

    public int onPlayerJumpCoolDown;
    public int eatingColldown;
    private boolean queuedPreAttackBuff;
    private boolean resolvedPreAttackBuffThisCombat;
    private boolean resolvedFleeThisCombat;
    private boolean shouldFleeThisCombat;
    @Nullable
    private InteractionHand pendingDrinkCleanupHand;
    private ItemStack pendingDrinkCleanupStack = ItemStack.EMPTY;
    private ItemStack pendingDrinkCleanupRemainder = ItemStack.EMPTY;
    private boolean pendingDrinkCleanup;
    public boolean isFleeing;
    public long lastCombatTime;
    @Nullable
    public LivingEntity toAvoid;
    // Investigate Sound
    public BlockPos investigateSound = BlockPos.ZERO;
    public BlockPos investigateSound() {
		return investigateSound;
	}
    public void setInvestigateSound(BlockPos investigateSound) {
		this.investigateSound = investigateSound;
	}
    // Chest
    public int lookForChestCooldown;
    // Food
    public HumanFood food = new HumanFood();
    public int healCooldown;

    public void addExhaustion(float p_38704_) {
       this.food.exhaustionLevel = Math.min(this.food.exhaustionLevel + p_38704_, 40.0F);
    }

    public boolean needsFood() {
       return this.food.foodLevel < 20;
    }

    public void eat(int food, float sat) {
       this.food.foodLevel = Math.min(food + this.food.foodLevel, 20);
       this.food.saturationLevel = Math.min(this.food.saturationLevel + (float)food * sat * 2.0F, (float)this.food.foodLevel);
    }
    //
    public int ticksOutOfCombat;
    public int timesHealedInCombat;
    //

    public boolean isAlert;

    public Human(EntityType<? extends HumanEntity> entityType, Level level, HumanTier type) {
        super(entityType, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setCanPickUpLoot(true);
//        this.setCustomName(null);
        setTier(type);
        initTeam(type);



        this.moveControl = new Human.HumanMoveControl(this);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.waterNavigation = new WaterBoundPathNavigation(this, level);
        this.groundNavigation = new GroundPathNavigation(this, level);
        this.groundNavigation.setCanOpenDoors(true);
        this.groundNavigation.setCanPassDoors(true);
        this.groundNavigation.setMaxVisitedNodesMultiplier(50);
        this.navigation = this.groundNavigation;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.ATTACK_DAMAGE, 1.0D).add(ForgeMod.ENTITY_REACH.get(), 3).add(Attributes.FOLLOW_RANGE, 40);
    }

    public void applySpawnedWeaponEnchantments(RandomSource random, float enchantChance) {
        this.enchantSpawnedWeapon(random, enchantChance);
    }

    public void applySpawnedArmorEnchantments(RandomSource random, float enchantChance, EquipmentSlot equipmentSlot) {
        this.enchantSpawnedArmor(random, enchantChance, equipmentSlot);
    }

    protected PathNavigation createNavigation(Level p_33802_) {
        return new GroundPathNavigation(this, p_33802_);
    }

    private void initTeam(HumanTier type) {
        if (team.isEmpty()) {
            if (type == HumanTier.ROAMER) {
                team = "roamer" + getRandom().nextInt(1, 100000);
            } else {
                team = "human";
            }
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        lastCombatTime = tickCount;

        if (damageSource.getEntity() instanceof LivingEntity attacker && attacker != this && this.canAttack(attacker)) {
            setTarget(attacker);
        }

        if (amount > 1) {
            var slots = EquipmentSlot.values();
            for (EquipmentSlot equipmentslot : slots) {
                if (equipmentslot.getType() == EquipmentSlot.Type.ARMOR) {
                    if (random.nextFloat() < (getTier() == HumanTier.LEVEL1 ? 0.0025 : 0.0025 / 2)) {
                        var item = this.getItemBySlot(equipmentslot);
                        if (!item.isEmpty()) {
                            playSound(SoundEvents.ITEM_BREAK, 1, 1);
                            setItemSlot(equipmentslot, Items.AIR.getDefaultInstance());
                        }
                    }
                }
            }
        }
        return super.hurt(damageSource, amount);
    }

    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    public ResourceLocation getResourceLocation() {
        return TEXTURE_BY_VARIANT.get(getVariant());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        goalSelector.addGoal(-10, new HumanFloatGoal(this));
        goalSelector.addGoal(-10, new AvoidCreeperGoal(this, 10, 1.0D, 1.2D));
        goalSelector.addGoal(-5, new OpenDoorsGoal(this, true));
        goalSelector.addGoal(-5, new OpenFenceGoal(this, true));
        goalSelector.addGoal(-5, new OpenTrapdoorGoal(this, true));
        goalSelector.addGoal(-5, new LadderClimbGoal(this));
        goalSelector.addGoal(0, new FindWaterOnFireGoal(this, 1.2D));
        goalSelector.addGoal(0, new RunFromTarget(this, 6.0F, 1.0D, 1.2D));
        goalSelector.addGoal(0, new AvoidTNTGoal(this, 6.0F, 1.0D, 1.2D));
        goalSelector.addGoal(0, new InvestigateSoundGoal(this, 1.0F));
        goalSelector.addGoal(1, new PotionRangedAttackGoal(this, 1.0, 10, 10));
        goalSelector.addGoal(3, new RaiseShieldGoal(this));
        goalSelector.addGoal(-30, new LookForChestGoal(this, 1.0F));
        goalSelector.addGoal(-30, new LookForBedGoal(this, 1.0F));
        if ((this.getType() == ROAMER.get())) {
            goalSelector.addGoal(8, new RandomStrollGoalFar(this, 0.65D, 15, false));
        } else {
            goalSelector.addGoal(8, new RandomStrollGoalWithHome(this, 0.65D, 120, true));
        }

        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        goalSelector.addGoal(11, new HumanLookAtPlayerGoal(this, Player.class, 64.0F));
        targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoalCustom<>(this, LivingEntity.class, 13, true, false, this::isAngryAt));
        targetSelector.addGoal(1, new NearestAttackableTargetGoalWithHumanLimiter<>(this, Player.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false, (target) -> {
        	if (target instanceof EnderMan) return false;
            if (this.getTier() == HumanTier.ROAMER) {
                return target instanceof Animal && !(target instanceof Bee) && String.valueOf(target.getId()).hashCode() % 100 < 30; //only attack 30% of animals
            }
            return target instanceof Enemy && (!(target instanceof Creeper) || HumanUtil.shouldFightCreeper(this));
        }));
    }

    public void setCombatTask() {
        if (!level().isClientSide) {

            goalSelector.removeGoal(bowAttackGoal);
            goalSelector.removeGoal(meleeAttackGoal);
            goalSelector.removeGoal(crossbowAttackGoal);
            goalSelector.removeGoal(tridentGoal);

            ItemStack itemstack = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, this::canFireProjectileWeapon));
            if (getMainHandItem().getItem() instanceof TridentItem) {
                goalSelector.addGoal(2, tridentGoal);
                goalSelector.addGoal(3, meleeAttackGoal);
            } else if (itemstack.getItem() instanceof CrossbowItem) {
                goalSelector.addGoal(2, crossbowAttackGoal);
            } else if (itemstack.getItem() instanceof BowItem) {
                bowAttackGoal.setMinAttackInterval(40);
                goalSelector.addGoal(2, bowAttackGoal);
            } else {
                goalSelector.addGoal(2, meleeAttackGoal);
            }
        }
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity entityIn) {
        float originalAttackDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (this.meleeFlurryDamageTicks > 0) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(0.5D, originalAttackDamage * 0.4D));
        }

        boolean result = super.doHurtTarget(entityIn);

        if (this.meleeFlurryDamageTicks > 0) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(originalAttackDamage);
        }

        swing(InteractionHand.MAIN_HAND);
        return result;
    }

    @Override
    protected void blockUsingShield(LivingEntity entityIn) {
        super.blockUsingShield(entityIn);
        if (entityIn.getMainHandItem().canDisableShield(this.useItem, this, entityIn)) this.disableShield(true);
    }

    public void disableShield(boolean increase) {
        float chance = 0.25F + (float) EnchantmentHelper.getBlockEfficiency(this) * 0.05F;
        if (increase) chance += 0.75;
        if (this.random.nextFloat() < chance) {
            this.shieldCoolDown = 100;
            this.stopUsingItem();
            this.level().broadcastEntityEvent(this, (byte) 30);
        }
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficulty, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag compoundTag) {
        spawnGroupData = super.finalizeSpawn(serverLevelAccessor, difficulty, mobSpawnType, spawnGroupData, compoundTag);

        List<String> variants = new ArrayList<>(TEXTURE_BY_VARIANT.keySet());
        setRandomVariant(variants);

        setCanPickUpLoot(true);
        return spawnGroupData;
    }

    private void setRandomVariant(List<String> variants) {
        setVariant(variants.get(this.random.nextInt(variants.size())));
    }

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {
        super.setItemSlot(slotIn, stack);
        if (!this.level().isClientSide && !stack.isEmpty()) {
            this.setCombatTask();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        if (this.isBlocking()) {
            return SoundEvents.SHIELD_BLOCK;
        }
        return super.getHurtSound(damageSourceIn);
    }

    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        if (this.useItem.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) {
            if (damage >= 3.0F) {
                int i = 1 + Mth.floor(damage);
                InteractionHand hand = this.getUsedItemHand();
                this.useItem.hurtAndBreak(i, this, (entity) -> entity.broadcastBreakEvent(hand));
                if (this.useItem.isEmpty()) {
                    if (hand == InteractionHand.MAIN_HAND) {
                        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }
                    this.useItem = ItemStack.EMPTY;
                    this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F);
                }
            }
        }
    }

    @Override
    public void startUsingItem(@NotNull InteractionHand hand) {
        super.startUsingItem(hand);
        ItemStack itemstack = this.getItemInHand(hand);
        if (itemstack.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK) || isFood(itemstack)) {
            AttributeInstance modifiableattributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            modifiableattributeinstance.removeModifier(USE_ITEM_SPEED_PENALTY);
            modifiableattributeinstance.addTransientModifier(USE_ITEM_SPEED_PENALTY);
        }
        if (!this.level().isClientSide) {
            trackPendingDrinkCleanup(hand, itemstack);
        }
    }

    @Override
    public void stopUsingItem() {
        int remainingTicks = this.getUseItemRemainingTicks();
        super.stopUsingItem();
        if (this.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(USE_ITEM_SPEED_PENALTY))
            this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(USE_ITEM_SPEED_PENALTY);
        if (!this.level().isClientSide && !this.pendingDrinkCleanupStack.isEmpty()) {
            if (remainingTicks <= 1) {
                this.pendingDrinkCleanup = true;
            } else {
                clearPendingDrinkCleanup();
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setCombatTask();
    }

    @Override
    public boolean canAttack(LivingEntity entity) {
        if (entity instanceof Human otherHuman && otherHuman.isAlive()) {
            return !this.team.equals(otherHuman.team);
        }

        return super.canAttack(entity);
    }

    public boolean isAngryAt(LivingEntity entity) {
        if (entity instanceof Human otherHuman && otherHuman.isAlive()) {
            return !this.team.equals(otherHuman.team);
        }

        if (!this.canAttack(entity)) {
            return false;
        }
        if ((entity) instanceof AbstractSchoolingFish) {
            return false;
        }

        if (entity instanceof Player) {
            return false;
        }

        return entity.getUUID().equals(this.getPersistentAngerTarget());
    }

    @Override
    public void setTarget(@Nullable LivingEntity livingEntity) {
        LivingEntity previousTarget = this.getTarget();
        super.setTarget(livingEntity);

        if (this.level().isClientSide) {
            return;
        }

        if (this.getTier() == HumanTier.LEVEL2 && livingEntity instanceof Player && previousTarget != livingEntity && !this.resolvedPreAttackBuffThisCombat) {
            this.resolvedPreAttackBuffThisCombat = true;
            this.queuedPreAttackBuff = this.random.nextFloat() < Config.preAttackBuffChance.get();
        } else if (livingEntity == null && this.ticksOutOfCombat > 20 * 60 * 2) {
            this.queuedPreAttackBuff = false;
            this.resolvedPreAttackBuffThisCombat = false;
        }
    }

    @Override
    public void finalizeSpawn() {
        super.finalizeSpawn();
        generateInventory(this, false);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    public void setBanner(ItemStack banner) {
        this.setItemSlot(EquipmentSlot.HEAD, banner);
    }

    public void putItemAway(ItemStack stack) {
        for (int i = 0; i < 16; i++) {
            if (getData().getInventoryItem(i).isEmpty()) {
                getData().setInventoryItem(i, stack.copy());
                break;
            }
        }
        stack.shrink(stack.getCount());
        setCombatTask();
    }

    public boolean equipWeapon(Predicate<ItemStack> predicate) {
        return equipWeapon(predicate, EquipmentSlot.MAINHAND);
    }

    public boolean equipWeapon(Predicate<ItemStack> predicate, EquipmentSlot slot) {
        for (int i = 0; i < 16; i++) {
            ItemStack inventoryItem = getData().getInventoryItem(i);
            if (predicate.test(inventoryItem)) {

                if (!getMainHandItem().isEmpty()) {
                    putItemAway(getMainHandItem().copy());
                }
                if (!getOffhandItem().isEmpty()) {
                    putItemAway(getOffhandItem().copy());
                }
                setItemSlot(slot, inventoryItem.copy());
                inventoryItem.shrink(inventoryItem.getCount());
                setCombatTask();
                return true;
            }
        }

        return false;
    }

    @Override
    protected void completeUsingItem() {
    	InteractionHand hand = this.getUsedItemHand();
        EquipmentSlot handSlot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        ItemStack usedStack = this.getItemInHand(hand).copy();
//          System.out.println("release "+hand+" "+this.useItem+" "+this.isUsingItem()+" "+this.getItemInHand(this.getUsedItemHand()));
        if (isFood(useItem)) {
        	if (useItem.getFoodProperties(this) != null) {
        		FoodProperties foodproperties = useItem.getFoodProperties(this);
        		this.eat(foodproperties.getNutrition(), foodproperties.getSaturationModifier());
        	}

        	if (this.getTarget() != null && this.timesHealedInCombat < Config.healUsesPerCombat.get()) {
        		timesHealedInCombat++;
        	}
        }
        super.completeUsingItem();
        if (!this.level().isClientSide) {
            ItemStack currentHandStack = this.getItemInHand(hand);
            ItemStack remainderStack = resolveConsumedRemainder(usedStack, currentHandStack);

            if (!remainderStack.isEmpty()) {
                if (!currentHandStack.isEmpty()
                        && (ItemStack.isSameItemSameTags(currentHandStack, usedStack)
                        || ItemStack.isSameItemSameTags(currentHandStack, remainderStack)
                        || usedStack.getUseAnimation() == UseAnim.DRINK)) {
                    this.setItemSlot(handSlot, ItemStack.EMPTY);
                    currentHandStack = ItemStack.EMPTY;
                }

                if (currentHandStack.isEmpty()) {
                    storeConsumedRemainder(remainderStack.copy());
                }
            }

            syncHandData(handSlot, currentHandStack);
            clearPendingDrinkCleanup();
        }
    }

    private void trackPendingDrinkCleanup(InteractionHand hand, ItemStack stack) {
        if (shouldTrackDrinkSanity(stack)) {
            this.pendingDrinkCleanupHand = hand;
            this.pendingDrinkCleanupStack = stack.copy();
            this.pendingDrinkCleanupRemainder = resolveExpectedDrinkRemainder(stack);
            this.pendingDrinkCleanup = false;
        } else {
            clearPendingDrinkCleanup();
        }
    }

    private boolean shouldTrackDrinkSanity(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getUseAnimation() == UseAnim.DRINK;
    }

    private void clearPendingDrinkCleanup() {
        this.pendingDrinkCleanupHand = null;
        this.pendingDrinkCleanupStack = ItemStack.EMPTY;
        this.pendingDrinkCleanupRemainder = ItemStack.EMPTY;
        this.pendingDrinkCleanup = false;
    }

    private void sanityClearPendingDrinkItem() {
        if (this.level().isClientSide || !this.pendingDrinkCleanup || this.pendingDrinkCleanupHand == null || this.isUsingItem()) {
            return;
        }

        ItemStack currentHandStack = this.getItemInHand(this.pendingDrinkCleanupHand);
        if (currentHandStack.isEmpty()) {
            clearPendingDrinkCleanup();
            return;
        }

        if (ItemStack.isSameItemSameTags(currentHandStack, this.pendingDrinkCleanupStack)
                || (!this.pendingDrinkCleanupRemainder.isEmpty()
                && ItemStack.isSameItemSameTags(currentHandStack, this.pendingDrinkCleanupRemainder))) {
            EquipmentSlot handSlot = this.pendingDrinkCleanupHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            this.setItemSlot(handSlot, ItemStack.EMPTY);
            storeConsumedRemainder(this.pendingDrinkCleanupRemainder.copy());
            syncHandData(handSlot, ItemStack.EMPTY);
        }

        clearPendingDrinkCleanup();
    }

    private void storeConsumedRemainder(ItemStack remainderStack) {
        if (remainderStack.isEmpty()) {
            return;
        }

        if (!storeInventoryItemAnywhere(remainderStack.copy())) {
            this.spawnAtLocation(remainderStack);
        }
    }

    private boolean storeInventoryItemAnywhere(ItemStack stack) {
        if (getData() == null || stack.isEmpty()) {
            return false;
        }

        for (int i = 0; i < getData().getInventoryItemsSize(); i++) {
            ItemStack existing = getData().getInventoryItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }

        for (int i = 0; i < getData().getInventoryItemsSize(); i++) {
            if (getData().getInventoryItem(i).isEmpty()) {
                getData().setInventoryItem(i, stack.copy());
                return true;
            }
        }

        return false;
    }

    private ItemStack resolveExpectedDrinkRemainder(ItemStack usedStack) {
        if (usedStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (usedStack.getItem().hasCraftingRemainingItem()) {
            return usedStack.getItem().getCraftingRemainingItem().getDefaultInstance();
        }

        if (usedStack.getUseAnimation() == UseAnim.DRINK && usedStack.getItem() == Items.POTION) {
            return Items.GLASS_BOTTLE.getDefaultInstance();
        }

        return ItemStack.EMPTY;
    }

    private ItemStack resolveConsumedRemainder(ItemStack usedStack, ItemStack currentHandStack) {
        ItemStack expectedRemainder = resolveExpectedDrinkRemainder(usedStack);
        if (!expectedRemainder.isEmpty()) {
            return expectedRemainder;
        }

        if (usedStack.getUseAnimation() == UseAnim.DRINK && !currentHandStack.isEmpty()
                && !ItemStack.isSameItemSameTags(currentHandStack, usedStack)) {
            return currentHandStack.copy();
        }

        return ItemStack.EMPTY;
    }

    private void syncHandData(EquipmentSlot handSlot, ItemStack stack) {
        if (getData() != null) {
            getData().setHandItem(handSlot == EquipmentSlot.MAINHAND ? 0 : 1, stack.copy());
        }
        setDataSyncNeeded();
    }

    @Override
    public void tick() {
        super.tick();
        sanityClearPendingDrinkItem();
        if (this.lookForChestCooldown > 0) this.lookForChestCooldown--;
        if (this.getTarget() != null) {
            ticksOutOfCombat = 0;
        } else {
            ticksOutOfCombat++;
            if (ticksOutOfCombat > 20 * 60 * 2) {
                timesHealedInCombat = 0;
                queuedPreAttackBuff = false;
                resolvedPreAttackBuffThisCombat = false;
                resolvedFleeThisCombat = false;
                shouldFleeThisCombat = false;
            }
        }

        if (this.wasEyeInWater) this.ticksEyesOutOfWater = 0;
        else this.ticksEyesOutOfWater++;

        if (this.level().isNight() && !this.hasDecidedToSleepTonight()) {
        	this.setSleepingThisNight(this.random.nextFloat() < .3f); //only sleep 30% of the time
        	this.setHasDecidedToSleepTonight(true);
        } else if (!this.level().isNight() && this.hasDecidedToSleepTonight()) {
        	this.setSleepingThisNight(false);
        	this.setHasDecidedToSleepTonight(false);
        }

        if (this.isSleeping()) {
        	if (!this.level().isClientSide && !this.level().isNight()) {
        		this.stopSleeping();
        	}
        } else {
            if (this.shouldUseWaterMovement()) {
                this.setPose(Pose.SWIMMING);
            } else if (this.getPose() == Pose.SWIMMING) {
                this.setPose(Pose.STANDING);
            }
        }

        //Healing
    	if (this.food.exhaustionLevel > 4.0F) {
            this.food.exhaustionLevel -= 4.0F;
            if (this.food.saturationLevel > 0.0F) {
               this.food.saturationLevel = Math.max(this.food.saturationLevel - 1.0F, 0.0F);
            }
            this.food.foodLevel = Math.max(this.food.foodLevel - 1, 0);
         }
    	if (this.food.saturationLevel > 0.0F && this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth() && this.food.foodLevel >= 20) {
            ++this.healCooldown;
            if (this.healCooldown >= 10) {
               float f = Math.min(this.food.saturationLevel, 6.0F);
               heal(f / 6.0F);
               this.addExhaustion(f);
               this.healCooldown = 0;
            }
         } else if (this.food.foodLevel >= 18 && this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth()) {
            ++this.healCooldown;
            if (this.healCooldown >= 80) {
               heal(1.0F);
               this.addExhaustion(6.0F);
               this.healCooldown = 0;
            }
         } else {
            this.healCooldown = 0;
         }


        if (level().isClientSide || this.isSleeping()) return;

        if (this.getAirSupply() <= this.getMaxAirSupply() / 8 && !shouldCatchBreath) {
            shouldCatchBreath = true;
            breathRecoveryTicks = this.getRandom().nextInt(20 * 3, 20 * 5 + 1);
        }
        if (shouldCatchBreath && !this.isEyeInFluid(FluidTags.WATER) && breathRecoveryTicks > 0) {
            breathRecoveryTicks--;
        }
        if (shouldCatchBreath && breathRecoveryTicks <= 0 && !this.isEyeInFluid(FluidTags.WATER)) {
            shouldCatchBreath = false;
        }

        if (tickCount % 20 == 0) {
            if (hasCustomName() && getCustomName().getString().contains("give_random_gear")) {
                setNoAi(true);
                return;
            } else setNoAi(false);
        }

        if (getData() == null) {
            HostileHumans.LOGGER.warn("Missing data during tick" + " " + this);
            discard();
            return;
        }

        if (toAvoid != null || getTarget() != null) {
            lastCombatTime = tickCount;
        }

        if (shieldUpTicks > 0) this.shieldUpTicks--;

        if (tickCount % 10 == 0) {
        	tryEquipTotem();
            tryEquipShield();
            tryEquipWeapon();
            tryEatingTick();
            tryEquipPotion();
        }
        if (tickCount % (20 * 15) == 0) {
            setCombatTask();
        }
    }

    private void tryEquipShield() {
        if (getOffhandItem().isEmpty()) {
            equipWeapon(HumanUtil::isShield, EquipmentSlot.OFFHAND);
        }
    }

    private void tryEquipTotem() {
    	for (int i = 16; i < 30; i++) {
            ItemStack inventoryItem = getData().getInventoryItem(i);
            if (inventoryItem.getItem() == Items.TOTEM_OF_UNDYING) {
            	for (int j = 0; 0 < 16; j++) {
                    ItemStack inventoryItem2 = getData().getInventoryItem(j);
                    if (inventoryItem2.isEmpty()) {
                    	getData().setInventoryItem(j, inventoryItem.copy());
                    	getData().setInventoryItem(i, ItemStack.EMPTY);
                    	break;
                    }
                }
            	break;
            }
        }


    	equipWeapon((stack) -> stack.getItem() == Items.TOTEM_OF_UNDYING, EquipmentSlot.OFFHAND);
    }

    private void tryEquipPotion() {
        if (getTier() != HumanTier.LEVEL2) return;
        if (getTarget() != null && (tickCount + String.valueOf(getId()).hashCode()) % throwPotionsEvery.get() == 0) {
            Potion potion = Potions.HARMING;
            if (!getTarget().hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                potion = Potions.SLOWNESS;
            } else if (getTarget().getHealth() >= 8.0F && !getTarget().hasEffect(MobEffects.POISON)) {
                potion = Potions.POISON;
            } else if (!getTarget().hasEffect(MobEffects.WEAKNESS) && this.random.nextFloat() < 0.25F) {
                potion = Potions.WEAKNESS;
            }

            if (potion == Potions.POISON && getTarget().getMobType() == MobType.UNDEAD) {
                potion = Potions.REGENERATION;
            }
            if (potion == Potions.HARMING && getTarget().getMobType() == MobType.UNDEAD) {
                potion = Potions.HEALING;
            }

            ItemStack potionItem = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion);

            EquipmentSlot handSlot = random.nextFloat() < 0.3f ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            ItemStack slotItem = this.getItemBySlot(handSlot);

            if (!slotItem.isEmpty()) {
                putItemAway(slotItem);
            }
            potionItem.enchant(Enchantments.VANISHING_CURSE, 1);
            this.setItemSlot(handSlot, potionItem);
        }
    }

    private void tryEatingTick() {
        if (HumanUtil.canStartEating(this)) {
            EquipmentSlot handSlot = random.nextFloat() < 0.3f ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            ItemStack slotItem = this.getItemBySlot(handSlot);

            if (!slotItem.isEmpty()) {
                putItemAway(slotItem);
            }

            if (wantsToSwim() && getTier() == HumanTier.LEVEL2 && !hasEffect(MobEffects.WATER_BREATHING)) {
                this.setItemSlot(handSlot, PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.WATER_BREATHING));
            }
            else if (getTier() == HumanTier.LEVEL2 && random.nextFloat() < 0.5) {
                this.setItemSlot(handSlot, EXTRA_EDIBLE_ITEMS[(int) (Math.random() * EXTRA_EDIBLE_ITEMS.length)]);
            } else {
            	if (getTier() == HumanTier.LEVEL2)
            		this.setItemSlot(handSlot, EDIBLE_ITEMS_2[(int) (Math.random() * EDIBLE_ITEMS_2.length)]);
            	else
            		this.setItemSlot(handSlot, EDIBLE_ITEMS[(int) (Math.random() * EDIBLE_ITEMS.length)]);
            }
            eatingColldown = 5 * 20;
            startUsingItem(handSlot == EquipmentSlot.MAINHAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        }
    }

    private void tryUsePreAttackBuff() {
        if (this.getTier() != HumanTier.LEVEL2 || !queuedPreAttackBuff || !(getTarget() instanceof Player) || isUsingItem() || !HumanUtil.isMeleeWeapon(getMainHandItem())) {
            return;
        }

        EquipmentSlot handSlot = random.nextFloat() < 0.3f ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        ItemStack slotItem = this.getItemBySlot(handSlot);
        if (!slotItem.isEmpty()) {
            putItemAway(slotItem);
        }

        ItemStack buffItem = PRE_ATTACK_BUFF_ITEMS[random.nextInt(PRE_ATTACK_BUFF_ITEMS.length)].copy();
        this.setItemSlot(handSlot, buffItem);
        this.queuedPreAttackBuff = false;
        this.eatingColldown = 5 * 20;
        startUsingItem(handSlot == EquipmentSlot.MAINHAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public boolean shouldStartFleeingThisCombat() {
        if (!resolvedFleeThisCombat) {
            resolvedFleeThisCombat = true;
            shouldFleeThisCombat = this.random.nextFloat() < Config.fleeChance.get();
        }
        return shouldFleeThisCombat;
    }

    private void tryEquipWeapon() {

        if (getTarget() == null && tickCount % (20 * 10) == 0) {
            equipWeapon(HumanUtil::isRangedWeapon);
        }

        // try to equip trident again
        if (tickCount % (20 * 10) == 0) {
            equipWeapon(HumanUtil::isTrident);
        }

        if (getMainHandItem().isEmpty()) {
            if (!equipWeapon(HumanUtil::isTrident))
                equipWeapon(HumanUtil::isMeleeWeapon);
        } else if (switchingWeaponCoolDown == 0) {
            if (this.getTarget() != null && tickCount > 10 && !isUsingItem()) {
                boolean isTargetFar = this.getTarget().distanceTo(this) >= 3 && this.ticksEyesOutOfWater > 120;

                ItemStack handItem = getItemBySlot(EquipmentSlot.MAINHAND);
                boolean forcedMelee = this.getHealth() <= this.getMaxHealth() * 0.3f && String.valueOf(getId()).hashCode() % 100 < 20;

                if ((forcedMelee || !isTargetFar) && !isMeleeWeapon(handItem)) {
                    equipWeapon(HumanUtil::isMeleeWeapon);
                } else if (isTargetFar && !isRangedWeapon(handItem)) {
                    equipWeapon(HumanUtil::isRangedWeapon);
                }
                switchingWeaponCoolDown = 3 * 20;
            }
        }

        tryUsePreAttackBuff();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (tickCount % 220 == 0 && getTarget() == null && !this.level().isClientSide) {
            if (this.getData() != null) for (ItemStack stack : this.getData().getInventoryItems()) {
                equipItemIfPossible(stack);
            }
            else {
                HostileHumans.LOGGER.warn("Missing data?" + " " + this);
                this.remove(RemovalReason.DISCARDED);
            }
        }

        if (this.shieldCoolDown > 0) --this.shieldCoolDown;
        if (this.switchingWeaponCoolDown > 0) --this.switchingWeaponCoolDown;

        if (this.onPlayerJumpCoolDown > 0) --this.onPlayerJumpCoolDown;
        if (this.eatingColldown > 0) --this.eatingColldown;
        if (this.meleeFlurryDamageTicks > 0) --this.meleeFlurryDamageTicks;

        this.updateSwingTime();
    }

    @Override
    public ItemStack equipItemIfPossible(ItemStack stack) {
        if (getTarget() != null) return ItemStack.EMPTY;

        EquipmentSlot equipmentslot = getEquipmentSlotForItem(stack);
        ItemStack itemstack = this.getItemBySlot(equipmentslot);
        boolean flag = this.canReplaceCurrentItem(stack, itemstack);
        if (flag && this.canHoldItem(stack) && !(stack.getItem() instanceof TieredItem)) {
            if (!itemstack.isEmpty()) {
                getData().storeInventoryItem(itemstack);
            }

            ItemStack equippedStack = stack.copyWithCount(1);
            this.setItemSlotAndDropWhenKilled(equipmentslot, equippedStack);
            stack.shrink(1);
            return equippedStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource p_21385_, int p_21386_, boolean p_21387_) {
    	super.dropCustomDeathLoot(p_21385_, p_21386_, p_21387_);

    	for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
    		ItemStack itemstack = this.getItemBySlot(equipmentslot);
    		itemstack.setDamageValue(itemstack.getMaxDamage()-random.nextInt(10));
    		float f = this.getEquipmentDropChance(equipmentslot);
    		boolean flag = f > 1.0F;
    		if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack) && (p_21387_ || flag) && Math.max(this.random.nextFloat() - (float)p_21386_ * 0.01F, 0.0F) < f) {
    			if (!flag && itemstack.isDamageableItem()) {
    				itemstack.setDamageValue(itemstack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemstack.getMaxDamage() - 3, 1))));
    			}

    			this.spawnAtLocation(itemstack);
    			this.setItemSlot(equipmentslot, ItemStack.EMPTY);
    		}
    	}

    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0D, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    @Override
    public Item getTameItem() {
        return Items.DIAMOND;
    }

    @Override
    public Ingredient getFoodItems() {
        return Ingredient.of(EDIBLE_ITEMS);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 20 * 30;
    }

    @Override
    public float getVoicePitch() {
        return 1;
    }

    @Override
    public void setChargingCrossbow(boolean pIsCharging) {
        setCharging(pIsCharging);
    }

    public boolean canFireProjectileWeapon(Item item) {
        return item instanceof ProjectileWeaponItem weaponItem && canFireProjectileWeapon(weaponItem);
    }

    @Override
    public boolean canFireProjectileWeapon(ProjectileWeaponItem item) {
        return item instanceof BowItem || item instanceof CrossbowItem;
    }

    @Override
    public void shootCrossbowProjectile(LivingEntity target, ItemStack crossbow, Projectile projectile, float angle) {
        this.shootCrossbowProjectile(this, target, projectile, angle, 1.6F);
    }

    @Override
    public ItemStack getProjectile(ItemStack p_21272_) {
        return new ItemStack(Items.ARROW);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.getMainHandItem().getItem() instanceof TridentItem) {
            performRangedAttackTrident(target, distanceFactor);
            return;
        }
        this.shieldCoolDown = 8;
        ItemStack weaponStack = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, this::canFireProjectileWeapon));
        if (weaponStack.getItem() instanceof CrossbowItem) {
            this.performCrossbowAttack(this, 1.6F);
        } else {
            ItemStack itemstack = getProjectile(weaponStack);
            AbstractArrow mobArrow = ProjectileUtil.getMobArrow(this, itemstack, distanceFactor);
            if (getMainHandItem().getItem() instanceof BowItem)
                mobArrow = ((BowItem) getMainHandItem().getItem()).customArrow(mobArrow);
            double d0 = target.getX() - this.getX();
            double d1 = target.getY(0.3333333333333333D) - mobArrow.getY();
            double d2 = target.getZ() - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            mobArrow.shoot(d0, d1 + d3 * (double) 0.2F, d2, 1.6F, (float) (14 - this.level().getDifficulty().getId() * 4));
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.level().addFreshEntity(mobArrow);
        }
    }

    public void performRangedAttackTrident(LivingEntity p_32356_, float p_32357_) {

        //getData().setInventoryItem(2, getMainHandItem());
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        var tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.enchant(Enchantments.VANISHING_CURSE, 1);
        tridentStack.enchant(Enchantments.LOYALTY, 1);

        ThrownTrident throwntrident = new ThrownTrident(this.level(), this, tridentStack);

        double d0 = p_32356_.getX() - this.getX();
        double d1 = p_32356_.getY(1f / 3f) - throwntrident.getY();
        double d2 = p_32356_.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        throwntrident.shoot(d0, d1 + d3 * (double) 0.2F, d2, 1.6F, (float) (14 - this.level().getDifficulty().getId() * 4));
        throwntrident.setOwner(this);
        this.playSound(SoundEvents.TRIDENT_THROW, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(throwntrident);
    }

    @Override
    public void performPotionRangedAttack(LivingEntity target, float var2) {
        Vec3 deltaMovement = target.getDeltaMovement();
        double $$3 = target.getX() + deltaMovement.x - this.getX();
        double $$4 = target.getEyeY() - 1.1 - this.getY();
        double $$5 = target.getZ() + deltaMovement.z - this.getZ();
        double $$6 = Math.sqrt($$3 * $$3 + $$5 * $$5);

        ItemStack potionStack = null;
        if (getMainHandItem().getItem() instanceof SplashPotionItem)
            potionStack = getMainHandItem();
        else if (getOffhandItem().getItem() instanceof SplashPotionItem)
            potionStack = getOffhandItem();

        if (potionStack == null) return;

        ThrownPotion thrownPotion = new ThrownPotion(this.level(), this);
        thrownPotion.setItem(potionStack.copy());
        thrownPotion.setXRot(thrownPotion.getXRot() + 20.0F);
        thrownPotion.shoot($$3, $$4 + $$6 * 0.2, $$5, 0.75F, 8.0F);
        potionStack.shrink(1);
        if (!this.isSilent()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_THROW, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        }

        this.level().addFreshEntity(thrownPotion);
    }





    @Override
    public boolean isVisuallySwimming() {
    	return super.isVisuallySwimming() || (this.shouldUseWaterMovement() && this.isEyeInFluid(FluidTags.WATER));
    }

    public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F);
    public static final EntityDimensions SWIMMING_DIMENSIONS = EntityDimensions.scalable(0.6F, 0.6F);
    public EntityDimensions getDimensions(Pose p_36166_) {
    	if (p_36166_ == Pose.SWIMMING) return SWIMMING_DIMENSIONS;
    	return super.getDimensions(p_36166_);
    }

    public boolean shouldCatchBreath;
    public int breathRecoveryTicks;
    public boolean prefersToFloat() {
    	return this.getTarget() == null || this.shouldCatchBreath || this.breathRecoveryTicks > 0;
    }
    protected final WaterBoundPathNavigation waterNavigation;
    protected final GroundPathNavigation groundNavigation;
    public boolean wantsToSwim() {
    	if (this.shouldCatchBreath || this.breathRecoveryTicks > 0) return false;
        if (!this.hasSwimmingClearance()) return false;
    	LivingEntity livingentity = this.getTarget();
        if (livingentity == null) return false;
        if (livingentity.isEyeInFluid(FluidTags.WATER) || livingentity.isInWater()) return true;
    	return this.isInWater() && (this.distanceTo(livingentity) < 4.0F || livingentity.getY() > this.getY() - 0.5D);
    }

    public boolean hasSwimmingClearance() {
        BlockPos feetPos = this.blockPosition();
        BlockPos upperPos = feetPos.above();
        return this.level().getFluidState(feetPos).is(FluidTags.WATER)
                && this.level().getFluidState(upperPos).is(FluidTags.WATER)
                && this.level().getBlockState(upperPos).getCollisionShape(this.level(), upperPos).isEmpty();
    }

    public boolean shouldUseWaterMovement() {
        return this.isInWater() && this.hasSwimmingClearance() && (this.prefersToFloat() || this.wantsToSwim());
    }

    public boolean shouldJumpOutOfWaterToward(double wantedX, double wantedY, double wantedZ) {
        if (!this.isInWater()) {
            return false;
        }

        LivingEntity target = this.getTarget();
        boolean targetLeavingWater = target != null && !target.isInWater() && target.getY() >= this.getY() - 0.5D;
        boolean pathLeavingWater = wantedY > this.getY() + 0.6D;
        if (!targetLeavingWater && !pathLeavingWater) {
            return false;
        }

        double dx = wantedX - this.getX();
        double dz = wantedZ - this.getZ();
        double horizontalDistanceSqr = dx * dx + dz * dz;
        if (horizontalDistanceSqr < 0.04D) {
            return false;
        }

        double horizontalDistance = Math.sqrt(horizontalDistanceSqr);
        double stepX = dx / horizontalDistance * 0.6D;
        double stepZ = dz / horizontalDistance * 0.6D;

        BlockPos frontPos = BlockPos.containing(this.getX() + stepX, this.getY() + 0.2D, this.getZ() + stepZ);
        BlockPos climbPos = frontPos.above();
        BlockPos headPos = climbPos.above();

        BlockState frontState = this.level().getBlockState(frontPos);
        BlockState climbState = this.level().getBlockState(climbPos);
        BlockState headState = this.level().getBlockState(headPos);

        boolean canStepOnto = !frontState.getCollisionShape(this.level(), frontPos).isEmpty()
                && climbState.getCollisionShape(this.level(), climbPos).isEmpty()
                && headState.getCollisionShape(this.level(), headPos).isEmpty();

        return canStepOnto || this.horizontalCollision;
    }

    public void travel(Vec3 p_32394_) {
       if (this.isEffectiveAi() && this.shouldUseWaterMovement()) {
          this.moveRelative(0.01F, p_32394_);
          this.move(MoverType.SELF, this.getDeltaMovement());
          this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
          this.setPose(Pose.SWIMMING);
       } else {
    	   if (this.getPose() == Pose.SWIMMING) this.setPose(Pose.STANDING);
          super.travel(p_32394_);
       }

    }

    public void updateSwimming() {
       if (!this.level().isClientSide) {
          if (this.isEffectiveAi() && this.shouldUseWaterMovement()) {
             this.navigation = this.waterNavigation;
             this.setSwimming(true);
          } else {
             this.navigation = this.groundNavigation;
             this.setSwimming(false);
          }
       }

    }

    static class HumanMoveControl extends MoveControl {
       private final Human human;

       public HumanMoveControl(Human p_32433_) {
          super(p_32433_);
          this.human = p_32433_;
       }

        public void tick() {
           LivingEntity livingentity = this.human.getTarget();
            if (this.human.shouldUseWaterMovement()) {
              if (this.human.shouldJumpOutOfWaterToward(this.wantedX, this.wantedY, this.wantedZ)) {
                 this.human.getJumpControl().jump();
                 this.human.setDeltaMovement(this.human.getDeltaMovement().add(0.0D, 0.18D, 0.0D));
              }
              if (this.human.shouldCatchBreath) {
                 this.human.setDeltaMovement(this.human.getDeltaMovement().add(0.0D, 0.02D, 0.0D));
              } else if (livingentity != null && livingentity.getY() > this.human.getY()) {
                 this.human.setDeltaMovement(this.human.getDeltaMovement().add(0.0D, 0.002D, 0.0D));
              }

             if (this.operation != MoveControl.Operation.MOVE_TO || this.human.getNavigation().isDone()) {
                this.human.setSpeed(0.0F);
                return;
             }

             double d0 = this.wantedX - this.human.getX();
             double d1 = this.wantedY - this.human.getY();
             double d2 = this.wantedZ - this.human.getZ();
             double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
             d1 /= d3;
             float f = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
             this.human.setYRot(this.rotlerp(this.human.getYRot(), f, 90.0F));
             this.human.yBodyRot = this.human.getYRot();
             float f1 = (float)(this.speedModifier * this.human.getAttributeValue(Attributes.MOVEMENT_SPEED)) * 2;
             float f2 = Mth.lerp(0.125F, this.human.getSpeed(), f1);
             this.human.setSpeed(f2);
             this.human.setDeltaMovement(this.human.getDeltaMovement().add((double)f2 * d0 * 0.005D, (double)f2 * d1 * 0.1D, (double)f2 * d2 * 0.005D));
          } else {
             if (!this.human.onGround()) {
                this.human.setDeltaMovement(this.human.getDeltaMovement().add(0.0D, -0.008D, 0.0D));
             }

             super.tick();
          }

       }
    }

}

package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.entities.ChestExtension;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LookForChestGoal extends Goal {
    private record ChestKey(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final Map<ChestKey, UUID> RESERVED_CHESTS = new HashMap<>();
    private static final Map<ChestKey, Long> RESERVED_UNTIL = new HashMap<>();
    private static final Map<ChestKey, Long> CHEST_COOLDOWN_UNTIL = new HashMap<>();

    protected final Human mob;
    private final double speedModifier;
    @Nullable
    protected BlockPos pos = UNREACHABLE;
    protected int timer = 0;
    private boolean chestOpened;
    private static final float CHEST_OPEN_CHANCE = 0.5F;

    public static final BlockPos UNREACHABLE = new BlockPos(0, -9999, 0);

    public LookForChestGoal(Human pMob, double pSpeedModifier) {
        this.mob = pMob;
        this.speedModifier = pSpeedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        cleanupExpiredState();
        if (this.mob.lookForChestCooldown > 0) {
            return false;
        } else if (this.mob.getTarget() != null || this.mob.isSleeping() || this.mob.isFleeing) {
            return false;
        }

        if (this.pos != UNREACHABLE) {
            return true;
        }

        if (this.mob.getRandom().nextFloat() >= CHEST_OPEN_CHANCE) {
            this.mob.lookForChestCooldown = this.mob.getRandom().nextInt(20 * 15, 20 * 45);
            return false;
        }

        for (int x = -20; x < 20; x++) {
            for (int y = -5; y < 5; y++) {
                for (int z = -20; z < 20; z++) {
                    BlockPos chestPos = this.mob.blockPosition().offset(x, y, z);
                    if (this.mob.level().getBlockState(chestPos).getBlock() instanceof ChestBlock && canClaimChest(chestPos)) {
                        this.pos = chestPos;
                        this.timer = this.mob.getRandom().nextInt(20 * 5, 20 * 15);
                        return true;
                    }
                }
            }
        }

        this.mob.lookForChestCooldown = this.mob.getRandom().nextInt(20 * 30, 20 * 90);
        return false;
    }

    public boolean canContinueToUse() {
        if (this.pos == UNREACHABLE || this.mob.getTarget() != null || this.mob.isSleeping() || this.mob.isFleeing) {
            return false;
        }

        if (this.timer <= 0) {
            return false;
        }

        if (this.chestOpened) {
            return true;
        }

        if (this.mob.blockPosition().distSqr(this.pos) < 5D) {
            return true;
        }

        return this.mob.blockPosition().distSqr(this.pos) < 1000D;
    }

    public void start() {
        this.chestOpened = false;
        reserveChest();
    }

    public void stop() {
        if (this.chestOpened && this.mob.level().getBlockEntity(this.pos) instanceof ChestExtension ch) {
            ch.hostileHumans$setForcedOpen(false);
        }
        releaseChest(this.timer <= 0);
        this.chestOpened = false;
        this.mob.getNavigation().stop();
        if (this.timer <= 0) {
            this.pos = UNREACHABLE;
        }
    }

    public void tick() {
        if (this.chestOpened) {
            this.mob.getNavigation().stop();
            if (this.mob.level().getBlockEntity(this.pos) instanceof ChestExtension ch) {
                ch.hostileHumans$setForcedOpen(true);
            }

            this.timer--;
            if (this.timer <= 0) {
                if (this.mob.level().getBlockEntity(this.pos) instanceof ChestExtension ch) {
                    ch.hostileHumans$setForcedOpen(false);
                }
                this.chestOpened = false;
                this.pos = UNREACHABLE;
                this.mob.lookForChestCooldown = this.mob.getRandom().nextInt(20 * 60 * 5, 20 * 60 * 10);
            }
            return;
        }

        if (this.mob.blockPosition().distSqr(this.pos) < 5D) {
            this.mob.getNavigation().stop();
            if (this.mob.level().getBlockEntity(this.pos) instanceof ChestExtension ch) {
                this.chestOpened = true;
                ch.hostileHumans$setForcedOpen(true);
            } else {
                this.pos = UNREACHABLE;
                this.mob.lookForChestCooldown = this.mob.getRandom().nextInt(20 * 10, 20 * 20);
            }
        } else {
            this.mob.getNavigation().moveTo(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.speedModifier);
        }
    }

    private boolean canClaimChest(BlockPos chestPos) {
        ChestKey key = chestKey(chestPos);
        long gameTime = this.mob.level().getGameTime();
        Long cooldownUntil = CHEST_COOLDOWN_UNTIL.get(key);
        if (cooldownUntil != null && cooldownUntil > gameTime) {
            return false;
        }

        UUID reservedBy = RESERVED_CHESTS.get(key);
        Long reservedUntil = RESERVED_UNTIL.get(key);
        return reservedBy == null || reservedUntil == null || reservedUntil <= gameTime || reservedBy.equals(this.mob.getUUID());
    }

    private void reserveChest() {
        if (this.pos == UNREACHABLE) {
            return;
        }

        ChestKey key = chestKey(this.pos);
        long reserveUntil = this.mob.level().getGameTime() + this.timer + 40L;
        RESERVED_CHESTS.put(key, this.mob.getUUID());
        RESERVED_UNTIL.put(key, reserveUntil);
    }

    private void releaseChest(boolean addCooldown) {
        if (this.pos == UNREACHABLE) {
            return;
        }

        ChestKey key = chestKey(this.pos);
        UUID reservedBy = RESERVED_CHESTS.get(key);
        if (reservedBy != null && reservedBy.equals(this.mob.getUUID())) {
            RESERVED_CHESTS.remove(key);
            RESERVED_UNTIL.remove(key);
            if (addCooldown) {
                CHEST_COOLDOWN_UNTIL.put(key, this.mob.level().getGameTime() + this.mob.getRandom().nextInt(20 * 60 * 3, 20 * 60 * 6));
            }
        }
    }

    private void cleanupExpiredState() {
        long gameTime = this.mob.level().getGameTime();
        RESERVED_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
        RESERVED_CHESTS.entrySet().removeIf(entry -> !RESERVED_UNTIL.containsKey(entry.getKey()));
        CHEST_COOLDOWN_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
    }

    private ChestKey chestKey(BlockPos chestPos) {
        return new ChestKey(this.mob.level().dimension(), new BlockPos(chestPos.getX(), chestPos.getY(), chestPos.getZ()));
    }
}

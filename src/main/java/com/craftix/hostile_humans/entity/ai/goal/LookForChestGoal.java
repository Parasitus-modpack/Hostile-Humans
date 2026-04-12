package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.ChestExtension;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.ChestBlock;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class LookForChestGoal extends Goal {
    protected final Human mob;
    private final double speedModifier;
    @Nullable
    protected BlockPos pos = UNREACHABLE;
    protected int timer = 0;
    private boolean chestOpened;

    public static final BlockPos UNREACHABLE = new BlockPos(0, -9999, 0);

    public LookForChestGoal(Human pMob, double pSpeedModifier) {
        this.mob = pMob;
        this.speedModifier = pSpeedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        if (this.mob.lookForChestCooldown > 0) {
            return false;
        } else if (this.mob.getTarget() != null || this.mob.isSleeping() || this.mob.isFleeing) {
            return false;
        }

        if (this.pos != UNREACHABLE) {
            return true;
        }

        if (this.mob.getRandom().nextFloat() >= Config.chestOpenChance.get()) {
            this.mob.lookForChestCooldown = this.mob.getRandom().nextInt(20 * 30, 20 * 90);
            return false;
        }

        for (int x = -20; x < 20; x++) {
            for (int y = -5; y < 5; y++) {
                for (int z = -20; z < 20; z++) {
                    BlockPos chestPos = this.mob.blockPosition().offset(x, y, z);
                    if (this.mob.level().getBlockState(chestPos).getBlock() instanceof ChestBlock) {
                        this.pos = chestPos;
                        this.timer = this.mob.getRandom().nextInt(20 * 5, 20 * 15);
                        return true;
                    }
                }
            }
        }

        this.mob.lookForChestCooldown = 20 * 60 * 20;
        return false;
    }

    public boolean canContinueToUse() {
        if (this.pos == UNREACHABLE || this.mob.getTarget() != null || this.mob.isSleeping() || this.mob.isFleeing) {
            return false;
        }

        if (this.mob.blockPosition().distSqr(this.pos) < 5D) {
            return this.timer > 0;
        }

        return this.mob.blockPosition().distSqr(this.pos) < 1000D;
    }

    public void start() {
        this.chestOpened = false;
    }

    public void stop() {
        if (this.chestOpened && this.mob.level().getBlockEntity(this.pos) instanceof ChestExtension ch) {
            ch.openersCounter().decrementOpeners(null, this.mob.level(), this.pos, this.mob.level().getBlockState(this.pos));
        }
        this.chestOpened = false;
        this.mob.getNavigation().stop();
        if (this.timer <= 0) {
            this.pos = UNREACHABLE;
        }
    }

    public void tick() {
        if (this.mob.blockPosition().distSqr(this.pos) < 5D) {
            this.mob.getNavigation().stop();
            if (this.mob.level().getBlockEntity(this.pos) instanceof ChestExtension ch) {
                if (!this.chestOpened) {
                    ch.openersCounter().incrementOpeners(null, this.mob.level(), this.pos, this.mob.level().getBlockState(this.pos));
                    this.chestOpened = true;
                }
                this.timer--;
                if (this.timer <= 0) {
                    ch.openersCounter().decrementOpeners(null, this.mob.level(), this.pos, this.mob.level().getBlockState(this.pos));
                    this.chestOpened = false;
                    this.pos = UNREACHABLE;
                    this.mob.lookForChestCooldown = this.mob.getRandom().nextInt(20 * 60 * 5, 20 * 60 * 10);
                }
            }
        } else {
            this.mob.getNavigation().moveTo(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.speedModifier);
        }
    }
}

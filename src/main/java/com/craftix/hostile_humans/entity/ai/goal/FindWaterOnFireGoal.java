package com.craftix.hostile_humans.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class FindWaterOnFireGoal extends Goal {
    private static final int SEARCH_RADIUS = 24;
    private static final int SEARCH_HEIGHT = 8;
    private static final int SEARCH_INTERVAL_TICKS = 10;

    protected final PathfinderMob mob;
    private final double speedModifier;
    private final Level level;
    private double wantedX;
    private double wantedY;
    private double wantedZ;
    private int nextSearchTick;

    public FindWaterOnFireGoal(PathfinderMob p_25221_, double p_25222_) {
        this.mob = p_25221_;
        this.speedModifier = p_25222_;
        this.level = p_25221_.level();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        if (mob.getEffect(MobEffects.FIRE_RESISTANCE) != null) return false;
        if (!this.mob.isOnFire()) {
            return false;
        }
        if (this.mob.getTarget() != null
                && !(this.mob.getRemainingFireTicks() > 100
                && this.mob.getHealth() < this.mob.getMaxHealth() * 0.6F)) {
            return false;
        }
        return this.setWantedPos();
    }

    protected boolean setWantedPos() {
        Vec3 hidePos = this.getHidePos();
        if (hidePos == null) {
            return false;
        } else {
            this.wantedX = hidePos.x;
            this.wantedY = hidePos.y;
            this.wantedZ = hidePos.z;
            return true;
        }
    }

    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    public void start() {
        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
    }

    @Nullable
    protected Vec3 getHidePos() {

        if (this.mob.tickCount < this.nextSearchTick) {
            return null;
        }
        this.nextSearchTick = this.mob.tickCount + SEARCH_INTERVAL_TICKS;

        return BlockPos.findClosestMatch(this.mob.blockPosition(), SEARCH_RADIUS, SEARCH_HEIGHT,
                        pos -> level.getBlockState(pos).getBlock() == Blocks.WATER)
                .map(Vec3::atBottomCenterOf)
                .orElse(null);
    }
}

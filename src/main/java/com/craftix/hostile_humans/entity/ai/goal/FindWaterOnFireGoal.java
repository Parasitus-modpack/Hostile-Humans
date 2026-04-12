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
    protected final PathfinderMob mob;
    private final double speedModifier;
    private final Level level;
    private double wantedX;
    private double wantedY;
    private double wantedZ;

    public FindWaterOnFireGoal(PathfinderMob p_25221_, double p_25222_) {
        this.mob = p_25221_;
        this.speedModifier = p_25222_;
        this.level = p_25221_.level();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        if (mob.getEffect(MobEffects.FIRE_RESISTANCE) != null) return false;
        if (this.mob.getTarget() != null) {
            return false;
        } else if (!this.mob.isOnFire()) {
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

        BlockPos blockpos = this.mob.blockPosition();

        int radius = 10;
        for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.getX() - radius, blockpos.getY() - radius / 3, blockpos.getZ() - radius, blockpos.getX() + radius, blockpos.getY() + radius / 3, blockpos.getZ() + radius)) {
            if (level.getBlockState(blockpos1).getBlock() == Blocks.WATER) {
                return Vec3.atBottomCenterOf(blockpos1);
            }
        }
        return null;
    }
}

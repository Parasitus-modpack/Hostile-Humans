package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.entities.Human;
import java.util.EnumSet;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class HumanFloatGoal extends Goal {

    private final Human mob;

    public HumanFloatGoal(Human p_25230_) {
        this.mob = p_25230_;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP));
        p_25230_.getNavigation().setCanFloat(true);
    }

    public boolean canUse() {
        if (this.mob.wantsToSwim()) return false;
        if (this.mob.isInShallowWater()) return false;
        return this.mob.isEyeInFluid(FluidTags.WATER) || this.mob.isInLava();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.mob.getRandom().nextFloat() < 0.8F) {
            Vec3 movement = this.mob.getDeltaMovement();
            double desiredY = this.mob.getFluidSurfaceY() - 1.4D;
            double vertical = Mth.clamp((desiredY - this.mob.getY()) * 0.25D, -0.08D, 0.10D);
            double newY = movement.y > vertical ? Math.max(vertical, movement.y * 0.5D) : vertical;
            this.mob.setDeltaMovement(movement.x, newY, movement.z);
        }
    }
}

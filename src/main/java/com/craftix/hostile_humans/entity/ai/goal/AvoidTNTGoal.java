package com.craftix.hostile_humans.entity.ai.goal;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class AvoidTNTGoal extends Goal {
    protected final PathfinderMob mob;
    protected final float maxDist;
    protected final PathNavigation pathNav;
    protected final Predicate<LivingEntity> avoidPredicate;
    protected final Predicate<LivingEntity> predicateOnAvoidEntity;
    private final double walkSpeedModifier;
    private final double sprintSpeedModifier;
    @Nullable
    protected PrimedTnt toAvoid;
    @Nullable
    protected Path path;

    public AvoidTNTGoal(PathfinderMob p_25027_, float p_25029_, double p_25030_, double p_25031_) {
        this(p_25027_, (p_25052_) -> {
            return true;
        }, p_25029_, p_25030_, p_25031_, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
    }

    public AvoidTNTGoal(PathfinderMob p_25040_, Predicate<LivingEntity> p_25042_, float p_25043_, double p_25044_, double p_25045_, Predicate<LivingEntity> p_25046_) {
        this.mob = p_25040_;

        this.avoidPredicate = p_25042_;
        this.maxDist = p_25043_;
        this.walkSpeedModifier = p_25044_;
        this.sprintSpeedModifier = p_25045_;
        this.predicateOnAvoidEntity = p_25046_;
        this.pathNav = p_25040_.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Nullable
    PrimedTnt getNearestEntity(List<PrimedTnt> p_45983_, double p_45986_, double p_45987_, double p_45988_) {
        double d0 = -1.0D;
        PrimedTnt t = null;

        for (PrimedTnt t1 : p_45983_) {

            double d1 = t1.distanceToSqr(p_45986_, p_45987_, p_45988_);
            if (d0 == -1.0D || d1 < d0) {
                d0 = d1;
                t = t1;
            }
        }

        return t;
    }

    public boolean canUse() {
        this.toAvoid = getNearestEntity(mob.level().getEntitiesOfClass(PrimedTnt.class, this.mob.getBoundingBox().inflate((double) this.maxDist, 3.0D, (double) this.maxDist)), mob.getX(), mob.getY(), mob.getZ());
        if (this.toAvoid == null) {
            return false;
        } else {
            Vec3 vec3 = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.toAvoid.position());
            if (vec3 == null) {
                return false;
            } else if (this.toAvoid.distanceToSqr(vec3.x, vec3.y, vec3.z) < this.toAvoid.distanceToSqr(this.mob)) {
                return false;
            } else {
                this.path = this.pathNav.createPath(vec3.x, vec3.y, vec3.z, 0);
                return this.path != null;
            }
        }
    }

    public boolean canContinueToUse() {
        return !this.pathNav.isDone();
    }

    public void start() {
        this.pathNav.moveTo(this.path, this.walkSpeedModifier);
    }

    public void stop() {
        this.toAvoid = null;
    }

    public void tick() {
        if (this.mob.distanceToSqr(this.toAvoid) < 49.0D) {
            this.mob.getNavigation().setSpeedModifier(this.sprintSpeedModifier);
        } else {
            this.mob.getNavigation().setSpeedModifier(this.walkSpeedModifier);
        }
    }
}

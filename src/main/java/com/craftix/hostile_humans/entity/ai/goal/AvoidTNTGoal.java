package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.ai.control.HumanEntityWalkControl;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
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
    protected final Predicate<LivingEntity> avoidPredicate;
    protected final Predicate<LivingEntity> predicateOnAvoidEntity;
    private final double walkSpeedModifier;
    private final double sprintSpeedModifier;
    @Nullable
    protected PrimedTnt toAvoid;
    @Nullable
    protected Path path;
    private boolean rawFlee;

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
        }
        Vec3 vec3 = null;
        for (int attempt = 0; attempt < 3 && vec3 == null; attempt++) {
            Vec3 candidate = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.toAvoid.position());
            if (candidate != null && this.toAvoid.distanceToSqr(candidate.x, candidate.y, candidate.z) >= this.toAvoid.distanceToSqr(this.mob)) {
                vec3 = candidate;
            }
        }
        if (vec3 == null) {
            vec3 = this.computeDirectAwayPos();
        }
        this.path = this.mob.getNavigation().createPath(vec3.x, vec3.y, vec3.z, 0);
        if (this.path != null) {
            this.rawFlee = false;
            return true;
        }
        if (this.mob.getMoveControl() instanceof HumanEntityWalkControl) {
            this.rawFlee = true;
            return true;
        }
        return false;
    }

    private Vec3 computeDirectAwayPos() {
        double dx = this.mob.getX() - this.toAvoid.getX();
        double dz = this.mob.getZ() - this.toAvoid.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-4D) {
            dx = 1.0D;
            dz = 0.0D;
            horizontal = 1.0D;
        }
        return new Vec3(this.mob.getX() + dx / horizontal * 10.0D, this.mob.getY(), this.mob.getZ() + dz / horizontal * 10.0D);
    }

    public boolean canContinueToUse() {
        if (this.rawFlee) {
            return this.toAvoid != null && this.toAvoid.isAlive() && this.mob.isAlive()
                    && this.mob.distanceToSqr(this.toAvoid) < (double) ((this.maxDist + 4.0F) * (this.maxDist + 4.0F));
        }
        return this.toAvoid != null && this.toAvoid.isAlive() && !this.mob.getNavigation().isDone();
    }

    public void start() {
        if (this.mob instanceof Human fleeingHuman) {
            fleeingHuman.isFleeing = true;
        }
        if (this.rawFlee) {
            this.requestRawFlee(this.sprintSpeedModifier);
        } else {
            this.mob.getNavigation().moveTo(this.path, this.walkSpeedModifier);
        }
    }

    public void stop() {
        if (this.mob instanceof Human fleeingHuman) {
            fleeingHuman.isFleeing = false;
        }
        this.toAvoid = null;
        this.path = null;
    }

    public void tick() {
        if (this.rawFlee) {
            if (this.toAvoid != null) {
                this.requestRawFlee(this.sprintSpeedModifier);
            }
            return;
        }
        if (this.toAvoid != null) {
            if (this.mob.distanceToSqr(this.toAvoid) < 49.0D) {
                this.mob.getNavigation().setSpeedModifier(this.sprintSpeedModifier);
            } else {
                this.mob.getNavigation().setSpeedModifier(this.walkSpeedModifier);
            }
        }
    }

    private void requestRawFlee(double speed) {
        if (this.toAvoid == null) {
            return;
        }
        if (this.mob.getMoveControl() instanceof HumanEntityWalkControl humanControl) {
            Vec3 away = this.computeDirectAwayPos();
            humanControl.requestMoveTo(away.x, away.y, away.z, speed);
        }
    }
}

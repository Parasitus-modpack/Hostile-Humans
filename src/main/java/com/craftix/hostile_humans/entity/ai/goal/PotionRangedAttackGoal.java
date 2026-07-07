//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.PotionRangedAttackMob;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.SplashPotionItem;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class PotionRangedAttackGoal extends Goal {
    private final Mob mob;
    private final PotionRangedAttackMob rangedAttackMob;
    private final double speedModifier;
    private final int attackIntervalMin;
    private final int attackIntervalMax;
    private final float attackRadius;
    private final float attackRadiusSqr;

    @Nullable
    private LivingEntity target;
    private int attackTime;
    private int seeTime;

    public PotionRangedAttackGoal(PotionRangedAttackMob p_25768_, double p_25769_, int p_25770_, float p_25771_) {
        this(p_25768_, p_25769_, p_25770_, p_25770_, p_25771_);
    }

    public PotionRangedAttackGoal(PotionRangedAttackMob p_25773_, double p_25774_, int p_25775_, int p_25776_, float p_25777_) {
        this.attackTime = -1;
        if (!(p_25773_ instanceof LivingEntity)) {
            throw new IllegalArgumentException("AttackGoal requires Mob implements PotionRangedAttackMob");
        } else {
            this.rangedAttackMob = p_25773_;
            this.mob = (Mob) p_25773_;
            this.speedModifier = p_25774_;
            this.attackIntervalMin = p_25775_;
            this.attackIntervalMax = p_25776_;
            this.attackRadius = p_25777_;
            this.attackRadiusSqr = p_25777_ * p_25777_;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }
    }

    public boolean canUse() {
        if (this.mob instanceof Human human && (human.isFleeing || human.isSleepingOrLyingDown())) {
            return false;
        }
        LivingEntity mobTarget = this.mob.getTarget();
        if (mobTarget != null && mobTarget.isAlive()) {
            if (mob.getMainHandItem().getItem() instanceof SplashPotionItem || mob.getOffhandItem().getItem() instanceof SplashPotionItem) {
                this.target = mobTarget;
                return true;
            }
        }
        return false;
    }

    public boolean canContinueToUse() {
        if (this.mob instanceof Human human && (human.isFleeing || human.isSleepingOrLyingDown())) {
            return false;
        }
        return this.canUse() || !this.mob.getNavigation().isDone();
    }

    public void stop() {
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.target == null || (this.mob instanceof Human human && human.isSleepingOrLyingDown())) {
            return;
        }
        double distanceToSqr = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(this.target);
        if (hasLineOfSight) {
            ++this.seeTime;
        } else {
            this.seeTime = 0;
        }

        if (!(distanceToSqr > (double) this.attackRadiusSqr) && this.seeTime >= 5) {
            //  this.mob.getNavigation().stop();
        } else {
            //  this.mob.getNavigation().moveTo(this.target, this.speedModifier);
        }

        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        if (--this.attackTime == 0) {
            if (!hasLineOfSight) {
                return;
            }

            float v = (float) Math.sqrt(distanceToSqr) / this.attackRadius;
            float clamp = Mth.clamp(v, 0.1F, 1.0F);
            this.rangedAttackMob.performPotionRangedAttack(this.target, clamp);
            this.attackTime = Mth.floor(v * (float) (this.attackIntervalMax - this.attackIntervalMin) + (float) this.attackIntervalMin);
        } else if (this.attackTime < 0) {
            this.attackTime = Mth.floor(Mth.lerp(Math.sqrt(distanceToSqr) / (double) this.attackRadius, (double) this.attackIntervalMin, (double) this.attackIntervalMax));
        }
    }
}

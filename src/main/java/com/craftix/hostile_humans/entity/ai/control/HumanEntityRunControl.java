package com.craftix.hostile_humans.entity.ai.control;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class HumanEntityRunControl extends MoveControl {
    private final Human human;
    private int lungeCooldown = 0;
    private int airborneTicks = 0;
    private int postLungeRecoveryTicks = 0;
    private boolean isLunging = false;
    private float lockedLungeYaw;

    public HumanEntityRunControl(Mob mob) {
        super(mob);
        this.human = (Human) mob;
    }

    public boolean isLunging() {
        return this.isLunging;
    }

    public boolean shouldKeepControl() {
        return this.isLunging;
    }

    public boolean shouldPreferWalkControl() {
        return this.postLungeRecoveryTicks > 0;
    }

    @Override
    public void tick() {
        if (this.lungeCooldown > 0) {
            this.lungeCooldown--;
        }
        if (this.postLungeRecoveryTicks > 0) {
            this.postLungeRecoveryTicks--;
        }

        LivingEntity target = this.human.getTarget();
        if (target == null || this.human.isFleeing) {
            this.mob.setZza(0.0F);
            this.isLunging = false;
            this.airborneTicks = 0;
            return;
        }

        double lungeVelocity = Config.runLungeVelocity.get();

        this.faceTarget(target, 45.0F);

        if (this.isLunging) {
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.lockedLungeYaw, 90.0F));
            this.mob.yBodyRot = this.mob.getYRot();

            if (!this.mob.onGround()) {
                this.airborneTicks++;
            }

            if (this.human.distanceTo(target) < 2.5F || (this.airborneTicks > 0 && this.mob.onGround())) {
                stopLunge();
            }
            return;
        }

        if (this.mob.onGround() && this.lungeCooldown == 0) {
            boolean canLunge = this.human.distanceTo(target) >= 6.0F
                    && Config.runJump.get()
                    && !HumanUtil.isRangedWeapon(this.human.getMainHandItem())
                    && !HumanUtil.isTrident(this.human.getMainHandItem());

            if (canLunge) {
                this.mob.getJumpControl().jump();
                this.operation = Operation.JUMPING;

                Vec3 toTarget = new Vec3(target.getX() - this.mob.getX(), 0.0D, target.getZ() - this.mob.getZ());
                if (toTarget.lengthSqr() > 1.0E-6D) {
                    Vec3 dir = toTarget.normalize().scale(lungeVelocity);
                    this.lockedLungeYaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0D);
                    this.mob.setYRot(this.lockedLungeYaw);
                    this.mob.yBodyRot = this.mob.getYRot();
                    this.mob.setDeltaMovement(dir.x, Math.max(this.mob.getDeltaMovement().y, 0.42D), dir.z);
                }

                this.isLunging = true;
                this.airborneTicks = 0;
                this.lungeCooldown = 12;
                this.human.onPlayerJumpCoolDown = 6;
                return;
            }
        }

        float speed = this.speedModifier > 1.0E-4D
                ? (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED))
                : (float) this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        this.mob.setSpeed(speed);
        this.mob.setZza(1.0F);
    }

    private void stopLunge() {
        Vec3 current = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(current.x * 0.2D, current.y, current.z * 0.2D);
        this.isLunging = false;
        this.airborneTicks = 0;
        this.lungeCooldown = Math.max(this.lungeCooldown, 6);
        this.postLungeRecoveryTicks = 8;
        this.operation = Operation.WAIT;

        LivingEntity target = this.human.getTarget();
        if (target != null) {
            this.mob.getNavigation().moveTo(target, 1.0D);
        }
    }

    private void faceTarget(LivingEntity target, float maxTurn) {
        float targetYaw = (float) (Math.toDegrees(Math.atan2(target.getZ() - this.mob.getZ(), target.getX() - this.mob.getX())) - 90.0D);
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, maxTurn));
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.lookAt(target, 30.0F, 30.0F);
    }
}

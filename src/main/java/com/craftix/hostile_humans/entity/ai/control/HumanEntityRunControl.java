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
    private int airTicks = 0;
    private boolean isLunging = false;
    private float lockedLungeYaw;

    public HumanEntityRunControl(Mob mob) {
        super(mob);
        this.human = (Human) mob;
    }

    @Override
    public void tick() {
        if (this.lungeCooldown > 0) {
            this.lungeCooldown--;
        }

        LivingEntity target = this.human.getTarget();
        if (target == null || this.human.isFleeing) {
            this.mob.setZza(0.0F);
            this.isLunging = false;
            this.airTicks = 0;
            return;
        }

        double airAccel = Config.runAirAcceleration.get();
        double maxHorizontalSpeed = Config.runMaxHorizontalSpeed.get();
        double lungeVelocity = Config.runLungeVelocity.get();

        this.faceTarget(target, 45.0F);

        if (this.isLunging) {
            Vec3 toTarget = new Vec3(target.getX() - this.mob.getX(), 0.0D, target.getZ() - this.mob.getZ());
            if (toTarget.lengthSqr() > 1.0E-6D) {
                Vec3 dir = toTarget.normalize().scale(airAccel);
                Vec3 current = this.mob.getDeltaMovement();
                Vec3 horizontal = new Vec3(current.x + dir.x, 0.0D, current.z + dir.z);
                double maxHorizontalSpeedSqr = maxHorizontalSpeed * maxHorizontalSpeed;
                if (horizontal.lengthSqr() > maxHorizontalSpeedSqr) {
                    horizontal = horizontal.normalize().scale(maxHorizontalSpeed);
                }

                this.mob.setDeltaMovement(horizontal.x, current.y, horizontal.z);
            }

            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.lockedLungeYaw, 90.0F));
            this.mob.yBodyRot = this.mob.getYRot();

            this.airTicks++;
            if (this.mob.onGround()) {
                this.isLunging = false;
                this.airTicks = 0;
                this.mob.getNavigation().recomputePath();
            }
            return;
        }

        if (this.mob.onGround() && this.lungeCooldown == 0) {
            boolean canLunge = this.human.distanceTo(target) >= 6.0F
                    && Config.runJump.get()
                    && !HumanUtil.isRangedWeapon(this.human.getMainHandItem())
                    && !HumanUtil.isTrident(this.human.getMainHandItem())
                    && this.mob.getNavigation().isInProgress();

            if (canLunge) {
                this.mob.getJumpControl().jump();
                this.operation = Operation.JUMPING;

                Vec3 toTarget = new Vec3(target.getX() - this.mob.getX(), 0.0D, target.getZ() - this.mob.getZ());
                if (toTarget.lengthSqr() > 1.0E-6D) {
                    Vec3 dir = toTarget.normalize().scale(lungeVelocity);
                    this.mob.setDeltaMovement(dir.x, this.mob.getDeltaMovement().y, dir.z);
                    this.lockedLungeYaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0D);
                    this.mob.setYRot(this.lockedLungeYaw);
                    this.mob.yBodyRot = this.mob.getYRot();
                }

                this.isLunging = true;
                this.airTicks = 0;
                this.lungeCooldown = 12;
                this.human.onPlayerJumpCoolDown = 6;
                return;
            }
        }

        this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
        this.mob.setZza(1.0F);
    }

    private void faceTarget(LivingEntity target, float maxTurn) {
        float targetYaw = (float) (Math.toDegrees(Math.atan2(target.getZ() - this.mob.getZ(), target.getX() - this.mob.getX())) - 90.0D);
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, maxTurn));
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.lookAt(target, 30.0F, 30.0F);
    }
}

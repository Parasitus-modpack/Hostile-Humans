package com.craftix.hostile_humans.entity.ai.control;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import static com.craftix.hostile_humans.HumanUtil.isLookingAtTarget;


public class HumanEntityWalkControl extends MoveControl {

    int skipTicks;

    Human human;

    public HumanEntityWalkControl(Mob mob) {
        super(mob);

        human = (Human) mob;
    }

    @Override
    public void tick() {
        if (skipTicks > 0) {
            skipTicks--;
            if (mob.getTarget() != null)
                mob.lookAt(mob.getTarget(), 0, 0);
            return;
        }

        if (mob.onGround() && skipTicks == 0) {
            skipTicks = -1;
            mob.getNavigation().timeLastRecompute = 0;
            mob.getNavigation().recomputePath();

            if (mob.getTarget() != null)
                mob.lookAt(mob.getTarget(), 0, 0);
            return;
        }

        float f9;
        if (this.operation == MoveControl.Operation.STRAFE) {
            float f = (float) this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            float f1 = (float) this.speedModifier * f;
            float f2 = this.strafeForwards;
            float f3 = this.strafeRight;
            float f4 = Mth.sqrt(f2 * f2 + f3 * f3);
            if (f4 < 1.0F) {
                f4 = 1.0F;
            }

            f4 = f1 / f4;
            f2 *= f4;
            f3 *= f4;
            float f5 = Mth.sin(this.mob.getYRot() * 0.017453292F);
            float f6 = Mth.cos(this.mob.getYRot() * 0.017453292F);
            float f7 = f2 * f6 - f3 * f5;
            f9 = f3 * f6 + f2 * f5;
            if (!this.isWalkable(f7, f9)) {
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;
            }

            this.mob.setSpeed(f1);
            this.mob.setZza(this.strafeForwards);
            this.mob.setXxa(this.strafeRight);
            this.operation = MoveControl.Operation.WAIT;
        } else if (this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.WAIT;
            double d0 = this.wantedX - this.mob.getX();
            double d1 = this.wantedZ - this.mob.getZ();
            double d2 = this.wantedY - this.mob.getY();
            double d3 = d0 * d0 + d2 * d2 + d1 * d1;
            if (d3 < 2.500000277905201E-7) {
                this.mob.setZza(0.0F);
                return;
            }

            f9 = (float) (Mth.atan2(d1, d0) * 57.2957763671875) - 90.0F;
            if (!human.isFleeing || human.onGround())
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f9, 90.0F));
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            BlockPos blockpos = this.mob.blockPosition();
            BlockState blockstate = this.mob.level().getBlockState(blockpos);
            VoxelShape voxelshape = blockstate.getCollisionShape(this.mob.level(), blockpos);
            if (d2 > (double) this.mob.getStepHeight() && d0 * d0 + d1 * d1 < (double) Math.max(1.0F, this.mob.getBbWidth()) || !voxelshape.isEmpty() && this.mob.getY() < voxelshape.max(Direction.Axis.Y) + (double) blockpos.getY() && !blockstate.is(BlockTags.DOORS) && !blockstate.is(BlockTags.FENCES)) {
                this.mob.getJumpControl().jump();
                this.operation = MoveControl.Operation.JUMPING;
            }

            if (Config.runJump.get()) {
                if (human.onPlayerJumpCoolDown == 0) {
                    if (human.isFleeing && human.getRandom().nextFloat() < 0.1 && human.toAvoid != null) {
                        if (mob.onGround()) {
                            this.mob.getJumpControl().jump();
                            this.operation = MoveControl.Operation.JUMPING;

                            addVelocityToMobTowardsPosition(mob, human.toAvoid.getX(), human.toAvoid.getY(), human.toAvoid.getZ(), -1.0);
                            mob.setYRot((float) Math.toDegrees(Math.atan2(mob.getDeltaMovement().z, mob.getDeltaMovement().x)) - 90);
                            //   mob.getNavigation().recomputePath();
                        }
                    } else if (!human.isFleeing && human.getTarget() != null && mob.distanceTo(human.getTarget()) >= 5 && (isLookingAtTarget(mob, human.getTarget()))) {
                        if (mob.onGround()) {
                            // no jumping to player if holding ranged weapon
                            if (!HumanUtil.isRangedWeapon(human.getMainHandItem()) && !HumanUtil.isTrident(human.getMainHandItem())) {
                                this.mob.getJumpControl().jump();
                                this.operation = MoveControl.Operation.JUMPING;

                                addVelocityToMobTowardsPosition(mob, human.getTarget().getX(), human.getTarget().getY(), human.getTarget().getZ(), 0.8);
                                mob.lookAt(human.getTarget(), 0, 0);

                                mob.getNavigation().recomputePath();
                                skipTicks = 1;
                            }
                        }
                    }
                }
            }

            // simulate crit
            if (Config.attackJump.get()) {
                if (human.getRandom().nextFloat() < 0.1f
                        && human.onGround()
                        && human.getTarget() != null
                        && human.getTarget().distanceTo(human) < 2
                        && HumanUtil.isMeleeWeapon(human.getMainHandItem())) {
                    this.mob.getJumpControl().jump();
                }
            }
        } else if (this.operation == MoveControl.Operation.JUMPING) {
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (this.mob.onGround()) {
                this.operation = MoveControl.Operation.WAIT;
            }
        } else {
            this.mob.setZza(0.0F);
        }
    }

    private void addVelocityToMobTowardsPosition(LivingEntity entity, double x, double y, double z, double speed) {
        double d0 = x - entity.getX();
        double d1 = y - entity.getY();
        double d2 = z - entity.getZ();
        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
        entity.setDeltaMovement(d0 / d3 * speed, 0, d2 / d3 * speed);
    }
}

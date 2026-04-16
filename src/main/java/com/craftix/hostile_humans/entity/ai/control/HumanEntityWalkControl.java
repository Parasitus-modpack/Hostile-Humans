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

public class HumanEntityWalkControl extends MoveControl {
    private final Human human;

    public HumanEntityWalkControl(Mob mob) {
        super(mob);
        this.human = (Human) mob;
    }

    @Override
    public void tick() {
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
            double dx = this.wantedX - this.mob.getX();
            double dz = this.wantedZ - this.mob.getZ();
            double dy = this.wantedY - this.mob.getY();
            double distSqr = dx * dx + dy * dy + dz * dz;
            if (distSqr < 2.500000277905201E-7) {
                this.mob.setZza(0.0F);
                return;
            }

            f9 = (float) (Mth.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f9, 30.0F));
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

            BlockPos blockpos = this.mob.blockPosition();
            BlockState blockstate = this.mob.level().getBlockState(blockpos);
            VoxelShape voxelshape = blockstate.getCollisionShape(this.mob.level(), blockpos);
            double horizontalDistSqr = dx * dx + dz * dz;
            if (dy > (double) this.mob.getStepHeight() && horizontalDistSqr < (double) Math.max(1.0F, this.mob.getBbWidth())
                    || !voxelshape.isEmpty() && this.mob.getY() < voxelshape.max(Direction.Axis.Y) + (double) blockpos.getY()
                    && !blockstate.is(BlockTags.DOORS)
                    && !blockstate.is(BlockTags.FENCES)) {
                this.mob.getJumpControl().jump();
                this.operation = MoveControl.Operation.JUMPING;
            }

            if (Config.attackJump.get()) {
                LivingEntity target = this.human.getTarget();
                if (target != null
                        && this.mob.onGround()
                        && target.distanceTo(this.human) < 2.0F
                        && HumanUtil.isMeleeWeapon(this.human.getMainHandItem())
                        && this.human.meleeFlurryHitsRemaining <= 0
                        && this.human.meleeFlurryDamageTicks <= 0
                        && this.human.getRandom().nextFloat() < 0.15F) {
                    this.mob.getJumpControl().jump();
                }
            }
        } else if (this.operation == MoveControl.Operation.JUMPING) {
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            LivingEntity target = this.human.getTarget();
            if (target != null) {
                float targetYaw = (float) (Mth.atan2(target.getZ() - this.mob.getZ(), target.getX() - this.mob.getX()) * 57.2957763671875D) - 90.0F;
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, 90.0F));
                this.mob.yBodyRot = this.mob.getYRot();
                this.mob.lookAt(target, 30.0F, 30.0F);
            }
            if (this.mob.onGround()) {
                this.operation = MoveControl.Operation.WAIT;
            }
        } else {
            LivingEntity target = this.human.getTarget();
            if (target != null && !this.human.isFleeing) {
                float targetYaw = (float) (Mth.atan2(target.getZ() - this.mob.getZ(), target.getX() - this.mob.getX()) * 57.2957763671875D) - 90.0F;
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, 30.0F));
                this.mob.yBodyRot = this.mob.getYRot();
                this.mob.lookAt(target, 30.0F, 30.0F);
                this.mob.setSpeed((float) this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.mob.setZza(1.0F);
            } else {
                this.mob.setZza(0.0F);
            }
        }
    }
}

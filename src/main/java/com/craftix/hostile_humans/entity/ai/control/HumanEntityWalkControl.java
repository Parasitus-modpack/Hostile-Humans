package com.craftix.hostile_humans.entity.ai.control;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.UUID;

public class HumanEntityWalkControl extends MoveControl {
    private static final AttributeModifier CHASE_BOOST_FAR = new AttributeModifier(UUID.fromString("7e0b9c42-51f6-4c9a-9d3e-8a1b2c3d4e5f"), "Human chase speed boost far", 0.28D, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier CHASE_BOOST_MID = new AttributeModifier(UUID.fromString("8f1cad53-62a7-4d0b-ae4f-9b2c3d4e5f60"), "Human chase speed boost mid", 0.14D, AttributeModifier.Operation.ADDITION);

    private final Human human;
    private PrimedTnt nearbyTnt;
    private int tntScanCooldown;
    private boolean tntFleeActive;

    public HumanEntityWalkControl(Mob mob) {
        super(mob);
        this.human = (Human) mob;
    }

    public void requestMoveTo(double x, double y, double z, double speedModifier) {
        this.setWantedPosition(x, y, z, speedModifier);
        this.operation = MoveControl.Operation.MOVE_TO;
    }

    @Override
    public void tick() {
        if (--this.tntScanCooldown <= 0) {
            this.tntScanCooldown = 4;
            PrimedTnt nearest = null;
            double nearestDistSqr = Double.MAX_VALUE;
            for (PrimedTnt tnt : this.mob.level().getEntitiesOfClass(PrimedTnt.class, this.mob.getBoundingBox().inflate(8.0D, 3.0D, 8.0D))) {
                double distSqr = tnt.distanceToSqr(this.mob);
                if (distSqr < nearestDistSqr) {
                    nearestDistSqr = distSqr;
                    nearest = tnt;
                }
            }
            this.nearbyTnt = nearest;
        }
        if (this.nearbyTnt != null) {
            this.human.isFleeing = true;
            this.tntFleeActive = true;
            double dx = this.mob.getX() - this.nearbyTnt.getX();
            double dz = this.mob.getZ() - this.nearbyTnt.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal < 1.0E-4D) {
                dx = 1.0D;
                dz = 0.0D;
                horizontal = 1.0D;
            }
            this.requestMoveTo(this.mob.getX() + dx / horizontal * 10.0D, this.mob.getY(), this.mob.getZ() + dz / horizontal * 10.0D, 1.3D);
        } else if (this.tntFleeActive) {
            this.tntFleeActive = false;
            this.human.isFleeing = false;
        }

        if (!this.mob.onGround() && !this.human.shouldUseWaterMovement() && !this.human.feetInWater()
                && this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.JUMPING;
        }

        if (this.mob.onGround()) {
            this.updateSprintState();
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

            LivingEntity target = this.human.getTarget();
            if (this.human.feetInWater()) {
                LivingEntity exitTarget = !this.human.isFleeing ? target : null;
                double exitGoalX = exitTarget != null ? exitTarget.getX() : this.wantedX;
                double exitGoalY = exitTarget != null ? exitTarget.getY() : this.wantedY;
                double exitGoalZ = exitTarget != null ? exitTarget.getZ() : this.wantedZ;
                double exitDx = exitGoalX - this.mob.getX();
                double exitDz = exitGoalZ - this.mob.getZ();
                double exitDist = Math.sqrt(exitDx * exitDx + exitDz * exitDz);
                if (exitDist <= 12.0D && this.human.shouldJumpOutOfWaterToward(exitGoalX, exitGoalY, exitGoalZ)) {
                    double exitY = this.human.hasDetectedExitY()
                            ? this.human.getDetectedExitY()
                            : Math.min(exitGoalY, this.mob.getY() + 2.0D);
                    double riseToLip = exitY - this.mob.getY();
                    double climbSpeed = riseToLip > 1.3D
                            ? Mth.clamp(riseToLip * 0.22D, 0.10D, 0.30D)
                            : Mth.clamp(riseToLip * 0.30D + 0.04D, 0.06D, 0.33D);
                    Vec3 movement = this.mob.getDeltaMovement();
                    if (exitDist > 1.0E-4D) {
                        this.mob.setDeltaMovement(exitDx / exitDist * 0.30D, climbSpeed, exitDz / exitDist * 0.30D);
                    } else {
                        this.mob.setDeltaMovement(movement.x, climbSpeed, movement.z);
                    }
                    return;
                }
            }
            if (this.human.shouldUseWaterMovement()) {
                LivingEntity pursueTarget = !this.human.isFleeing ? this.human.getTarget() : null;
                double goalX = pursueTarget != null ? pursueTarget.getX() : this.wantedX;
                double goalY = pursueTarget != null ? pursueTarget.getY() : this.wantedY;
                double goalZ = pursueTarget != null ? pursueTarget.getZ() : this.wantedZ;
                double chaseX = goalX - this.mob.getX();
                double chaseZ = goalZ - this.mob.getZ();
                double chaseLength = Math.sqrt(chaseX * chaseX + chaseZ * chaseZ);

                if (chaseLength > 1.0E-4D) {
                    float chaseYaw = (float) (Mth.atan2(chaseZ, chaseX) * 57.2957763671875D) - 90.0F;
                    this.mob.setYRot(this.rotlerp(this.mob.getYRot(), chaseYaw, 30.0F));
                    this.mob.yBodyRot = this.mob.getYRot();
                }

                Vec3 movement = this.mob.getDeltaMovement();
                if (this.human.prefersToFloat() && this.human.isEyeInFluid(FluidTags.WATER)) {
                    this.mob.setDeltaMovement(movement.x, 0.15D, movement.z);
                    return;
                }

                if (this.human.isEyeInFluid(FluidTags.WATER) && this.human.getAirSupply() <= this.human.getMaxAirSupply() / 2) {
                    this.mob.setDeltaMovement(movement.x, 0.10D, movement.z);
                    return;
                }

                double verticalSpeed;
                if (pursueTarget != null && !pursueTarget.isInWater()) {
                    verticalSpeed = Mth.clamp((goalY - 0.5D) - this.mob.getY(), -0.05D, 0.06D);
                } else if (pursueTarget != null && pursueTarget.getEyeY() >= this.human.getFluidSurfaceY() - 0.5D) {
                    verticalSpeed = Mth.clamp(this.human.getFluidSurfaceY() + 0.1D - this.mob.getY(), 0.0D, 0.10D);
                } else {
                    double heightOffset = pursueTarget != null ? 0.9D : 0.0D;
                    double desiredDelta = (goalY - heightOffset) - this.mob.getY();
                    double minDescent = desiredDelta < -2.0D ? -0.12D : -0.08D;
                    verticalSpeed = Mth.clamp(desiredDelta, minDescent, 0.08D);
                }
                this.mob.setDeltaMovement(movement.x, verticalSpeed, movement.z);
                return;
            }

            if (this.human.feetInWater() && !this.human.isInShallowWater() && !this.human.isEyeInFluid(FluidTags.WATER)) {
                Vec3 tread = this.mob.getDeltaMovement();
                double treadDelta = (this.human.getFluidSurfaceY() - 1.4D) - this.mob.getY();
                double treadVertical = Mth.clamp(treadDelta * 0.25D, -0.05D, 0.06D);
                double treadY = tread.y > treadVertical ? Math.max(treadVertical, tread.y * 0.5D) : treadVertical;
                this.mob.setDeltaMovement(tread.x, treadY, tread.z);
            }

            if (this.tryRunJump(target)) {
                return;
            }

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

            if (this.operation != MoveControl.Operation.JUMPING && Config.attackJump.get()) {
                if (target != null
                        && this.mob.onGround()
                        && !this.mob.isSprinting()
                        && this.human.onPlayerJumpCoolDown <= 0
                        && target.distanceTo(this.human) < 1.35F
                        && HumanUtil.isMeleeWeapon(this.human.getMainHandItem())
                        && this.human.getRandom().nextFloat() < 0.01F) {
                    this.mob.getJumpControl().jump();
                    this.operation = MoveControl.Operation.JUMPING;

                    double x = target.getX() - this.mob.getX();
                    double z = target.getZ() - this.mob.getZ();
                    double len = Math.sqrt(x * x + z * z);
                    if (len > 1.0E-4D) {
                        double boostX = x / len * 0.12D;
                        double boostZ = z / len * 0.12D;
                        this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(boostX, 0.0D, boostZ));
                    }

                    this.human.onPlayerJumpCoolDown = 80;
                }
            }
        } else if (this.operation == MoveControl.Operation.JUMPING) {
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (this.mob.onGround() || this.human.shouldUseWaterMovement()) {
                this.operation = MoveControl.Operation.WAIT;
            }
        } else {
            if (this.mob.onGround() && !this.human.isFleeing && this.human.healingAfterFleeTicks <= 0
                    && this.human.getNavigation().isDone()) {
                LivingEntity idleChase = this.human.getTarget();
                if (idleChase != null && idleChase.distanceTo(this.human) > 3.0F) {
                    this.requestMoveTo(idleChase.getX(), idleChase.getY(), idleChase.getZ(), 1.0D);
                }
            }
            this.mob.setZza(0.0F);
            this.mob.setXxa(0.0F);
        }
    }

    private boolean tryRunJump(LivingEntity target) {
        if (!Config.runJump.get() || target == null || this.human.isFleeing
                || this.human.healingAfterFleeTicks > 0
                || !this.mob.onGround()
                || !this.mob.isSprinting()
                || this.human.onPlayerJumpCoolDown > 0
                || target.distanceTo(this.human) < 4.0F
                || target.distanceTo(this.human) > 18.0F
                || Math.abs(target.getY() - this.human.getY()) > 2.5D
                || this.human.isHolding(HumanUtil::isRangedWeapon)
                || HumanUtil.isTrident(this.human.getMainHandItem())
                || !this.human.getSensing().hasLineOfSight(target)) {
            return false;
        }

        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        double horizontalLength = Math.sqrt(dx * dx + dz * dz);
        if (horizontalLength < 1.0E-4D) {
            return false;
        }

        if (!this.isJumpAlignedWithPath(dx, dz, horizontalLength)) {
            return false;
        }

        float targetYaw = (float) (Mth.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, 30.0F));
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.getJumpControl().jump();
        this.operation = MoveControl.Operation.JUMPING;

        Vec3 current = this.mob.getDeltaMovement();
        double desiredSpeed = Math.max(0.30D, Math.min(0.50D, 0.12D + horizontalLength * 0.055D));
        this.mob.setDeltaMovement(dx / horizontalLength * desiredSpeed, current.y, dz / horizontalLength * desiredSpeed);
        this.human.onPlayerJumpCoolDown = 6;
        return true;
    }

    private boolean isJumpAlignedWithPath(double dirX, double dirZ, double dirLength) {
        Path path = this.human.getNavigation().getPath();
        if (path == null || path.isDone() || dirLength < 1.0E-4D) {
            return true;
        }
        int count = path.getNodeCount();
        int index = path.getNextNodeIndex();
        if (index >= count) {
            return true;
        }
        int lastIndex = Math.min(index + 4, count - 1);
        Node from = path.getNode(index);
        Node to = path.getNode(lastIndex);

        double tangentX;
        double tangentZ;
        if (lastIndex > index) {
            tangentX = (double) to.x - (double) from.x;
            tangentZ = (double) to.z - (double) from.z;
        } else {
            tangentX = (double) from.x + 0.5D - this.mob.getX();
            tangentZ = (double) from.z + 0.5D - this.mob.getZ();
        }

        double tangentLength = Math.sqrt(tangentX * tangentX + tangentZ * tangentZ);
        if (tangentLength < 1.0E-4D) {
            return true;
        }

        double dot = (tangentX / tangentLength) * (dirX / dirLength)
                + (tangentZ / tangentLength) * (dirZ / dirLength);
        return dot > 0.65D;
    }

    private void updateSprintState() {
        LivingEntity target = this.human.getTarget();
        boolean shouldSprint = Config.runJump.get()
                && target != null
                && !this.human.isFleeing
                && this.human.healingAfterFleeTicks <= 0
                && !this.human.isUsingItem()
                && !this.human.isHolding(HumanUtil::isRangedWeapon)
                && !HumanUtil.isTrident(this.human.getMainHandItem())
                && this.human.distanceTo(target) >= 4.0F;
        this.human.setSprinting(shouldSprint);
        AttributeInstance speedAttribute = this.human.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeModifier wantedBoost = null;
        if (shouldSprint && target instanceof Player) {
            float chaseDistance = this.human.distanceTo(target);
            if (chaseDistance >= 10.0F) {
                wantedBoost = CHASE_BOOST_FAR;
            } else if (chaseDistance >= 6.0F) {
                wantedBoost = CHASE_BOOST_MID;
            }
        }
        if (wantedBoost != CHASE_BOOST_FAR && speedAttribute.hasModifier(CHASE_BOOST_FAR)) {
            speedAttribute.removeModifier(CHASE_BOOST_FAR);
        }
        if (wantedBoost != CHASE_BOOST_MID && speedAttribute.hasModifier(CHASE_BOOST_MID)) {
            speedAttribute.removeModifier(CHASE_BOOST_MID);
        }
        if (wantedBoost != null && !speedAttribute.hasModifier(wantedBoost)) {
            speedAttribute.addTransientModifier(wantedBoost);
        }
    }
}


package com.craftix.hostile_humans.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class DoorInteractHumanGoal extends Goal {
    protected final Mob mob;
    protected BlockPos doorPos = BlockPos.ZERO;
    protected boolean hasDoor;
    private boolean passed;
    private float doorOpenDirX;
    private float doorOpenDirZ;

    protected DoorInteractHumanGoal(Mob mob) {
        this.mob = mob;
        if (!GoalUtils.hasGroundPathNavigation(mob)) {
            throw new IllegalArgumentException("Unsupported mob type for DoorInteractHumanGoal");
        }
    }

    protected boolean isOpen() {
        if (!this.hasDoor) {
            return false;
        }

        BlockState blockState = this.mob.level().getBlockState(this.doorPos);
        if (!(blockState.getBlock() instanceof DoorBlock)) {
            this.hasDoor = false;
            return false;
        }

        if (blockState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            this.doorPos = this.doorPos.below();
            blockState = this.mob.level().getBlockState(this.doorPos);
            if (!(blockState.getBlock() instanceof DoorBlock)) {
                this.hasDoor = false;
                return false;
            }
        }

        return blockState.getValue(DoorBlock.OPEN);
    }

    protected void setOpen(boolean open) {
        if (!this.hasDoor) {
            return;
        }

        BlockState blockState = this.mob.level().getBlockState(this.doorPos);
        if (!(blockState.getBlock() instanceof DoorBlock doorBlock)) {
            this.hasDoor = false;
            return;
        }

        if (blockState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            this.doorPos = this.doorPos.below();
            blockState = this.mob.level().getBlockState(this.doorPos);
            if (!(blockState.getBlock() instanceof DoorBlock normalizedDoorBlock)) {
                this.hasDoor = false;
                return;
            }
            doorBlock = normalizedDoorBlock;
        }

        if (DoorBlock.isWoodenDoor(this.mob.level(), this.doorPos)) {
            doorBlock.setOpen(this.mob, this.mob.level(), blockState, this.doorPos, open);
            return;
        }

        if (open) {
            triggerNearbyMetalDoorOpeners(this.mob.level(), this.mob.getEyePosition(), this.doorPos);
        }
    }

    @Override
    public boolean canUse() {
        if (!(this.mob.getNavigation() instanceof GroundPathNavigation groundPathNavigation)) {
            return false;
        }
        Path path = groundPathNavigation.getPath();
        if (path != null && !path.isDone() && groundPathNavigation.canOpenDoors()) {
            for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); ++i) {
                Node node = path.getNode(i);
                this.doorPos = normalizeDoorPos(this.mob.level(), new BlockPos(node.x, node.y + 1, node.z));
                if (!(this.mob.distanceToSqr((double) this.doorPos.getX(), this.mob.getY(), (double) this.doorPos.getZ()) > 2.25D)) {
                    this.hasDoor = isDoor(this.mob.level(), this.doorPos);
                    if (this.hasDoor) {
                        return true;
                    }
                }
            }

            this.doorPos = normalizeDoorPos(this.mob.level(), this.mob.blockPosition().above());
            this.hasDoor = isDoor(this.mob.level(), this.doorPos);
            return this.hasDoor;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.passed;
    }

    @Override
    public void start() {
        this.passed = false;
        this.doorOpenDirX = (float) ((double) this.doorPos.getX() + 0.5D - this.mob.getX());
        this.doorOpenDirZ = (float) ((double) this.doorPos.getZ() + 0.5D - this.mob.getZ());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        float f = (float) ((double) this.doorPos.getX() + 0.5D - this.mob.getX());
        float f1 = (float) ((double) this.doorPos.getZ() + 0.5D - this.mob.getZ());
        float f2 = this.doorOpenDirX * f + this.doorOpenDirZ * f1;
        if (f2 < 0.0F) {
            this.passed = true;
        }
    }

    protected static boolean isDoor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock && state.is(BlockTags.DOORS);
    }

    protected static BlockPos normalizeDoorPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    private static void triggerNearbyMetalDoorOpeners(Level level, Vec3 from, BlockPos doorPos) {
        for (BlockPos nearbyPos : BlockPos.betweenClosed(doorPos.getX() - 2, doorPos.getY() - 2, doorPos.getZ() - 2,
                doorPos.getX() + 2, doorPos.getY() + 2, doorPos.getZ() + 2)) {
            BlockState state = level.getBlockState(nearbyPos);
            Block block = state.getBlock();
            if (!canMobSeeBlock(level, from, nearbyPos)) {
                continue;
            }

            if (block instanceof ButtonBlock buttonBlock) {
                buttonBlock.press(state, level, nearbyPos);
            } else if (block instanceof LeverBlock leverBlock) {
                leverBlock.pull(state, level, nearbyPos);
                level.gameEvent((Entity) null, GameEvent.BLOCK_ACTIVATE, nearbyPos);
            }
        }
    }

    private static boolean canMobSeeBlock(Level level, Vec3 from, BlockPos to) {
        return level.clip(new ClipContext(
                from,
                new Vec3(to.getX() + 0.5D, to.getY() + 0.5D, to.getZ() + 0.5D),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                null
        )).getType() == HitResult.Type.MISS;
    }
}

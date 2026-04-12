package com.craftix.hostile_humans.mixin;

import com.craftix.hostile_humans.entity.HumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DoorInteractGoal.class)
public abstract class DoorInteractMixin {

    @Shadow
    protected Mob mob;

    @Unique
    private static boolean isMetalDoor(Level p_52746_, BlockPos p_52747_) {
        return isMetalDoor(p_52746_.getBlockState(p_52747_));
    }

    @Unique
    private static boolean isMetalDoor(BlockState p_52818_) {
        return p_52818_.getBlock() instanceof DoorBlock && p_52818_.is(BlockTags.DOORS) && !p_52818_.is(BlockTags.WOODEN_DOORS);
    }

    @Unique
    private static boolean canMobSeeBlock(Level world, Vec3 from, BlockPos to) {
        return world.clip(new ClipContext(
                from,
                new Vec3(to.getX() + 0.5D, to.getY() + 0.5D, to.getZ() + 0.5D),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                null
        )).getType() == HitResult.Type.MISS;
    }

    @Redirect(method = "canUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/DoorBlock;isWoodenDoor(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean injected(Level level, BlockPos blockpos) {
        boolean isWoodenDoor = DoorBlock.isWoodenDoor(level, blockpos);
        if (!(mob instanceof HumanEntity)) return isWoodenDoor;

        boolean isMetalDoor = isMetalDoor(level, blockpos);
        boolean isMetalOpen = false;
        if (isMetalDoor) {
            for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.getX() - 2, blockpos.getY() - 2, blockpos.getZ() - 2, blockpos.getX() + 2, blockpos.getY() + 2, blockpos.getZ() + 2)) {
                BlockState state = level.getBlockState(blockpos1);
                Block block = state.getBlock();
                if (canMobSeeBlock(level, mob.getEyePosition(), blockpos1)) {

                    if (block instanceof ButtonBlock) {
                        ((ButtonBlock) block).press(state, level, blockpos1);
                        isMetalOpen = true;
                    }
                    if (block instanceof LeverBlock) {
                        ((LeverBlock) block).pull(state, level, blockpos1);
                        isMetalOpen = true;
                    }
                }
            }
        }
        return isWoodenDoor || isMetalOpen;
    }
}

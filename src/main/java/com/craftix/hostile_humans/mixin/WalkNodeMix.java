package com.craftix.hostile_humans.mixin;

import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.HumanEntity;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WalkNodeEvaluator.class)
public abstract class WalkNodeMix extends NodeEvaluator {

    @Inject(method = "getBlockPathTypeRaw", at = @At(value = "HEAD"), cancellable = true)
    private static void injected3(BlockGetter p_77644_, BlockPos p_77645_, CallbackInfoReturnable<BlockPathTypes> cir) {
        BlockState blockstate = p_77644_.getBlockState(p_77645_);
        BlockPathTypes type = blockstate.getBlockPathType(p_77644_, p_77645_, null);
        if (type != null) return;
        if (blockstate.getBlock() instanceof FenceGateBlock) {
            cir.setReturnValue(BlockPathTypes.DOOR_IRON_CLOSED);
        }
    }

    @Inject(method = "evaluateBlockPathType", at = @At(value = "HEAD"), cancellable = true)
    public void injected(BlockGetter getter, boolean p_77615_, boolean p_77616_, BlockPos blockPos, BlockPathTypes pathTypes, CallbackInfoReturnable<BlockPathTypes> cir) {
        if (!(mob instanceof Human))
            return;
        if (pathTypes == BlockPathTypes.DOOR_IRON_CLOSED && p_77615_ && p_77616_) {
            pathTypes = BlockPathTypes.WALKABLE_DOOR;
        }
        cir.setReturnValue(pathTypes);
    }

    @Inject(method = "getNeighbors", at = @At(value = "RETURN"), cancellable = true)
    public void injected2(Node[] p_77640_, Node p_77641_, CallbackInfoReturnable<Integer> cir) {
        if (!(mob instanceof Human))
            return;
        cir.setReturnValue(HumanUtil.createLadderNodeFor(cir.getReturnValue(), p_77640_, p_77641_, (p) -> getNode(p), this.level, this.mob));
    }
}

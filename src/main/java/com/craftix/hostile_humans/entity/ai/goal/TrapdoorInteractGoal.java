package com.craftix.hostile_humans.entity.ai.goal;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.tags.BlockTags;

public abstract class TrapdoorInteractGoal extends Goal {
	protected Mob mob;
	protected BlockPos doorPos = BlockPos.ZERO;
	protected boolean hasDoor;
	private boolean passed;
	private float trapdoorOpenDirY;

	public TrapdoorInteractGoal(Mob p_25193_) {
		this.mob = p_25193_;
		if (!GoalUtils.hasGroundPathNavigation(p_25193_)) {
			throw new IllegalArgumentException("Unsupported mob type for TrapdoorInteractGoal");
		}
	}

	protected boolean isOpen() {
		if (!this.hasDoor) {
			return false;
		} else {
			BlockState blockstate = this.mob.level().getBlockState(this.doorPos);
			if (!(blockstate.getBlock() instanceof TrapDoorBlock)) {
				this.hasDoor = false;
				return false;
			} else {
				return blockstate.getValue(TrapDoorBlock.OPEN);
			}
		}
	}

	protected void setOpen(boolean p_25196_) {
		System.out.println("Tried to open "+p_25196_);
		if (this.hasDoor) {
			BlockState blockstate = this.mob.level().getBlockState(this.doorPos);
			if (blockstate.getBlock() instanceof TrapDoorBlock) {
				blockstate.setValue(TrapDoorBlock.OPEN, p_25196_);
				setOpenTrapdoor(this.mob, this.mob.level(), blockstate, this.doorPos, p_25196_);
			}
		}

	}

	public boolean canUse() {
		if (!GoalUtils.hasGroundPathNavigation(this.mob)) { 
			return false; 
		} 
		else if (!this.mob.verticalCollision) {
			return false;
		} else {
			GroundPathNavigation groundpathnavigation = (GroundPathNavigation)this.mob.getNavigation();
			Path path = groundpathnavigation.getPath();
			if (path != null && !path.isDone() && groundpathnavigation.canOpenDoors()) {
				for(int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); ++i) {
					///System.out.println(this.mob.getId()+" B"+i);
					Node node = path.getNode(i);
					this.doorPos = new BlockPos(node.x, node.y, node.z);
					for (int j = 0; j < 3; j++) {
						this.doorPos = this.doorPos.above();
						///System.out.println(this.mob.getId()+" Node + "+j+" "+this.doorPos);
						//						if (!(this.mob.distanceToSqr((double)this.mob.getX(), this.doorPos.getY(), (double)this.mob.getZ()) > 2.25D)) {
						///System.out.println(this.mob.getId()+" C"+i);
						this.hasDoor = isWoodenTrapdoor(this.mob.level(), this.doorPos);
						if (this.hasDoor) {
							///System.out.println(this.mob.getId()+" D"+i);
							return true;
						}
						//						}
					}

				}

				this.doorPos = this.mob.blockPosition().above();
				this.hasDoor = isWoodenTrapdoor(this.mob.level(), this.doorPos);
				return this.hasDoor;
			} else {
				return false;
			}
		}
	}

	public boolean canContinueToUse() {
		return !this.passed;
	}

	public void start() {
		this.passed = false;
		this.trapdoorOpenDirY = (float)((double)this.doorPos.getY() + 0.5D - this.mob.getY());
	}

	public boolean requiresUpdateEveryTick() {
		return true;
	}

	public void tick() {
		float f = (float)((double)this.doorPos.getY() + 0.5D - this.mob.getY());
		float f2 = this.trapdoorOpenDirY * f;
		if (f2 < 0.0F) {
			this.passed = true;
		}

	}
	//


	public static boolean isWoodenTrapdoor(Level p_52746_, BlockPos p_52747_) {
		return isWoodenTrapdoor(p_52746_.getBlockState(p_52747_));
	}

	public static boolean isWoodenTrapdoor(BlockState p_52818_) {
		return p_52818_.getBlock() instanceof TrapDoorBlock && p_52818_.is(BlockTags.WOODEN_TRAPDOORS);
	}
	
	public void setOpenTrapdoor(@Nullable Entity p_153166_, Level p_153167_, BlockState p_153168_, BlockPos p_153169_, boolean p_153170_) {
		if (/* p_153168_.is(this) && */p_153168_.getValue(TrapDoorBlock.OPEN) != p_153170_) {
			p_153167_.setBlock(p_153169_, p_153168_.setValue(TrapDoorBlock.OPEN, Boolean.valueOf(p_153170_)), 10);
			//			p_153168_.getBlock().playSound(p_153167_, p_153169_, p_153170_);
			p_153167_.gameEvent(p_153166_, p_153170_ ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, p_153169_);
		}
	}
}

package com.craftix.hostile_humans.entity.ai.goal;

import java.util.EnumSet;

import javax.annotation.Nullable;

import com.craftix.hostile_humans.entity.entities.Human;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public class LookForBedGoal extends Goal {
	protected final Mob mob;
	private final double speedModifier;
	@Nullable
	protected BlockPos pos = UNREACHABLE;

	public static final BlockPos UNREACHABLE = new BlockPos(0, -9999, 0);

	public LookForBedGoal(Mob pMob, double pSpeedModifier) {
		this.mob = pMob;
		this.speedModifier = pSpeedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	/**
	 * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
	 * method as well.
	 */
	public boolean canUse() {
		if (this.mob.getTarget() != null) return false;
		if (this.mob.isSleeping() && this.mob.level().isNight()) return true;
		if (!this.mob.level().isNight() || !((Human)this.mob).isSleepingThisNight()) {
			this.pos = UNREACHABLE;
			return false;
		}
		else {
			if (this.pos != UNREACHABLE) return true;
			for (int x = -20; x < 20; x++)
				for (int y = -5; y < 5; y++)
					for (int z = -20; z < 20; z++) {
						BlockPos bedPos = this.mob.blockPosition().offset(x, y, z);
						BlockState isBed = this.mob.level().getBlockState(bedPos);
						//System.out.println("BED?"+bedPos);
						if (isBed.isBed(this.mob.level(), bedPos, mob) && !isBed.getValue(BedBlock.OCCUPIED)) {
							if (isBed.hasProperty(BedBlock.PART) && isBed.getValue(BedBlock.PART) == BedPart.FOOT) {
								BlockPos blockpos = bedPos.relative(getNeighbourDirection(isBed.getValue(BedBlock.PART), isBed.getValue(BedBlock.FACING)));
								this.pos = blockpos;
							}
							else {
								this.pos = bedPos;
							}
							return true;
						}
					}
			if (this.pos == UNREACHABLE) return false;
			return this.mob.blockPosition().distSqr(this.pos) < 1000D;
		}
	}

	private static Direction getNeighbourDirection(BedPart p_49534_, Direction p_49535_) {
		return p_49534_ == BedPart.FOOT ? p_49535_ : p_49535_.getOpposite();
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean canContinueToUse() {
		if (this.pos == UNREACHABLE) return false;
		if (this.mob.blockPosition().distSqr(this.pos) < .5D) {
			return false;
		}

		return this.canUse();
	}

	/**
	 * Execute a one shot task or start executing a continuous task
	 */
	public void start() {
		
	}

	/**
	 * Reset the task's internal state. Called when this task is interrupted by another one
	 */
	public void stop() {
		//this.pos = UNREACHABLE;
		this.mob.getNavigation().stop();
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		if (!this.mob.level().getBlockState(this.pos).hasProperty(BedBlock.OCCUPIED) || this.mob.level().getBlockState(this.pos).getValue(BedBlock.OCCUPIED)) {
			this.pos = UNREACHABLE;
			return;
		}
		if (this.mob.blockPosition().distSqr(this.pos) < 5D) {
			this.mob.getNavigation().stop();
			if (!this.mob.isSleeping() && !this.mob.level().getBlockState(this.pos).getValue(BedBlock.OCCUPIED))
				this.mob.startSleeping(pos);
		} else {
			this.mob.getNavigation().moveTo(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.speedModifier);
		}
	}
}

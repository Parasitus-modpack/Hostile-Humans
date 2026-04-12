package com.craftix.hostile_humans.entity.ai.goal;

import java.util.EnumSet;

import javax.annotation.Nullable;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ChestExtension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.ChestBlock;

public class LookForChestGoal extends Goal {
	protected final Human mob;
	private final double speedModifier;
	@Nullable
	protected BlockPos pos = UNREACHABLE;
	protected int timer = 0;

	public static final BlockPos UNREACHABLE = new BlockPos(0, -9999, 0);

	public LookForChestGoal(Human pMob, double pSpeedModifier) {
		this.mob = pMob;
		this.speedModifier = pSpeedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	/**
	 * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
	 * method as well.
	 */
	public boolean canUse() {
		if (this.mob.lookForChestCooldown > 0) return false;
		else if (this.mob.getTarget() != null || this.mob.isSleeping()) return false;
		if (this.pos != UNREACHABLE) return true;
		for (int x = -20; x < 20; x++)
			for (int y = -5; y < 5; y++)
				for (int z = -20; z < 20; z++) {
					BlockPos chestPos = this.mob.blockPosition().offset(x, y, z);
					if (this.mob.level().getBlockState(chestPos).getBlock() instanceof ChestBlock) {
						this.pos = chestPos;
						this.timer = this.mob.getRandom().nextInt(20 * 5, 20 * 15);
						return true;
					}
				}
		if (this.pos == UNREACHABLE) {
			this.mob.lookForChestCooldown = 20 * 60 * 20;
			return false;
		}
		return this.mob.blockPosition().distSqr(this.pos) < 1000D;
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
		this.mob.getNavigation().stop();
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		if (this.mob.blockPosition().distSqr(this.pos) < 5D) {
			this.mob.getNavigation().stop();
			if (this.mob.level().getBlockEntity(this.pos) instanceof ChestExtension ch) {
				this.timer--;
				if (timer > 0)
					ch.openersCounter().incrementOpeners(null, this.mob.level(), this.pos, this.mob.level().getBlockState(pos));
				else {
					ch.openersCounter().decrementOpeners(null, this.mob.level(), this.pos, this.mob.level().getBlockState(pos));
					this.pos = UNREACHABLE;
					this.mob.lookForChestCooldown = 20 * 60 * 10;
				}
			}
		} else {
			this.mob.getNavigation().moveTo(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.speedModifier);
		}
	}
}

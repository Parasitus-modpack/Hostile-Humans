package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.item.Items;

public class TridentAttackGoal extends RangedAttackGoal {
    private final Human human;

    public TridentAttackGoal(Human p_32450_, double p_32451_, int p_32452_, float p_32453_) {
        super(p_32450_, p_32451_, p_32452_, p_32453_);
        this.human = p_32450_;
    }

    public boolean canUse() {
        if (human.getTarget() == null) return false;
        if (human.isFleeing || human.isSleepingOrLyingDown()) return false;

        return super.canUse() && this.human.getMainHandItem().is(Items.TRIDENT) && human.getTarget().distanceTo(human) > 2.5;
    }

    @Override
    public void tick() {
        if (human.isSleepingOrLyingDown()) {
            human.stopUsingItem();
            return;
        }
        human.getNavigation().stop();
        super.tick();
    }

    @Override
    public boolean canContinueToUse() {
        if (human.isFleeing || human.isSleepingOrLyingDown()) return false;
        if (!this.human.getMainHandItem().is(Items.TRIDENT)) return false;
        return super.canContinueToUse();
    }

    public void start() {
        super.start();

        this.human.setAggressive(true);
        this.human.startUsingItem(InteractionHand.MAIN_HAND);
    }

    public void stop() {
        super.stop();
        this.human.stopUsingItem();
        this.human.setAggressive(false);
    }
}
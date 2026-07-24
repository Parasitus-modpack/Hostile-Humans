package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.CrossbowItem;

public class RaiseShieldGoal extends Goal {

    public final Human human;

    public RaiseShieldGoal(Human guard) {
        this.human = guard;
    }

    @Override
    public boolean canUse() {
        if (human.shieldUpTicks > 0) return human.shieldCoolDown == 0 && human.isBlocking();
    	
        boolean animalAnger = true;
        if (human.getTarget() instanceof Animal animal) {
            if (animal.getTarget() != human) {
                animalAnger = false;
            }
        }

        if (human.getRandom().nextFloat() < 0.08) {
            human.shieldCoolDown = 6;
            return false;
        }

        return !CrossbowItem.isCharged(human.getMainHandItem()) && (human.getOffhandItem().getItem().canPerformAction(human.getOffhandItem(), net.minecraftforge.common.ToolActions.SHIELD_BLOCK) && raiseShield() && human.shieldCoolDown == 0 && animalAnger);
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        if (human.getOffhandItem().getItem().canPerformAction(human.getOffhandItem(), net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) {
        	human.shieldUpTicks = 20;
        	human.startUsingItem(InteractionHand.OFF_HAND);
        }
    }

    @Override
    public void stop() {
        human.stopUsingItem();
    }

    protected boolean raiseShield() {
        LivingEntity target = human.getTarget();
        if (target != null && human.shieldCoolDown == 0) {
            boolean isRanged = HumanUtil.isRangedWeapon(human.getMainHandItem());

            return human.distanceTo(target) <= 4.0D || target instanceof Creeper || target instanceof RangedAttackMob && target.distanceTo(human) >= 5.0D && !isRanged || target instanceof Ravager;
        }
        return false;
    }
}

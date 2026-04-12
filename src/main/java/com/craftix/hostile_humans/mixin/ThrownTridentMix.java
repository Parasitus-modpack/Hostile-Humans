package com.craftix.hostile_humans.mixin;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {ThrownTrident.class})
public abstract class ThrownTridentMix extends AbstractArrow {

    @Shadow
    private ItemStack tridentItem;

    protected ThrownTridentMix(EntityType<? extends AbstractArrow> p_36721_, Level p_36722_) {
        super(p_36721_, p_36722_);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void injected(CallbackInfo ci) {
        if (level().isClientSide) return;
        if (tickCount < 20) return;
        if (getOwner() == null) return;

        var otherHumans = level().getEntities(this, this.getBoundingBox().inflate(0.4), entity -> entity instanceof Human otherHuman && getOwner() == otherHuman);
        for (Entity otherHuman : otherHumans) {
            Human human = (Human) otherHuman;
            human.putItemAway(tridentItem.copy());
            human.putItemAway(human.getMainHandItem());
            discard();
            ci.cancel();
        }
    }
}

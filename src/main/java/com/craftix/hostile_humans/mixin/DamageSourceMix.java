package com.craftix.hostile_humans.mixin;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

@Mixin(value = DamageSources.class, priority = 999)
public class DamageSourceMix {

    private static final ResourceKey<DamageType> HUMAN_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(HostileHumans.MOD_ID, "human"));
    private static final ResourceKey<DamageType> HUMAN_WITH_NAME_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(HostileHumans.MOD_ID, "human_with_name"));

    @Shadow
    @Final
    private Registry<DamageType> damageTypes;

    @Inject(method = "mobAttack", at = @At("HEAD"), cancellable = true)
    private void getRenderDistance(LivingEntity entity, CallbackInfoReturnable<DamageSource> cir) {
        if (entity instanceof Human) {
            var damageTypeKey = entity.hasCustomName() ? HUMAN_WITH_NAME_DAMAGE_TYPE : HUMAN_DAMAGE_TYPE;
            cir.setReturnValue(new DamageSource(this.damageTypes.getHolderOrThrow(damageTypeKey), entity));
        }
    }
}

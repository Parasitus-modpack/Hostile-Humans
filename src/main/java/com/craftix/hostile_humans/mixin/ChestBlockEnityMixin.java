package com.craftix.hostile_humans.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.craftix.hostile_humans.entity.entities.ChestExtension;

import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;

@Mixin(ChestBlockEntity.class)
public class ChestBlockEnityMixin implements ChestExtension {
	@Shadow @Final private ContainerOpenersCounter openersCounter;
    @Shadow @Final private ChestLidController chestLidController;
	@Unique
	private boolean hostileHumans$forcedOpen;

	@Override
	public ContainerOpenersCounter openersCounter() {
		return openersCounter;
	}

    @Override
    public void hostileHumans$setForcedOpen(boolean open) {
		this.hostileHumans$forcedOpen = open;
        this.chestLidController.shouldBeOpen(open);
    }

	@Inject(method = "recheckOpen", at = @At("TAIL"))
	private void hostileHumans$preserveForcedOpen(CallbackInfo ci) {
		if (this.hostileHumans$forcedOpen) {
			this.chestLidController.shouldBeOpen(true);
		}
	}
}

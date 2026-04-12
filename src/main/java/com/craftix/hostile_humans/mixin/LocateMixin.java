package com.craftix.hostile_humans.mixin;

import com.craftix.hostile_humans.HumanUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LocateCommand.class)
public abstract class LocateMixin {

    @Unique
    private static final DynamicCommandExceptionType ERROR_DISABLED = new DynamicCommandExceptionType(value -> Component.translatable("error.disabled", value));
    @Shadow
    @Final
    private static DynamicCommandExceptionType ERROR_STRUCTURE_INVALID;

    @Inject(method = "locateStructure", at = @At("HEAD"))
    private static void injected(CommandSourceStack sourceStack, ResourceOrTagKeyArgument.Result<Structure> result, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {

        Registry<Structure> registry = sourceStack.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> holderset = result.unwrap()
                .map(resourceKey -> registry.getHolder(resourceKey).map(HolderSet::direct), registry::getTag)
                .orElseThrow(() -> ERROR_STRUCTURE_INVALID.create(result.asPrintable()));

        for (var val : holderset) {
            String path = val.unwrapKey().get().location().getPath();
            if (HumanUtil.isStructureDisabled(path)) {
                throw ERROR_DISABLED.create(result.asPrintable());
            }
        }
    }
}

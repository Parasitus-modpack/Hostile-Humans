package com.craftix.hostile_humans.network.message;

import com.craftix.hostile_humans.entity.HumanCommand;
import com.craftix.hostile_humans.entity.HumanEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class MessageCommandHuman {
    protected final String humanMobEntityUUID;
    protected final String command;

    public MessageCommandHuman(String humanMobEntityUUID, String command) {
        this.humanMobEntityUUID = humanMobEntityUUID;
        this.command = command;
    }

    public static void handle(MessageCommandHuman message,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handlePacket(message, context));
        context.setPacketHandled(true);
    }

    public static void handlePacket(MessageCommandHuman message,
                                    NetworkEvent.Context context) {
        ServerPlayer serverPlayer = context.getSender();
        ServerLevel serverLevel = serverPlayer.serverLevel();
        UUID uuid = UUID.fromString(message.getHHFollowerUUID());
        Entity entity = serverLevel.getEntity(uuid);

        if (entity instanceof HumanEntity humanEntity) {
            HumanCommand command = HumanCommand.valueOf(message.getCommand());
            if (serverPlayer.getUUID().equals(humanEntity.getOwnerUUID())) {
                humanEntity.handleCommand(command);
            }
        }
    }

    public String getCommand() {
        return this.command;
    }

    public String getHHFollowerUUID() {
        return this.humanMobEntityUUID;
    }
}

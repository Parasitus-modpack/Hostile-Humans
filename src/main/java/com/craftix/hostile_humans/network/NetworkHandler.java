package com.craftix.hostile_humans.network;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.network.message.MessageCommandHuman;
import com.craftix.hostile_humans.network.message.MessageHumanData;
import com.craftix.hostile_humans.network.message.MessageHumansData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel INSTANCE =
            NetworkRegistry.newSimpleChannel(new ResourceLocation(HostileHumans.MOD_ID, "network"),
                    () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static final ConcurrentHashMap<UUID, ServerPlayer> serverPlayerMap = new ConcurrentHashMap<>();
    private static int id = 0;
    private static CompoundTag lastHumanMobDataPackage;
    private static CompoundTag lastHumanMobsDataPackage;

    protected NetworkHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handlePlayerChangedDimensionEvent(PlayerChangedDimensionEvent event) {
        addServerPlayer(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handlePlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
        addServerPlayer(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handlePlayerLoggedOutEvent(PlayerEvent.PlayerLoggedOutEvent event) {
        removeServerPlayer(event.getEntity());
    }

    public static void registerNetworkHandler(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            INSTANCE.registerMessage(id++, MessageCommandHuman.class, (message, buffer) -> {
                        buffer.writeUtf(message.getHHFollowerUUID());
                        buffer.writeUtf(message.getCommand());
                    }, buffer -> new MessageCommandHuman(buffer.readUtf(), buffer.readUtf()),
                    MessageCommandHuman::handle);

            INSTANCE.registerMessage(id++, MessageHumansData.class,
                    (message, buffer) -> buffer.writeNbt(message.data()),
                    buffer -> new MessageHumansData(buffer.readNbt()),
                    MessageHumansData::handle);

            INSTANCE.registerMessage(id++, MessageHumanData.class, (message, buffer) -> {
                        buffer.writeUtf(message.getHHFollowerUUID());
                        buffer.writeNbt(message.getData());
                    }, buffer -> new MessageHumanData(buffer.readUtf(), buffer.readNbt()),
                    MessageHumanData::handle);
        });
    }

    public static void updateHHFollowersData(UUID ownerUUID, CompoundTag humanMobsData) {
        if (ownerUUID != null && humanMobsData != null && !humanMobsData.isEmpty()
                && !humanMobsData.equals(lastHumanMobsDataPackage)) {
            ServerPlayer serverPlayer = getServerPlayer(ownerUUID);
            if (serverPlayer == null) {
                return;
            }

            INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new MessageHumansData(humanMobsData));
            lastHumanMobsDataPackage = humanMobsData;
        }
    }

    public static void updateHHFollowerData(UUID humanMobEntityUUID, UUID ownerUUID,
                                            CompoundTag humanMobData) {
        if (humanMobEntityUUID != null && ownerUUID != null && humanMobData != null
                && !humanMobData.isEmpty() && !humanMobData.equals(lastHumanMobDataPackage)) {
            ServerPlayer serverPlayer = getServerPlayer(ownerUUID);
            if (serverPlayer == null) {
                return;
            }

            INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new MessageHumanData(humanMobEntityUUID.toString(), humanMobData));
            lastHumanMobDataPackage = humanMobData;
        }
    }

    public static void addServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            addServerPlayer(serverPlayer.getUUID(), serverPlayer);
        }
    }

    public static void addServerPlayer(UUID uuid, ServerPlayer serverPlayer) {
        serverPlayerMap.put(uuid, serverPlayer);
    }

    public static void removeServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            removeServerPlayer(serverPlayer.getUUID());
        }
    }

    public static void removeServerPlayer(UUID uuid) {
        serverPlayerMap.remove(uuid);
    }

    public static ServerPlayer getServerPlayer(UUID uuid) {
        return serverPlayerMap.get(uuid);
    }
}

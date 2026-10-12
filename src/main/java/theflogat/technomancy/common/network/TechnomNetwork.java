package theflogat.technomancy.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if forge {
import net.minecraftforge.network.NetworkDirection;
//?}
//? if forge {
import net.minecraftforge.network.NetworkRegistry;
//?}
import net.minecraftforge.network.PacketDistributor;
//? if forge {
import net.minecraftforge.network.simple.SimpleChannel;
//?}
import theflogat.technomancy.Technomancy;

/** The mod's one network channel: the Existence HUD sync and the client-drawn effects. */
public final class TechnomNetwork {

    /**
     * The channel's protocol version. Adding a packet is a breaking change for a mismatched
     * client/server pair, so this moves with the packet list: "2" added the effect packet.
     */
    private static final String VERSION = "2";
    //? if forge {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Technomancy.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    //?}

    /** How far an effect packet reaches; wider than the render distance would be wasted. */
    private static final double FX_RADIUS = 64.0D;

    private TechnomNetwork() {
    }

    //? if forge {
    public static void register(net.minecraftforge.eventbus.api.IEventBus modBus) {
        int id = 0;
        CHANNEL.messageBuilder(ExistenceSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ExistenceSyncPacket::encode)
                .decoder(ExistenceSyncPacket::decode)
                .consumerMainThread(ExistenceSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(TechnomFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TechnomFxPacket::encode)
                .decoder(TechnomFxPacket::decode)
                .consumerMainThread(TechnomFxPacket::handle)
                .add();
    }

    //?} else {
    /*public static void register(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar(VERSION);
            registrar.playToClient(ExistenceSyncPacket.TYPE, ExistenceSyncPacket.CODEC, (packet, context) -> packet.handle(context));
            registrar.playToClient(TechnomFxPacket.TYPE, TechnomFxPacket.CODEC, (packet, context) -> packet.handle(context));
        });
    }
    *///?}

    public static void sendExistence(ServerPlayer player, int level, int power, int affinity) {
        //? if forge {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ExistenceSyncPacket(level, power, affinity));
        //?} else {
        /*PacketDistributor.sendToPlayer(player, new ExistenceSyncPacket(level, power, affinity));
        *///?}
    }

    /** Sends an effect to every player close enough to see it. */
    public static void sendFx(ServerLevel level, BlockPos pos, TechnomFxPacket packet) {
        //? if forge {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, FX_RADIUS, level.dimension())), packet);
        //?} else {
        /*PacketDistributor.sendToPlayersNear(level, null, pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, FX_RADIUS, packet);
        *///?}
    }
}

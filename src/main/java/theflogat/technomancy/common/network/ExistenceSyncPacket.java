package theflogat.technomancy.common.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
//? if forge {
import net.minecraftforge.fml.DistExecutor;
//?}
//? if forge {
import net.minecraftforge.network.NetworkEvent;
//?}

/** Server to client: the player's Existence level, power and dominant affinity ordinal. */
//? if forge {
public record ExistenceSyncPacket(int level, int power, int affinity) {
//?} else {
/*public record ExistenceSyncPacket(int level, int power, int affinity) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<ExistenceSyncPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("technom", "existence"));
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, ExistenceSyncPacket> CODEC =
            net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> packet.encode(buffer), ExistenceSyncPacket::decode);

    @Override
    public Type<ExistenceSyncPacket> type() {
        return TYPE;
    }
*///?}

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(level);
        buffer.writeVarInt(power);
        buffer.writeVarInt(affinity);
    }

    public static ExistenceSyncPacket decode(FriendlyByteBuf buffer) {
        return new ExistenceSyncPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    //? if forge {
    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> theflogat.technomancy.client.ExistenceHud.accept(level, power, affinity)));
        ctx.setPacketHandled(true);
    }
    //?} else {
    /*public void handle(net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
                theflogat.technomancy.client.ExistenceHud.accept(level, power, affinity);
            }
        });
    }
    *///?}
}

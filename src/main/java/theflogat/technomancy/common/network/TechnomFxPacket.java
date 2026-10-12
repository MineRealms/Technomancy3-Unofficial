package theflogat.technomancy.common.network;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
//? if forge {
import net.minecraftforge.fml.DistExecutor;
//?}
//? if forge {
import net.minecraftforge.network.NetworkEvent;
//?}

/**
 * Server to client: something happened at {@code pos} that only a client can draw.
 *
 * <p>Particles need no packet — {@code ServerLevel#sendParticles} already reaches the right
 * players. A bolt is geometry, and geometry belongs to whoever owns the render loop, so this
 * carries just enough to rebuild it: where, what colour, and which shape.</p>
 */
//? if forge {
public record TechnomFxPacket(byte kind, BlockPos pos, int rgb) {
//?} else {
/*public record TechnomFxPacket(byte kind, BlockPos pos, int rgb) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<TechnomFxPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("technom", "fx"));
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, TechnomFxPacket> CODEC =
            net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> packet.encode(buffer), TechnomFxPacket::decode);

    @Override
    public Type<TechnomFxPacket> type() {
        return TYPE;
    }
*///?}

    /** A node came into being: four jagged bolts climbing off the block's corners. */
    public static final byte KIND_NODE = 0;

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(kind);
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(rgb);
    }

    public static TechnomFxPacket decode(FriendlyByteBuf buffer) {
        return new TechnomFxPacket(buffer.readByte(), buffer.readBlockPos(), buffer.readVarInt());
    }

    //? if forge {
    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> theflogat.technomancy.client.fx.TechnomClientFx.accept(kind, pos, rgb)));
        ctx.setPacketHandled(true);
    }
    //?} else {
    /*public void handle(net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
                theflogat.technomancy.client.fx.TechnomClientFx.accept(kind, pos, rgb);
            }
        });
    }
    *///?}
}

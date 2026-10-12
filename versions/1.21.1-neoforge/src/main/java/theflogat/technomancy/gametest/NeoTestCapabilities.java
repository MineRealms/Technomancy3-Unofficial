package theflogat.technomancy.gametest;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import theflogat.technomancy.Technomancy;

/** Dev-only provider; the entire gametest package is excluded from release artifacts. */
@EventBusSubscriber(modid = Technomancy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoTestCapabilities {
    private NeoTestCapabilities() {
    }

    @SubscribeEvent
    public static void registerTestSink(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntityType.BARREL,
                (barrel, side) -> GameTestEnergySink.at(barrel.getBlockPos()));
    }
}

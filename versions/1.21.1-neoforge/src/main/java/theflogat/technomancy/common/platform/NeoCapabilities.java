package theflogat.technomancy.common.platform;

import dev.tc4port.thaumcraft.api.wand.WandCarrierItem;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import theflogat.technomancy.common.energy.MachineEnergy;
import theflogat.technomancy.common.registry.TechnomBlockEntities;
import theflogat.technomancy.common.wands.TechnomWandRods;
import theflogat.technomancy.common.wands.TechnoturgeEnergyStorage;
import theflogat.technomancy.compat.gtceu.GtceuPresence;

/** NeoForge provider registration, separate from the shared energy and machine logic. */
public final class NeoCapabilities {
    private NeoCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        registerEnergy(event, Capabilities.EnergyStorage.BLOCK);
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper(stack),
                theflogat.technomancy.common.registry.TechnomFluids.MANA_BUCKET.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechnomBlockEntities.PROCESSOR_TC.get(),
                (machine, side) -> machine.itemView(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechnomBlockEntities.ADV_DECON_TABLE.get(),
                (machine, side) -> machine.itemView(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TechnomBlockEntities.FLUX_LAMP.get(),
                (machine, side) -> machine.fluidView());
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item instanceof WandCarrierItem) {
                event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, context) ->
                        TechnomWandRods.hasRod(stack, TechnomWandRods.TECHNOTURGE)
                                ? new TechnoturgeEnergyStorage(stack) : null, item);
            }
        });
        if (GtceuPresence.isLoaded()) {
            theflogat.technomancy.compat.gtceu.NeoGtCapabilities.register(event);
        }
    }

    /** Also used by optional EU registration without leaking GT types into common classes. */
    public static <T> void registerEnergy(RegisterCapabilitiesEvent event, BlockCapability<T, Direction> cap) {
        energy(event, cap, TechnomBlockEntities.ENERGY_CONDENSER.get(), machine -> machine.energy());
        energy(event, cap, TechnomBlockEntities.ESSENTIA_DYNAMO.get(), machine -> machine.energy());
        energy(event, cap, TechnomBlockEntities.NODE_DYNAMO.get(), machine -> machine.energy());
        energy(event, cap, TechnomBlockEntities.NODE_FABRICATOR.get(), machine -> machine.energy());
        energy(event, cap, TechnomBlockEntities.ESSENTIA_FUSOR.get(), machine -> machine.energy());
        energy(event, cap, TechnomBlockEntities.ELDRITCH_CONSUMER.get(), machine -> machine.energy());
        energy(event, cap, TechnomBlockEntities.ELECTRIC_BELLOWS.get(), machine -> machine.energy());
        energy(event, cap, TechnomBlockEntities.BIOME_MORPHER.get(), machine -> machine.energy());
        energy(event, cap, TechnomBlockEntities.EXISTENCE_BURNER.get(), machine -> machine.energy());
        event.registerBlockEntity(cap, TechnomBlockEntities.NODE_FABRICATOR_SHELL.get(), (shell, side) -> {
            var controller = shell.controller();
            return controller == null ? null : controller.energy().getCapability(cap, side);
        });
    }

    private static <T, B extends BlockEntity> void energy(RegisterCapabilitiesEvent event,
            BlockCapability<T, Direction> cap, BlockEntityType<B> type, Function<B, MachineEnergy> energy) {
        event.registerBlockEntity(cap, type, (machine, side) -> energy.apply(machine).getCapability(cap, side));
    }
}

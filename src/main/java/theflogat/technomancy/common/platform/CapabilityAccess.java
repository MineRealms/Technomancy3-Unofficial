package theflogat.technomancy.common.platform;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?} else {
/*import net.neoforged.neoforge.capabilities.Capabilities;
*///?}

/** Queries the loader's providers, including providers supplied by other mods. */
public final class CapabilityAccess {
    private CapabilityAccess() {
    }

    @Nullable
    public static IEnergyStorage energy(BlockEntity owner, @Nullable Direction side) {
        //? if forge {
        return owner.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
        //?} else {
        /*return owner.getLevel() == null ? null : owner.getLevel().getCapability(
                Capabilities.EnergyStorage.BLOCK, owner.getBlockPos(), owner.getBlockState(), owner, side);
        *///?}
    }

    @Nullable
    public static IEnergyStorage energy(ItemStack stack) {
        //? if forge {
        return stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        //?} else {
        /*return stack.getCapability(Capabilities.EnergyStorage.ITEM);
        *///?}
    }

    @Nullable
    public static IItemHandler items(BlockEntity owner, @Nullable Direction side) {
        //? if forge {
        return owner.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
        //?} else {
        /*return owner.getLevel() == null ? null : owner.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, owner.getBlockPos(), owner.getBlockState(), owner, side);
        *///?}
    }

    @Nullable
    public static IFluidHandler fluids(BlockEntity owner, @Nullable Direction side) {
        //? if forge {
        return owner.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
        //?} else {
        /*return owner.getLevel() == null ? null : owner.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, owner.getBlockPos(), owner.getBlockState(), owner, side);
        *///?}
    }
}

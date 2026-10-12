package theflogat.technomancy.compat.gtceu;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
//? if forge {
import com.gregtechceu.gtceu.api.capability.compat.EUToFEProvider;
//?}
//? if forge {
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
//?} else {
/*import com.gregtechceu.gtceu.api.capability.GTCapability;
import com.gregtechceu.gtceu.api.capability.compat.GTEnergyWrapper;
import net.neoforged.neoforge.capabilities.BlockCapability;
*///?}
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
//?}
//? if forge {
import net.minecraftforge.common.util.LazyOptional;
//?}
import theflogat.technomancy.common.energy.EnergyPorts;
import theflogat.technomancy.common.energy.EnergyProtocolExtension;
import theflogat.technomancy.common.energy.EnergyUnits;
import theflogat.technomancy.common.energy.MachineEnergy;

/**
 * GTCEu EU protocol attached to one {@link MachineEnergy}: it serves
 * {@code GTCapability.CAPABILITY_ENERGY_CONTAINER} per face and pushes packets into GT
 * neighbours on behalf of {@link MachineEnergy#pushOutput()}.
 *
 * <p>Instances are only ever created through {@code GtceuEnergyIntegration}, which registers
 * the factory after the presence gate confirmed GTCEu is loaded.</p>
 */
public final class GtceuEnergyProtocol implements EnergyProtocolExtension {

    private static final int NULL_SIDE = 6;

    private final MachineEnergy energy;
    /** Frozen at construction so a view's readings and a push's packet size cannot disagree. */
    private final long qPerEu;
    //? if forge {
    @SuppressWarnings("unchecked")
    private final LazyOptional<IEnergyContainer>[] views = new LazyOptional[7];
    //?} else {
    /*private final IEnergyContainer[] views = new IEnergyContainer[7];
    *///?}

    public GtceuEnergyProtocol(MachineEnergy energy) {
        this.energy = energy;
        this.qPerEu = EnergyUnits.qPerEu();
    }

    //? if forge {
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap != GTCapability.CAPABILITY_ENERGY_CONTAINER || !speaksEu(side)) {
            return LazyOptional.empty();
        }
        return view(side).cast();
    }

    //?} else {
    /*@Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap != GTCapability.CAPABILITY_ENERGY_CONTAINER || !speaksEu(side)) return null;
        return (T) view(side);
    }
    *///?}

    /**
     * Only a face that really carries EU gets a container. Shadowing an FE-only face with a
     * container that always refuses would also suppress GTCEu's own {@code nativeEUToFE} wrapper
     * on that face, so such faces are left alone. An unsided query still gets a read-only
     * container as long as the machine speaks EU at all, which is what probes and TOP ask for.
     */
    private boolean speaksEu(@Nullable Direction side) {
        EnergyPorts ports = energy.ports();
        if (side == null) {
            return ports.euInput() != 0 || ports.euOutput() != 0;
        }
        return ports.euIn(side) || ports.euOut(side);
    }

    //? if forge {
    private LazyOptional<IEnergyContainer> view(@Nullable Direction side) {
        int index = side == null ? NULL_SIDE : side.get3DDataValue();
        LazyOptional<IEnergyContainer> view = views[index];
        if (view == null) {
            EnergyPorts ports = energy.ports();
            // A null side is a read-only probe: it never carries transfer rights.
            boolean input = side != null && ports.euIn(side);
            boolean output = side != null && ports.euOut(side);
            EuPort port = new EuPort(energy.ledger(), energy::gameTime, energy::mutable, qPerEu, side, input, output);
            IEnergyContainer container = new GtEnergyContainerView(port);
            view = LazyOptional.of(() -> container);
            views[index] = view;
        }
        return view;
    }

    //?} else {
    /*private IEnergyContainer view(@Nullable Direction side) {
        int index = side == null ? NULL_SIDE : side.get3DDataValue();
        if (views[index] == null) {
            EnergyPorts ports = energy.ports();
            boolean input = side != null && ports.euIn(side);
            boolean output = side != null && ports.euOut(side);
            EuPort port = new EuPort(energy.ledger(), energy::gameTime, energy::mutable, qPerEu, side, input, output);
            views[index] = new GtEnergyContainerView(port);
        }
        return views[index];
    }
    *///?}

    @Override
    public long push(BlockEntity neighbour, Direction face, long gameTime) {
        Direction target = face.getOpposite();
        //? if forge {
        IEnergyContainer receiver = neighbour.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, target)
                .orElse(null);
        //?} else {
        /*IEnergyContainer receiver = neighbour.getLevel() == null ? null : neighbour.getLevel().getCapability(
                GTCapability.CAPABILITY_ENERGY_CONTAINER, neighbour.getBlockPos(), neighbour.getBlockState(), neighbour, target);
        *///?}
        if (receiver == null) {
            return NOT_APPLICABLE;
        }
        // With nativeEUToFE on, GTCEu hangs this wrapper on every block entity that exposes Forge
        // Energy. Such a neighbour does not really speak EU: sending packets into it would quantise
        // our output and convert twice at GT's ratio, so we decline and let MachineEnergy use the
        // native FE path instead.
        //? if forge {
        if (receiver instanceof EUToFEProvider.GTEnergyWrapper) {
        //?} else {
        /*if (receiver instanceof GTEnergyWrapper) {
        *///?}
            return NOT_APPLICABLE;
        }
        // Past this point the neighbour speaks EU, so every refusal has to be 0 rather than
        // NOT_APPLICABLE; otherwise an FE fallback would slip past its voltage and face rules.
        if (!receiver.inputsEnergy(target)) {
            return 0;
        }
        return EuPush.push(energy.ledger(), qPerEu, gameTime,
                (voltage, amperage) -> receiver.acceptEnergyFromNetwork(target, voltage, amperage));
    }

    @Override
    public void invalidate() {
        //? if forge {
        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].invalidate();
                views[i] = null;
            }
        }
        //?} else {
        /*java.util.Arrays.fill(views, null);
        *///?}
    }
}

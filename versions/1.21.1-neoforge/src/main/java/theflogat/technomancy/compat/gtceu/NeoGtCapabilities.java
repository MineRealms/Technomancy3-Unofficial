package theflogat.technomancy.compat.gtceu;

import com.gregtechceu.gtceu.api.capability.GTCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import theflogat.technomancy.common.platform.NeoCapabilities;

/** Loaded only after the GTCEu presence gate has succeeded. */
public final class NeoGtCapabilities {
    private NeoGtCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        NeoCapabilities.registerEnergy(event, GTCapability.CAPABILITY_ENERGY_CONTAINER);
    }
}

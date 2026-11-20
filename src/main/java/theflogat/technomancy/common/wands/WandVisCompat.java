package theflogat.technomancy.common.wands;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.BuiltinAspects;
import dev.tc4port.thaumcraft.api.vis.VisStorage;
import dev.tc4port.thaumcraft.api.vis.VisStorageCapabilities;
import dev.tc4port.thaumcraft.api.wand.WandCapabilities;
import dev.tc4port.thaumcraft.api.wand.WandTool;
import dev.tc4port.thaumcraft.api.wand.WandView;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** TC4R 20777 moved wand Vis to native item capabilities and keyed it by aspect. */
public final class WandVisCompat {

    /** The six primal aspects, in TC4's legacy channel order (bit masks and save order). */
    public static final List<AspectId> PRIMALS = List.of(BuiltinAspects.AER, BuiltinAspects.TERRA,
            BuiltinAspects.IGNIS, BuiltinAspects.AQUA, BuiltinAspects.ORDO, BuiltinAspects.PERDITIO);

    private WandVisCompat() { }

    public static Optional<WandView> wandView(ItemStack stack) {
        //? if forge {
        return stack.getCapability(WandCapabilities.TOOL).resolve().flatMap(WandTool::view);
        //?} else {
        /*return Optional.ofNullable(stack.getCapability(WandCapabilities.TOOL)).flatMap(WandTool::view);
        *///?}
    }

    public static Optional<VisStorage> wandStorage(ItemStack stack) {
        //? if forge {
        return stack.getCapability(VisStorageCapabilities.ITEM).resolve();
        //?} else {
        /*return Optional.ofNullable(stack.getCapability(VisStorageCapabilities.ITEM));
        *///?}
    }
}

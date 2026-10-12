package theflogat.technomancy.common.registry;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
//? if forge {
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import net.neoforged.neoforge.registries.NeoForgeRegistries;
*///?}
import theflogat.technomancy.Technomancy;
import theflogat.technomancy.common.fluids.ManaFluidBlock;
import theflogat.technomancy.common.fluids.ManaFluidType;
import theflogat.technomancy.common.items.ManaBucketItem;

/**
 * The mana fluid, its block and its bucket.
 *
 * <p>The 1.7.10 {@code ManaFluid} was one {@code Fluid} with the water textures; 1.20.1 splits it
 * into a {@link FluidType} (attributes and textures), a source and a flowing {@link Fluid}, a
 * {@link LiquidBlock} and a {@code BucketItem}. The block deliberately gets no {@code BlockItem}:
 * the bucket is the item, exactly as upstream.</p>
 */
public final class TechnomFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            //? if forge {
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Technomancy.MOD_ID);
            //?} else {
            /*DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Technomancy.MOD_ID);
            *///?}
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, Technomancy.MOD_ID);

    public static final Supplier<FluidType> MANA_TYPE =
            FLUID_TYPES.register("mana", ManaFluidType::new);

    public static final Supplier<FlowingFluid> MANA =
            FLUIDS.register("mana", () -> new ForgeFlowingFluid.Source(manaProperties()));
    public static final Supplier<FlowingFluid> FLOWING_MANA =
            FLUIDS.register("flowing_mana", () -> new ForgeFlowingFluid.Flowing(manaProperties()));

    /** Registered straight into the block registry: a fluid block has no {@code BlockItem}. */
    public static final Supplier<LiquidBlock> MANA_BLOCK =
            TechnomBlocks.BLOCKS.register("mana_fluid", () -> new ManaFluidBlock(MANA));

    public static final Supplier<Item> MANA_BUCKET =
            TechnomItems.ITEMS.register("mana_bucket", () -> new ManaBucketItem(MANA));

    private TechnomFluids() {
    }

    private static ForgeFlowingFluid.Properties manaProperties() {
        return new ForgeFlowingFluid.Properties(MANA_TYPE, MANA, FLOWING_MANA)
                .block(MANA_BLOCK)
                .bucket(MANA_BUCKET)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5);
    }
}

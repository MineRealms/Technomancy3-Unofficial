package theflogat.technomancy.common.tiles.coils;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import theflogat.technomancy.common.coils.CoilItemMove;
import theflogat.technomancy.common.coils.CoilLink;
import theflogat.technomancy.common.coils.CoupleType;
import theflogat.technomancy.common.registry.TechnomBlockEntities;

/**
 * The item coil: a wireless hopper that pulls from linked inventories into the one it faces.
 * Ported from {@code TileItemTransmitter}.
 *
 * <p>Rate is the original's: per tick, one slot's worth (up to a full stack) from each linked
 * inventory. Inventories are reached through Forge's {@link IItemHandler} capability on the face
 * the player clicked when linking, and the target through the face touching the coil, so sided
 * inventories keep their automation rules; the original read raw {@code IInventory} slots and
 * special-cased double chests, which the capability already combines.</p>
 */
public final class ItemCoilBlockEntity extends CoilBlockEntity {

    private static final String TAG_FILTER = "Filter";
    private static final int MAX_PER_SLOT = 64;

    /** A ghost item: only compared against, never held or dropped. */
    private ItemStack filter = ItemStack.EMPTY;

    public ItemCoilBlockEntity(BlockPos pos, BlockState state) {
        super(TechnomBlockEntities.ITEM_COIL.get(), pos, state);
    }

    @Override
    public CoupleType coupleType() {
        return CoupleType.ITEM;
    }

    public ItemStack filter() {
        return filter;
    }

    /** @return whether the filter changed */
    public boolean setFilter(ItemStack template) {
        ItemStack next = template.isEmpty() ? ItemStack.EMPTY : template.copyWithCount(1);
        if (ItemStack.isSameItemSameTags(next, filter)) {
            return false;
        }
        filter = next;
        syncToClients();
        return true;
    }

    private boolean passes(ItemStack stack) {
        return filter.isEmpty() || ItemStack.isSameItemSameTags(stack, filter);
    }

    @Nullable
    private static IItemHandler handler(Level level, BlockPos pos, @Nullable Direction face) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be instanceof CoilBlockEntity) {
            return null;
        }
        return theflogat.technomancy.common.platform.CapabilityAccess.items(be, face);
    }

    @Override
    protected boolean isValidSource(Level level, BlockPos pos, @Nullable Direction face) {
        return handler(level, pos, face) != null;
    }

    @Override
    protected void drawFrom(Level level, CoilLink link, BlockPos targetPos, Direction facing) {
        IItemHandler target = handler(level, targetPos, facing.getOpposite());
        IItemHandler source = handler(level, link.pos(), link.face());
        if (target == null || source == null) {
            return;
        }
        CoilItemMove.moveOne(new SourceView(source), (stack, simulate) -> ItemHandlerHelper.insertItem(target, stack, simulate),
                this::passes, ItemStack::getCount,
                stack -> Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, stack),
                MAX_PER_SLOT);
    }

    /** The original's {@code !InvHelper.isFull}: some slot could still take something. */
    @Override
    protected boolean targetHasRoom(Level level, BlockPos targetPos, Direction facing) {
        IItemHandler target = handler(level, targetPos, facing.getOpposite());
        if (target == null) {
            return false;
        }
        for (int slot = 0; slot < target.getSlots(); slot++) {
            ItemStack stack = target.getStackInSlot(slot);
            int limit = Math.min(target.getSlotLimit(slot), stack.isEmpty() ? 64 : stack.getMaxStackSize());
            if (stack.getCount() < limit) {
                return true;
            }
        }
        return false;
    }

    private record SourceView(IItemHandler handler) implements CoilItemMove.Source<ItemStack> {
        @Override
        public int slots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }

        @Override
        public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
            return handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack insertAnywhere(ItemStack stack) {
            return ItemHandlerHelper.insertItem(handler, stack, false);
        }
    }

    // ---- persistence ----

    @Override
    //? if forge {
    public void load(CompoundTag tag) {
    //?} else {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        //? if forge {
        super.load(tag);
        //?} else {
        /*super.loadAdditional(tag, registries);
        *///?}
        //? if forge {
        readClientData(tag);
        //?} else {
        /*readClientData(tag, registries);
        *///?}
    }

    @Override
    //? if forge {
    protected void saveAdditional(CompoundTag tag) {
    //?} else {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        //? if forge {
        super.saveAdditional(tag);
        //?} else {
        /*super.saveAdditional(tag, registries);
        *///?}
        //? if forge {
        writeClientData(tag);
        //?} else {
        /*writeClientData(tag, registries);
        *///?}
    }

    @Override
    //? if forge {
    protected void writeClientData(CompoundTag tag) {
    //?} else {
    /*protected void writeClientData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        if (!filter.isEmpty()) {
            //? if forge {
            tag.put(TAG_FILTER, filter.save(new CompoundTag()));
            //?} else {
            /*tag.put(TAG_FILTER, filter.save(registries));
            *///?}
        }
    }

    @Override
    //? if forge {
    protected void readClientData(CompoundTag tag) {
    //?} else {
    /*protected void readClientData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        //? if forge {
        filter = tag.contains(TAG_FILTER) ? ItemStack.of(tag.getCompound(TAG_FILTER)) : ItemStack.EMPTY;
        //?} else {
        /*filter = tag.contains(TAG_FILTER) ? ItemStack.parseOptional(registries, tag.getCompound(TAG_FILTER)) : ItemStack.EMPTY;
        *///?}
    }
}

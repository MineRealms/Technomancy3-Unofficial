package theflogat.technomancy.common.platform;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if neoforge {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

/** The port's own item keys; callers mutate a copy and explicitly commit it. */
public final class StackData {
    private StackData() {
    }

    public static CompoundTag read(ItemStack stack) {
        //? if forge {
        return stack.getTag() == null ? new CompoundTag() : stack.getTag().copy();
        //?} else {
        /*return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        *///?}
    }

    public static void write(ItemStack stack, CompoundTag tag) {
        //? if forge {
        stack.setTag(tag.isEmpty() ? null : tag.copy());
        //?} else {
        /*if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        *///?}
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> edit) {
        CompoundTag tag = read(stack);
        edit.accept(tag);
        write(stack, tag);
    }

    public static void remove(ItemStack stack, String key) {
        update(stack, tag -> tag.remove(key));
    }
}

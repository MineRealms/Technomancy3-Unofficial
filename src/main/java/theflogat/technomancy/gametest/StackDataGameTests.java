package theflogat.technomancy.gametest;

import theflogat.technomancy.common.platform.StackData;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("technom")
@PrefixGameTestTemplate(false)
public final class StackDataGameTests {
    @GameTest(template = GameTestTemplates.EMPTY_5X5X5, batch = "technom_stack_data")
    public static void updatePreservesUnknownKeysAndCopyIsIndependent(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.STICK);
        StackData.update(stack, tag -> tag.putString("other_mod", "keep"));
        StackData.update(stack, tag -> tag.putLong("technom_charge", 123));
        ItemStack copied = stack.copy();
        StackData.update(copied, tag -> tag.putLong("technom_charge", 456));
        helper.assertTrue(StackData.read(stack).getLong("technom_charge") == 123, "copy changed original charge");
        helper.assertTrue(StackData.read(copied).getLong("technom_charge") == 456, "copy update was not committed");
        helper.assertTrue("keep".equals(StackData.read(copied).getString("other_mod")), "foreign key lost on copy");
        helper.succeed();
    }

    @GameTest(template = GameTestTemplates.EMPTY_5X5X5, batch = "technom_stack_data")
    public static void readsCannotMutateTheStackAndRemovalPreservesForeignData(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.STICK);
        StackData.update(stack, tag -> {
            tag.putString("foreign", "keep");
            tag.putInt("owned", 7);
        });
        CompoundTag snapshot = StackData.read(stack);
        snapshot.putInt("owned", 99);
        helper.assertTrue(StackData.read(stack).getInt("owned") == 7, "reading exposed mutable stack data");
        StackData.remove(stack, "owned");
        helper.assertTrue(!StackData.read(stack).contains("owned"), "remove did not commit");
        helper.assertTrue("keep".equals(StackData.read(stack).getString("foreign")), "remove discarded foreign key");
        StackData.remove(stack, "foreign");
        helper.assertTrue(StackData.read(stack).isEmpty(), "last key was not removed");
        helper.succeed();
    }
}

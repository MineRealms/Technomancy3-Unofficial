package theflogat.technomancy.gametest;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransport;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import theflogat.technomancy.Technomancy;
import theflogat.technomancy.common.registry.TechnomBlocks;

/** Exercises the real loot table and BlockItem placement, not a manual NBT round trip. */
@GameTestHolder(Technomancy.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JarDropGameTests {
    private static final AspectId IGNIS = AspectId.parse("ignis");

    private JarDropGameTests() {
    }

    @GameTest(template = GameTestTemplates.EMPTY_5X5X5, batch = "technom_jar_drops")
    public static void quantumJarDropKeepsItsEssentia(GameTestHelper helper) {
        roundTrip(helper, TechnomBlocks.QUANTUM_JAR.get());
    }

    @GameTest(template = GameTestTemplates.EMPTY_5X5X5, batch = "technom_jar_drops")
    public static void reservoirDropKeepsItsEssentia(GameTestHelper helper) {
        roundTrip(helper, TechnomBlocks.ESSENTIA_RESERVOIR.get());
    }

    private static void roundTrip(GameTestHelper helper, Block block) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 2, 2));
        BlockPos target = helper.absolutePos(new BlockPos(3, 2, 2));
        level.setBlockAndUpdate(source, block.defaultBlockState());
        var entity = level.getBlockEntity(source);
        helper.assertTrue(entity instanceof EssentiaTransport, "missing source container");
        var jar = (EssentiaTransport) entity;
        helper.assertTrue(jar.addEssentia(IGNIS, 31, Direction.UP, EssentiaTransferMode.EXECUTE) == 31,
                "could not fill source jar");
        var drops = Block.getDrops(level.getBlockState(source), level, source, entity);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()), "wrong jar loot: " + drops);
        ItemStack drop = drops.get(0);
        level.removeBlock(source, false);
        //? if forge {
        Player player = helper.makeMockSurvivalPlayer();
        //?} else {
        /*Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
        player.setItemInHand(InteractionHand.MAIN_HAND, drop);
        var hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        helper.assertTrue(drop.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "dropped jar could not be placed");
        helper.assertTrue(level.getBlockEntity(target) instanceof EssentiaTransport, "missing placed jar");
        var placed = (EssentiaTransport) level.getBlockEntity(target);
        helper.assertTrue(placed.essentiaAmount(Direction.UP) == 31 && IGNIS.equals(placed.essentiaType(Direction.UP)),
                "breaking and replacing the jar lost its essentia");
        helper.succeed();
    }
}

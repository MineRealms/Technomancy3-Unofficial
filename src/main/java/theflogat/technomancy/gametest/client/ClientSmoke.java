package theflogat.technomancy.gametest.client;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//? if forge {
import net.minecraftforge.event.TickEvent;
//?} else {
/*import net.neoforged.neoforge.client.event.ClientTickEvent;
*///?}
import theflogat.technomancy.Technomancy;
import theflogat.technomancy.client.screen.RitualTomeScreen;

/** Opt-in dev smoke run. This package, like every GameTest, never ships in the release jar. */
@Mod.EventBusSubscriber(modid = Technomancy.MOD_ID, value = Dist.CLIENT)
public final class ClientSmoke {
    private static int ticks;
    private static boolean failed;
    private static boolean captured;
    private static String report = "";
    private static final BlockPos ORIGIN = new BlockPos(0, 100, 0);

    private ClientSmoke() {
    }

    @SubscribeEvent
    //? if forge {
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    //?} else {
    /*public static void tick(ClientTickEvent.Post event) {
    *///?}
        if (!Boolean.getBoolean("technom.clientSmoke")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        try {
            if (ticks == 40) placeGallery(mc);
            if (ticks == 140) inspect(mc);
            if (ticks == 180) {
                mc.setScreen(new RitualTomeScreen());
            }
            if (ticks == 220) {
                Screenshot.grab(mc.gameDirectory, "technom-client-smoke.png", mc.getMainRenderTarget(), message -> {
                    captured = true;
                    Technomancy.LOGGER.info("Client smoke screenshot: {}", message.getString());
                });
            }
            if (ticks >= 250 && captured) {
                Files.writeString(mc.gameDirectory.toPath().resolve("technom-client-smoke.txt"), report);
                Technomancy.LOGGER.info("TECHNOM_CLIENT_SMOKE {}", report);
                mc.stop();
            }
        } catch (Throwable error) {
            if (!failed) {
                failed = true;
                report = "FAIL " + error;
                Technomancy.LOGGER.error("Client smoke failed", error);
                try { Files.writeString(mc.gameDirectory.toPath().resolve("technom-client-smoke.txt"), report); }
                catch (Exception ignored) { }
                mc.stop();
            }
        }
    }

    private static void placeGallery(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            var level = server.overworld();
            for (int x = -3; x <= 18; x++) {
                for (int z = -5; z <= 8; z++) level.setBlockAndUpdate(ORIGIN.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
            }
            int index = 0;
            for (var id : BuiltInRegistries.BLOCK.keySet()) {
                if (!id.getNamespace().equals(Technomancy.MOD_ID)) continue;
                var block = BuiltInRegistries.BLOCK.get(id);
                level.setBlockAndUpdate(ORIGIN.offset((index % 8) * 2, 0, (index / 8) * 2), block.defaultBlockState());
                index++;
            }
            var player = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (player != null) {
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.teleportTo(level, 7.5, 103, -4.5, 0, 25);
            }
        });
    }

    private static void inspect(Minecraft mc) {
        List<String> problems = new ArrayList<>();
        var models = mc.getModelManager();
        var missing = models.getMissingModel();
        int blocks = 0;
        int states = 0;
        int items = 0;
        int custom = 0;
        int renderers = 0;
        for (var id : BuiltInRegistries.BLOCK.keySet()) {
            if (!id.getNamespace().equals(Technomancy.MOD_ID)) continue;
            blocks++;
            var block = BuiltInRegistries.BLOCK.get(id);
            for (var state : block.getStateDefinition().getPossibleStates()) {
                states++;
                var model = models.getBlockModelShaper().getBlockModel(state);
                if (model == missing || model.getParticleIcon().contents().name().getPath().contains("missingno")) {
                    problems.add("missing block model/particle: " + state);
                }
            }
            if (block instanceof EntityBlock entityBlock) {
                var entity = entityBlock.newBlockEntity(ORIGIN, block.defaultBlockState());
                if (entity != null && mc.getBlockEntityRenderDispatcher().getRenderer(entity) != null) renderers++;
            }
        }
        for (var id : BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals(Technomancy.MOD_ID)) continue;
            items++;
            var item = BuiltInRegistries.ITEM.get(id);
            var model = mc.getItemRenderer().getModel(new ItemStack(item), mc.level, mc.player, 0);
            if (model == missing || model.getParticleIcon().contents().name().getPath().contains("missingno")) {
                problems.add("missing item model/particle: " + id);
            }
            if (model.isCustomRenderer()) {
                var renderer = IClientItemExtensions.of(item).getCustomRenderer();
                if (renderer == null || !renderer.getClass().getName().startsWith("theflogat.technomancy.")) {
                    problems.add("missing custom item extension: " + id);
                } else custom++;
            }
        }
        if (blocks < 35 || items < 50 || renderers < 10 || custom < 10) problems.add("incomplete client registrations");
        report = (problems.isEmpty() ? "PASS" : "FAIL") + " blocks=" + blocks + " states=" + states
                + " items=" + items + " customItems=" + custom + " renderers=" + renderers + " issues=" + problems;
        if (!problems.isEmpty()) throw new IllegalStateException(report);
    }
}

package theflogat.technomancy.gametest;

import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectDefinition;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.BuiltinAspects;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import theflogat.technomancy.Technomancy;

/** Smoke checks that the required TC4R public API is linked and populated at runtime. */
@GameTestHolder(Technomancy.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Tc4rApiGameTests {
    private Tc4rApiGameTests() {}

    @GameTest(template = GameTestTemplates.EMPTY_5X5X5, batch = "technom_smoke")
    public static void primalIgnisResolvesThroughAspectApi(GameTestHelper helper) {
        AspectId ignis = AspectId.parse("ignis");
        helper.assertTrue(ignis.value().equals(ResourceLocation.tryBuild("thaumcraft", "ignis")),
                "compact aspect id did not resolve into the thaumcraft namespace: " + ignis);

        AspectDefinition definition = AspectApi.get(ignis);
        helper.assertTrue(definition != null, "AspectApi has no definition for " + ignis);
        helper.assertTrue(definition.primal(), ignis + " is not primal: " + definition.components());
        helper.assertTrue(definition.id().equals(ignis), ignis + " has the wrong id: " + definition.id());

        helper.assertTrue(AspectApi.primals().containsAll(java.util.List.of(BuiltinAspects.AER,
                BuiltinAspects.TERRA, BuiltinAspects.IGNIS, BuiltinAspects.AQUA,
                BuiltinAspects.ORDO, BuiltinAspects.PERDITIO)),
                "the six primal aspects are missing from AspectApi");

        AspectId lux = AspectId.parse("lux");
        helper.assertTrue(AspectApi.combination(BuiltinAspects.AER, ignis).equals(Optional.of(lux)),
                "aer + ignis did not combine into " + lux);

        // Generation 0 is the class-initialisation snapshot; >= 1 proves the server data-pack
        // reload installed the aspect registry that gameplay code will actually see.
        long generation = AspectApi.registry().generation();
        helper.assertTrue(generation >= 1, "aspect registry was never reloaded from data packs");
        helper.succeed();
    }
}
